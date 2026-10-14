#!/bin/bash

# Publish a new OpenMapView release to Maven Central
# Usage: ./scripts/publish-release.sh [vX.Y.Z]
#
# Bumps the dependency version shown in README.md and docs/REPLACING_GOOGLE_MAPS.md,
# commits it as "Release vX.Y.Z", creates an SSH-signed tag and atomically pushes
# main and the tag. The tag push triggers .github/workflows/release.yml.
#
# Without an argument, releases the version already shown in the docs if it is not
# tagged yet, otherwise the next patch version after the latest tag.

set -euo pipefail

SCRIPT_DIRECTORY="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
REPOSITORY_DIRECTORY="$(cd -- "$SCRIPT_DIRECTORY/.." && pwd -P)"
readonly SCRIPT_DIRECTORY REPOSITORY_DIRECTORY
cd "$REPOSITORY_DIRECTORY"

readonly VERSION_FILES=(README.md docs/REPLACING_GOOGLE_MAPS.md)

fail() {
    printf 'Error: %s\n' "$*" >&2
    exit 1
}

if (($# > 1)); then
    fail "Usage: ${0##*/} [vX.Y.Z]"
fi
if (($# == 1)) && [[ ! "$1" =~ ^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
    fail 'Release tag must look like v1.2.3, without leading zeros.'
fi

command -v python3 >/dev/null || fail 'python3 is required.'
[[ "$(git rev-parse --show-toplevel)" == "$REPOSITORY_DIRECTORY" ]] || fail 'Script must belong to the repository root.'
[[ "$(git symbolic-ref --quiet --short HEAD)" == main ]] || fail 'Check out main before releasing.'
[[ -z "$(git status --porcelain)" ]] || fail 'Commit or stash changes before releasing.'

git fetch --tags origin refs/heads/main:refs/remotes/origin/main
git merge-base --is-ancestor refs/remotes/origin/main HEAD || fail 'main is behind or diverged from origin/main. Reconcile it first.'

# Prints "<version> <docs-need-update>", e.g. "0.13.5 true"
version_info="$(python3 - "${1:-}" "${VERSION_FILES[@]}" <<'PY'
import pathlib
import re
import subprocess
import sys

DEPENDENCY = re.compile(r"de\.afarber:openmapview(?:-compose)?:([0-9]+\.[0-9]+\.[0-9]+)")
TAG = re.compile(r"v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)")

def parse(version):
    return tuple(int(part) for part in version.split("."))

requested = sys.argv[1]
doc_versions = set()
for name in sys.argv[2:]:
    found = DEPENDENCY.findall(pathlib.Path(name).read_text())
    if not found:
        sys.exit(f"No de.afarber:openmapview:X.Y.Z dependency found in {name}.")
    doc_versions.update(found)
if len(doc_versions) != 1:
    sys.exit(f"Docs disagree on the version: {', '.join(sorted(doc_versions))}. Align them first.")
doc_version = parse(doc_versions.pop())

tags = subprocess.check_output(["git", "tag", "--list", "v*"], text=True).splitlines()
tagged = [tuple(int(part) for part in match.groups()) for match in map(TAG.fullmatch, tags) if match]
highest = max(tagged, default=(0, 0, 0))

if requested:
    version = parse(requested[1:])
elif doc_version > highest:
    version = doc_version
else:
    version = (highest[0], highest[1], highest[2] + 1)

if version <= highest:
    sys.exit(f"Release version must exceed the latest tag v{'.'.join(map(str, highest))}.")
print(".".join(map(str, version)), "true" if version != doc_version else "false")
PY
)"
read -r version docs_need_update <<<"$version_info"
readonly version docs_need_update
readonly release_tag="v$version"

# Leave local changes intact if signing, tagging, or the atomic push fails.
# This allows retrying the same release without consuming another version.
release_stage='version update'
recover() {
    local exit_code=$?
    printf '\nRelease %s failed during %s; local changes have been preserved.\n' "$release_tag" "$release_stage" >&2
    case "$release_stage" in
        commit)
            printf 'Fix SSH signing, then commit %s with git commit -S -m "Release %s".\n' "${VERSION_FILES[*]}" "$release_tag" >&2
            printf 'Then create an SSH-signed tag with git tag -s %s -m "Release %s" and push main and the tag.\n' "$release_tag" "$release_tag" >&2
            ;;
        tag)
            printf 'Check the local tag state and SSH signing, then retry: git tag -s %s -m "Release %s"\n' "$release_tag" "$release_tag" >&2
            ;;
        push)
            printf 'After resolving the remote error, retry: git push --atomic origin HEAD:refs/heads/main refs/tags/%s\n' "$release_tag" >&2
            ;;
    esac
    exit "$exit_code"
}
trap recover ERR

printf 'Preparing release %s...\n' "$release_tag"

if [[ "$docs_need_update" == true ]]; then
    python3 - "$version" "${VERSION_FILES[@]}" <<'PY'
import pathlib
import re
import sys

for name in sys.argv[2:]:
    path = pathlib.Path(name)
    text, count = re.subn(r"(de\.afarber:openmapview(?:-compose)?:)[0-9]+\.[0-9]+\.[0-9]+", r"\g<1>" + sys.argv[1], path.read_text())
    if count == 0:
        sys.exit(f"No dependency version found in {name}; no changes written.")
    path.write_text(text)
PY
    release_stage=commit
    git add -- "${VERSION_FILES[@]}"
    git -c gpg.format=ssh commit -S -m "Release $release_tag" -- "${VERSION_FILES[@]}"
else
    printf 'Docs already show %s; tagging the current commit.\n' "$version"
fi

release_stage=tag
git -c gpg.format=ssh tag -s "$release_tag" -m "Release $release_tag"
# Some Git versions return success and create an unsigned tag when SSH signing fails.
# This name was absent before this release; discard only that incomplete new tag.
if ! git cat-file tag "$release_tag" | grep -q -- '^-----BEGIN SSH SIGNATURE-----$'; then
    git tag -d "$release_tag"
    printf 'Error: release tag has no SSH signature; it will not be pushed.\n' >&2
    false # Invoke the normal tag-stage recovery handler.
fi

release_stage=push
git push --atomic origin HEAD:refs/heads/main "refs/tags/$release_tag"
trap - ERR

printf '\nPushed %s. GitHub Actions will publish it to Maven Central.\n' "$release_tag"
printf 'Builds: https://github.com/afarber/OpenMapView/actions\n'
printf 'Artifact: https://central.sonatype.com/artifact/de.afarber/openmapview/%s\n' "$version"
