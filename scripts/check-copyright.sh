#!/bin/bash

# Check for missing MIT copyright headers in Kotlin files
# Returns 0 if all files have headers, 1 if any are missing

MISSING_FILES=()

while IFS= read -r file; do
  if ! grep -q "SPDX-License-Identifier: MIT" "$file"; then
    MISSING_FILES+=("$file")
  fi
done < <(find . -type f \( -name "*.kt" -o -name "*.kts" \) \
         -not -path "*/build/*" \
         -not -path "*/.gradle/*")

if [ ${#MISSING_FILES[@]} -gt 0 ]; then
  echo ""
  echo "ERROR: The following files are missing the MIT copyright header:"
  echo ""
  for file in "${MISSING_FILES[@]}"; do
    echo "  - $file"
  done
  echo ""
  echo "Required header format:"
  echo "/*"
  echo " * Copyright (c) 2025 Alexander Farber"
  echo " * SPDX-License-Identifier: MIT"
  echo " *"
  echo " * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)"
  echo " */"
  echo ""
  echo "Run './gradlew spotlessApply' to automatically add headers."
  echo ""
  exit 1
fi

echo "All Kotlin files have proper copyright headers."
exit 0
