#!/bin/bash

# Check test coverage meets minimum threshold (50%)
# Returns 0 if coverage is adequate, 1 if below threshold

COVERAGE_THRESHOLD=50

echo "Generating test coverage report..."
./gradlew :openmapview:jacocoTestReport --quiet

# Check if coverage report was generated
COVERAGE_FILE="openmapview/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"

if [ ! -f "$COVERAGE_FILE" ]; then
    echo "ERROR: Coverage report not found at $COVERAGE_FILE"
    echo "Run './gradlew :openmapview:test jacocoTestReport' to generate coverage."
    exit 1
fi

# Extract coverage percentage from XML report
# Format: <counter type="INSTRUCTION" missed="X" covered="Y"/>
INSTRUCTIONS=$(grep -A 1 '<counter type="INSTRUCTION"' "$COVERAGE_FILE" | tail -1)
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
    echo "Coverage report: openmapview/build/reports/jacoco/jacocoTestReport/html/index.html"
    echo ""
    exit 1
fi

echo "Test coverage check passed!"
exit 0
