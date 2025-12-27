#!/bin/bash

# Check code quality with Detekt static analysis
# Returns 0 if no issues found, 1 if issues are found

echo "Running Detekt static analysis..."

./gradlew detekt

if [ $? -ne 0 ]; then
    echo ""
    echo "Detekt static analysis failed!"
    echo "Run './gradlew detekt' to see details."
    echo ""
    exit 1
fi

echo "Static analysis passed."
exit 0
