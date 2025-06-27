#!/bin/bash

# Base directory where tests are stored
TEST_DIR="test/test"
LOG_FILE="logs/Test_Execution-$(date +'%d%m%Y-%H%M').log"

# Test categories
CATEGORIES=("valid")
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
        rel_path="${agu_file#*valid/}"          
        dir_part=$(dirname "$rel_path")         
        base_name=$(basename "$rel_path" .agu)  




        test_name=$(basename "$test_case_dir")
        test_rel_path="$category/$test_name/$(basename "$agu_file")"
        ((total++))

        output=$(java -cp /app/antlr-4.13.2-complete.jar:/app/src:/app/app app.Main "$agu_file" --test-execution "$MAX_ERRORS" 2>&1)
        result=$?

        if [ $result -eq 0 ]; then
            echo "✅ $test_name"
            ((passed++))
        else
            echo "❌ $test_name"
            ((failed++))

            {
                echo -e "\n> $test_rel_path"
                echo "--------------------------------"
                echo "$output"
                echo
            } >> "$FAILED_DETAILS"
        fi
    done
done

# Write summary
{
    echo ""
    echo "📊 Test Summary:"
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