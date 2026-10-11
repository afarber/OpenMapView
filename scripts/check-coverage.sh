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

# Extract coverage percentage from XML report
# Format: <counter type="INSTRUCTION" missed="X" covered="Y"/>
# The report is a single line; the last INSTRUCTION counter is the report-level total
INSTRUCTIONS=$(grep -o '<counter type="INSTRUCTION"[^>]*>' "$COVERAGE_FILE" | tail -1)
MISSED=$(echo "$INSTRUCTIONS" | sed -n 's/.*missed="\([0-9]*\)".*/\1/p')
COVERED=$(echo "$INSTRUCTIONS" | sed -n 's/.*covered="\([0-9]*\)".*/\1/p')

if [ -z "$MISSED" ] || [ -z "$COVERED" ]; then
    echo "ERROR: Could not extract coverage data from report"
    exit 1
fi

TOTAL=$((MISSED + COVERED))
if [ $TOTAL -eq 0 ]; then
    COVERAGE=0
else
    COVERAGE=$((COVERED * 100 / TOTAL))
fi

echo ""
echo "Test Coverage Report:"
echo "  Instructions covered: $COVERED / $TOTAL"
echo "  Coverage: ${COVERAGE}%"
echo "  Minimum required: ${COVERAGE_THRESHOLD}%"
echo ""

if [ $COVERAGE -lt $COVERAGE_THRESHOLD ]; then
    echo "ERROR: Test coverage ${COVERAGE}% is below minimum threshold of ${COVERAGE_THRESHOLD}%"
    echo ""
    echo "Please add more tests to increase coverage."
    echo "Coverage report: $REPORT_DIR/index.html"
    echo ""
    exit 1
fi

echo "Test coverage check passed!"
exit 0
