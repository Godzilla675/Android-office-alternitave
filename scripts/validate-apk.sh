#!/bin/bash
# Script to validate APK structure and installability
# This script can be run locally or in CI to verify the APK is valid

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================"
echo "APK Validation Script"
echo "========================================"
echo ""

# Check if APK exists
APK_DEBUG="$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
APK_RELEASE="$PROJECT_ROOT/app/build/outputs/apk/release/app-release-unsigned.apk"

if [ -f "$APK_DEBUG" ]; then
    APK_PATH="$APK_DEBUG"
    echo -e "${GREEN}Found debug APK${NC}"
elif [ -f "$APK_RELEASE" ]; then
    APK_PATH="$APK_RELEASE"
    echo -e "${GREEN}Found release APK${NC}"
else
    echo -e "${YELLOW}No APK found. Building debug APK...${NC}"
    cd "$PROJECT_ROOT"
    ./gradlew assembleDebug
    APK_PATH="$APK_DEBUG"
fi

echo ""
echo "APK Path: $APK_PATH"
echo ""

# Check APK file type
echo "=== File Type Check ==="
FILE_TYPE=$(file "$APK_PATH")
echo "$FILE_TYPE"
if [[ "$FILE_TYPE" == *"Android"* ]] || [[ "$FILE_TYPE" == *"Zip"* ]]; then
    echo -e "${GREEN}✓ Valid APK file format${NC}"
else
    echo -e "${RED}✗ Invalid APK file format${NC}"
    exit 1
fi
echo ""

# Check APK size
echo "=== APK Size ==="
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo "Size: $APK_SIZE"
echo ""

# Verify ZIP structure
echo "=== ZIP Structure Verification ==="
if unzip -t "$APK_PATH" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ APK ZIP structure is valid${NC}"
else
    echo -e "${RED}✗ APK ZIP structure is corrupted${NC}"
    exit 1
fi
echo ""

# Check for required APK components
echo "=== Required Components Check ==="
REQUIRED_FILES=("AndroidManifest.xml" "classes.dex" "resources.arsc")
MISSING_FILES=()

for file in "${REQUIRED_FILES[@]}"; do
    if unzip -l "$APK_PATH" | grep -q "$file"; then
        echo -e "${GREEN}✓ Found: $file${NC}"
    else
        echo -e "${RED}✗ Missing: $file${NC}"
        MISSING_FILES+=("$file")
    fi
done

