#!/bin/bash

# Check code formatting with Spotless
# Returns 0 if formatting is correct, 1 if issues are found

echo "Checking code formatting with Spotless..."

./gradlew spotlessCheck

if [ $? -ne 0 ]; then
    echo ""
    echo "Code formatting check failed!"
    echo "Run './gradlew spotlessApply' to fix formatting issues."
    echo ""
    exit 1
fi

echo "Code formatting is correct."
exit 0
