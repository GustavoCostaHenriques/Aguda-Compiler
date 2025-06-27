#!/bin/bash

# Base directory where tests are stored
TEST_DIR="test/test"
LOG_FILE="logs/Test_Syntax-$(date +'%d%m%Y-%H%M').log"

# Test categories
CATEGORIES=("valid" "invalid-syntax" "invalid-semantic")
# Allow user to specify max number of errors (default = 10)
MAX_ERRORS="${1:-10}"

if ! [[ "$MAX_ERRORS" =~ ^[0-9]+$ ]]; then
    echo "Invalid argument: must be a number."
    exit 1
fi


# Reset log file
echo "Test Report" > "$LOG_FILE"
echo "Generated on $(date)" >> "$LOG_FILE"
echo "===============================" >> "$LOG_FILE"

# Global counters
total=0
passed=0
failed=0

valid_passed=0
valid_failed=0
syntax_passed=0
syntax_failed=0
semantic_passed=0
semantic_failed=0

# Temporary file to store only failed test logs
FAILED_DETAILS=$(mktemp)

echo "Running tests..."

# Loop through each category
for category in "${CATEGORIES[@]}"; do
    echo -e "\n== $category =="

    for test_case_dir in "$TEST_DIR/$category"/*/; do
        [ -d "$test_case_dir" ] || continue

        agu_file=$(find "$test_case_dir" -maxdepth 1 -name "*.agu" | head -n 1)
        if [ -z "$agu_file" ]; then
            echo "No .agu file found in $test_case_dir"
            continue
        fi

        test_name=$(basename "$test_case_dir")
        test_rel_path="$category/$test_name/$(basename "$agu_file")"
        ((total++))

        output=$(java -cp /app/antlr-4.13.2-complete.jar:/app/src:/app/app app.Main "$agu_file" --test-syntax "$MAX_ERRORS")
        result=$?

        # Determine logic depending on category2
        if [ "$category" == "invalid-syntax" ]; then
            if [ $result -ne 0 ]; then
                echo "✅ $test_name"
                ((passed++))
                ((syntax_passed++))
            else
                echo "❌ $test_name"
                ((failed++))
                ((syntax_failed++))

                {
                    echo -e "\n> $test_rel_path"
                    echo "--------------------------------"
                    echo "⚠️  Expected an error but none was thrown."
                    echo
                } >> "$FAILED_DETAILS"
            fi
        else
            if [ $result -eq 0 ]; then
                echo "✅ $test_name"
                ((passed++))
                case "$category" in
                    valid) ((valid_passed++)) ;;
                    invalid-semantic) ((semantic_passed++)) ;;
                esac
            else
                echo "❌ $test_name"
                ((failed++))
                case "$category" in
                    valid) ((valid_failed++)) ;;
                    invalid-semantic) ((semantic_failed++)) ;;
                esac

                {
                    echo -e "\n> $test_rel_path"
                    echo "--------------------------------"
                    echo "$output"
                    echo
                } >> "$FAILED_DETAILS"
            fi
        fi
    done
done

# Write summary
{
    echo ""
    echo "📊 Test Summary:"
    echo "=============================="
    echo "Valid tests ($((valid_passed + valid_failed)))"
    echo "✅ Passed: $valid_passed"
    echo "❌ Failed: $valid_failed"
    echo "=============================="
    echo "Invalid syntax tests ($((syntax_passed + syntax_failed)))"
    echo "✅ Passed: $syntax_passed"
    echo "❌ Failed: $syntax_failed"
    echo "=============================="
    echo "Invalid semantic tests ($((semantic_passed + semantic_failed)))"
    echo "✅ Passed: $semantic_passed"
    echo "❌ Failed: $semantic_failed"
    echo "============================== 📋"
    echo "✅ SUMMARY"
    echo "📊 TOTAL: $total | ✅ PASSED: $passed | ❌ FAILED: $failed"
} >> "$LOG_FILE"

# Append failed test details at the end
if [ $failed -gt 0 ]; then
    echo -e "\n\nTests Failed ❌:" >> "$LOG_FILE"
    cat "$FAILED_DETAILS" >> "$LOG_FILE"
fi

rm "$FAILED_DETAILS"

# Final console summary
echo -e "\n=============================== 📋"
echo "✅ SUMMARY"
echo "📊 TOTAL: $total | ✅ PASSED: $passed | ❌ FAILED: $failed"
echo "📝 Report written to $LOG_FILE"