if [ ${#MISSING_FILES[@]} -gt 0 ]; then
    echo -e "${RED}APK is missing required files${NC}"
    exit 1
fi
echo ""

# Use aapt to analyze APK (if available)
echo "=== APK Badge Information ==="
if command -v aapt &> /dev/null; then
    AAPT_CMD="aapt"
elif [ -f "$ANDROID_HOME/build-tools/34.0.0/aapt" ]; then
    AAPT_CMD="$ANDROID_HOME/build-tools/34.0.0/aapt"
elif [ -f "/usr/local/lib/android/sdk/build-tools/34.0.0/aapt" ]; then
    AAPT_CMD="/usr/local/lib/android/sdk/build-tools/34.0.0/aapt"
else
    # Find any aapt in build-tools
    AAPT_CMD=$(find "${ANDROID_HOME:-/usr/local/lib/android/sdk}/build-tools" -name "aapt" 2>/dev/null | head -1)
fi

if [ -n "$AAPT_CMD" ] && [ -f "$AAPT_CMD" ]; then
    echo "Using aapt: $AAPT_CMD"
    echo ""
    
    # Extract package info
    PACKAGE_INFO=$("$AAPT_CMD" dump badging "$APK_PATH" 2>&1)
    
    PACKAGE_NAME=$(echo "$PACKAGE_INFO" | grep "^package:" | sed "s/package: name='\([^']*\)'.*/\1/")
    VERSION_NAME=$(echo "$PACKAGE_INFO" | grep "^package:" | sed "s/.*versionName='\([^']*\)'.*/\1/")
    VERSION_CODE=$(echo "$PACKAGE_INFO" | grep "^package:" | sed "s/.*versionCode='\([^']*\)'.*/\1/")
    MIN_SDK=$(echo "$PACKAGE_INFO" | grep "sdkVersion:" | sed "s/.*sdkVersion:'\([^']*\)'.*/\1/")
    TARGET_SDK=$(echo "$PACKAGE_INFO" | grep "targetSdkVersion:" | sed "s/.*targetSdkVersion:'\([^']*\)'.*/\1/")
    LAUNCHABLE_ACTIVITY=$(echo "$PACKAGE_INFO" | grep "launchable-activity" | sed "s/.*name='\([^']*\)'.*/\1/")
    
    echo "Package Name: $PACKAGE_NAME"
    echo "Version Name: $VERSION_NAME"
    echo "Version Code: $VERSION_CODE"
    echo "Min SDK: $MIN_SDK"
    echo "Target SDK: $TARGET_SDK"
    echo "Launchable Activity: $LAUNCHABLE_ACTIVITY"
    echo ""
    
    # Validate critical fields
    if [ -z "$PACKAGE_NAME" ]; then
        echo -e "${RED}✗ Package name not found - APK may be invalid${NC}"
        exit 1
    else
        echo -e "${GREEN}✓ Package name is valid${NC}"
    fi
    
    if [ -z "$LAUNCHABLE_ACTIVITY" ]; then
        echo -e "${RED}✗ No launchable activity found - app cannot be launched${NC}"
        exit 1
    else
        echo -e "${GREEN}✓ Launchable activity found${NC}"
    fi
    
    if [ -n "$MIN_SDK" ] && [ "$MIN_SDK" -lt 21 ]; then
        echo -e "${YELLOW}⚠ Min SDK ($MIN_SDK) is very low, may have compatibility issues${NC}"
    else
        echo -e "${GREEN}✓ Min SDK version is reasonable ($MIN_SDK)${NC}"
    fi
else
    echo -e "${YELLOW}aapt not found, skipping detailed APK analysis${NC}"
fi
echo ""

# Check signatures (debug vs release)
echo "=== Signature Check ==="
# Try using apksigner if available (more accurate for v2/v3/v4 signatures)
if [ -f "$ANDROID_HOME/build-tools/34.0.0/apksigner" ]; then
    APKSIGNER_CMD="$ANDROID_HOME/build-tools/34.0.0/apksigner"
elif [ -f "/usr/local/lib/android/sdk/build-tools/34.0.0/apksigner" ]; then
    APKSIGNER_CMD="/usr/local/lib/android/sdk/build-tools/34.0.0/apksigner"
else
    APKSIGNER_CMD=$(find "${ANDROID_HOME:-/usr/local/lib/android/sdk}/build-tools" -name "apksigner" 2>/dev/null | head -1)
fi

if [ -n "$APKSIGNER_CMD" ] && [ -f "$APKSIGNER_CMD" ]; then
    echo "Verifying with apksigner..."
    if "$APKSIGNER_CMD" verify --print-certs "$APK_PATH" 2>&1 | grep -q "Verified"; then
        echo -e "${GREEN}✓ APK signature is valid${NC}"
        "$APKSIGNER_CMD" verify --print-certs "$APK_PATH" 2>&1 | head -5
    else
        # For debug builds, signature verification may fail differently
        SIGN_RESULT=$("$APKSIGNER_CMD" verify "$APK_PATH" 2>&1)
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ APK is signed (debug signature)${NC}"
        else
            echo -e "${YELLOW}⚠ Signature verification result:${NC}"
            echo "$SIGN_RESULT"
        fi
    fi
elif unzip -l "$APK_PATH" | grep -q "META-INF/.*\.\(RSA\|DSA\|EC\)"; then
    echo -e "${GREEN}✓ APK is signed (v1 signature found)${NC}"
else
    echo -e "${YELLOW}⚠ Could not verify signature (apksigner not found)${NC}"
fi
echo ""

# Native libraries check
echo "=== Native Libraries Check ==="
NATIVE_LIBS=$(unzip -l "$APK_PATH" | grep "lib/" | grep "\.so" || true)
if [ -n "$NATIVE_LIBS" ]; then
    echo "Found native libraries:"
    echo "$NATIVE_LIBS" | head -10
    
    # Check for common ABIs
    for ABI in "arm64-v8a" "armeabi-v7a" "x86_64" "x86"; do
        if echo "$NATIVE_LIBS" | grep -q "$ABI"; then
            echo -e "${GREEN}✓ Supports: $ABI${NC}"
        fi
    done
else
    echo "No native libraries found (app is pure Java/Kotlin)"
fi
echo ""

# Check for common issues that cause "Package appears to be invalid"
echo "=== Common Issues Check ==="

# Check if manifest is parseable
echo "Checking AndroidManifest.xml..."
if unzip -p "$APK_PATH" "AndroidManifest.xml" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ AndroidManifest.xml is readable${NC}"
else
    echo -e "${RED}✗ Cannot read AndroidManifest.xml${NC}"
    exit 1
fi

# Check for split APKs indication
if unzip -l "$APK_PATH" | grep -q "split_config"; then
    echo -e "${YELLOW}⚠ This may be a split APK - requires all splits for installation${NC}"
else
    echo -e "${GREEN}✓ Not a split APK${NC}"
fi
echo ""

echo "========================================"
echo -e "${GREEN}APK Validation Complete - All checks passed!${NC}"
echo "========================================"
echo ""
echo "The APK appears to be valid and should install correctly."
echo "If you're still having issues on a specific device, try:"
echo "  1. Enable 'Install from unknown sources' in settings"
echo "  2. Clear any previous installations of the app"
echo "  3. Check device storage space"
echo "  4. Try 'adb install -r $APK_PATH' via command line for more details"
echo ""
