#!/bin/bash

# Setup Git hooks for OpenMapView
# This script installs a pre-commit hook that checks code formatting and copyright headers

HOOKS_DIR=".git/hooks"
HOOK_FILE="$HOOKS_DIR/pre-commit"

# Check if .git directory exists
if [ ! -d ".git" ]; then
    echo "Error: .git directory not found. Run this script from the repository root."
    exit 1
fi

# Create hooks directory if it doesn't exist
mkdir -p "$HOOKS_DIR"

# Create pre-commit hook
cat > "$HOOK_FILE" << 'EOF'
#!/bin/bash

echo "Running pre-commit checks..."
echo ""

# Check code formatting
echo "1. Checking code formatting..."
./scripts/check-format.sh > /dev/null 2>&1

if [ $? -ne 0 ]; then
    echo ""
    echo "Code formatting check failed!"
    echo "Run './gradlew spotlessApply' to fix formatting issues."
    echo ""
    exit 1
fi

echo "   Code formatting: OK"

# Check copyright headers
echo "2. Checking copyright headers..."
./scripts/check-copyright.sh > /dev/null 2>&1

if [ $? -ne 0 ]; then
    echo ""
    echo "Copyright header check failed!"
    echo "Run './gradlew spotlessApply' to fix issues."
    echo ""
    exit 1
fi

echo "   Copyright headers: OK"
echo ""
echo "All pre-commit checks passed!"
EOF

# Make hook executable
chmod +x "$HOOK_FILE"

echo "Git hooks installed successfully!"
echo ""
echo "Pre-commit hook will now:"
echo "  - Check code formatting before each commit"
echo "  - Check copyright headers on all Kotlin files"
echo "  - Block commits if issues are found"
echo "  - Prompt to run './gradlew spotlessApply' to fix issues"
echo ""
echo "To bypass the hook (not recommended), use: git commit --no-verify"
