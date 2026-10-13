#!/bin/bash

# Check test coverage meets minimum threshold (20%)
# Returns 0 if coverage is adequate, 1 if tests fail or coverage is below threshold

set -euo pipefail

COVERAGE_THRESHOLD=20
REPORT_DIR="openmapview/build/reports/coverage/test/debug"
COVERAGE_FILE="$REPORT_DIR/report.xml"

# Remove the previous report so a failed run can never be judged by stale numbers
rm -rf "$REPORT_DIR"

echo "Running unit tests and generating coverage report..."
if ! ./gradlew :openmapview:createDebugUnitTestCoverageReport --quiet; then
    echo "ERROR: Unit tests or coverage report generation failed"
    exit 1
fi

if [ ! -f "$COVERAGE_FILE" ]; then
    echo "ERROR: Coverage report not found at $COVERAGE_FILE"
    exit 1
fi

# Print "covered total percent" for a counter type, e.g. INSTRUCTION, LINE or BRANCH
# Format: <counter type="INSTRUCTION" missed="X" covered="Y"/>
# The report is a single line; the last counter of each type is the report-level total
counter() {
    local line missed covered total
    line=$(grep -o "<counter type=\"$1\"[^>]*>" "$COVERAGE_FILE" | tail -1)
    missed=$(echo "$line" | sed -n 's/.*missed="\([0-9]*\)".*/\1/p')
    covered=$(echo "$line" | sed -n 's/.*covered="\([0-9]*\)".*/\1/p')
    if [ -z "$missed" ] || [ -z "$covered" ]; then
        echo "ERROR: Could not extract $1 coverage from report" >&2
        exit 1
    fi
    total=$((missed + covered))
    if [ $total -eq 0 ]; then
        echo "$covered $total 0"
    else
        echo "$covered $total $((covered * 100 / total))"
    fi
}

# Assign first, so that set -e stops the script when a counter is missing
INSTRUCTION_COUNTER=$(counter INSTRUCTION)
LINE_COUNTER=$(counter LINE)
BRANCH_COUNTER=$(counter BRANCH)
read -r COVERED TOTAL COVERAGE <<< "$INSTRUCTION_COUNTER"
read -r LINES_COVERED LINES_TOTAL LINES_PCT <<< "$LINE_COUNTER"
read -r BRANCHES_COVERED BRANCHES_TOTAL BRANCHES_PCT <<< "$BRANCH_COUNTER"

echo ""
echo "Test Coverage Report:"
echo "  Instructions covered: $COVERED / $TOTAL (${COVERAGE}%)"
echo "  Lines covered:        $LINES_COVERED / $LINES_TOTAL (${LINES_PCT}%)"
echo "  Branches covered:     $BRANCHES_COVERED / $BRANCHES_TOTAL (${BRANCHES_PCT}%)"
echo "  Minimum required:     ${COVERAGE_THRESHOLD}% of instructions"
echo ""

# On GitHub Actions, show the numbers on the run's summary page
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    {
        echo "### Unit test coverage"
        echo ""
        echo "| Counter | Covered | Total | Coverage |"
        echo "| --- | ---: | ---: | ---: |"
        echo "| Instructions | $COVERED | $TOTAL | ${COVERAGE}% |"
        echo "| Lines | $LINES_COVERED | $LINES_TOTAL | ${LINES_PCT}% |"
        echo "| Branches | $BRANCHES_COVERED | $BRANCHES_TOTAL | ${BRANCHES_PCT}% |"
        echo ""
        echo "Minimum required: ${COVERAGE_THRESHOLD}% of instructions. The full HTML report is in the coverage-reports artifact."
    } >> "$GITHUB_STEP_SUMMARY"
fi

if [ "$COVERAGE" -lt "$COVERAGE_THRESHOLD" ]; then
    echo "ERROR: Test coverage ${COVERAGE}% is below minimum threshold of ${COVERAGE_THRESHOLD}%"
    echo ""
    echo "Please add more tests to increase coverage."
    echo "Coverage report: $REPORT_DIR/index.html"
    echo ""
    exit 1
fi

echo "Test coverage check passed!"
exit 0
