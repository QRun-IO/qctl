#!/bin/bash

# Simple GraalVM Environment Check Script
# Quick validation without building

echo "🔍 Quick GraalVM Environment Check"
echo "================================="

# Check if running the validation script directly
if [ -f "scripts/validate-graalvm.sh" ]; then
    ./scripts/validate-graalvm.sh
elif [ -f "../scripts/validate-graalvm.sh" ]; then
    ../scripts/validate-graalvm.sh
else
    echo "❌ Cannot find validation script. Run from project root directory."
    exit 1
fi
