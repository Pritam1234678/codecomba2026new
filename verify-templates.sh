#!/bin/bash

echo "=== Verifying Email Templates ==="
echo ""

TEMPLATE_DIR="src/main/resources/email-templates"
TEMPLATES=(
  "hosting-request-submitted"
  "hosting-approved"
  "hosting-rejected"
  "contest-created"
  "first-participant-joined"
  "contest-started"
  "contest-ended"
  "contest-cancelled"
)

SUCCESS_COUNT=0
FAIL_COUNT=0

for template in "${TEMPLATES[@]}"; do
  FILE="$TEMPLATE_DIR/$template.html"
  
  if [ ! -f "$FILE" ]; then
    echo "✗ FAIL: $template.html not found"
    ((FAIL_COUNT++))
    continue
  fi
  
  # Check for required elements
  CHECKS=(
    "CodeCoder"
    "https://codecoder.in/logo.png"
    "unsubscribe"
    "© 2026 CodeCoder"
  )
  
  ALL_PASS=true
  for check in "${CHECKS[@]}"; do
    if ! grep -q "$check" "$FILE"; then
      echo "✗ FAIL: $template.html missing '$check'"
      ALL_PASS=false
    fi
  done
  
  if $ALL_PASS; then
    echo "✓ PASS: $template.html"
    ((SUCCESS_COUNT++))
  else
    ((FAIL_COUNT++))
  fi
done

echo ""
echo "=== Summary ==="
echo "Passed: $SUCCESS_COUNT"
echo "Failed: $FAIL_COUNT"

if [ $FAIL_COUNT -eq 0 ]; then
  echo ""
  echo "All email templates are valid! ✓"
  exit 0
else
  echo ""
  echo "Some templates failed validation! ✗"
  exit 1
fi
