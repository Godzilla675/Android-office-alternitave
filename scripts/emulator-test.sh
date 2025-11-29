#!/bin/bash
# Script to test APK installation and launch on Android emulator
# This script is called by the android-emulator-test.yml workflow

set -e

PACKAGE_NAME="${PACKAGE_NAME:-}"
LAUNCHABLE_ACTIVITY="${LAUNCHABLE_ACTIVITY:-}"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "=== Emulator Ready ===" 
echo ""
echo "=== APK Package Info ==="
echo "Package: $PACKAGE_NAME"
echo "Activity: $LAUNCHABLE_ACTIVITY"
echo ""

echo "=== Installing APK ==="
if adb install -r "$APK_PATH"; then
    echo "=== APK Installation Successful ===" 
    
    echo ""
    echo "=== Launching App ==="
    if adb shell am start -n "$PACKAGE_NAME/$LAUNCHABLE_ACTIVITY"; then
        echo "App launch command succeeded"
    else
        echo "App launch command failed"
    fi
    
    sleep 5
    
    echo ""
    echo "=== Checking if app is running ==="
    if adb shell pidof "$PACKAGE_NAME" > /dev/null 2>&1; then
        echo "App is running successfully!"
        echo "RESULT=success" >> "$GITHUB_ENV"
        echo "TEST_MESSAGE=✅ APK installation and launch successful on Android emulator (API 30, x86_64)" >> "$GITHUB_ENV"
    else
        echo "App launched but may have crashed or is not running"
        echo "RESULT=partial" >> "$GITHUB_ENV"
        echo "TEST_MESSAGE=⚠️ APK installed but app may have crashed after launch" >> "$GITHUB_ENV"
    fi
else
    echo "=== APK Installation Failed ==="
    echo "RESULT=failed" >> "$GITHUB_ENV"
    echo "TEST_MESSAGE=❌ APK installation failed on Android emulator" >> "$GITHUB_ENV"
fi

echo ""
echo "=== Package Info ==="
adb shell pm list packages | grep "package:$PACKAGE_NAME" || echo "Package not found in installed packages"

echo ""
echo "=== APK Analysis ==="
# Find aapt in Android SDK
AAPT_CMD=$(find "$ANDROID_SDK_ROOT/build-tools" -name "aapt" 2>/dev/null | head -1)
if [ -n "$AAPT_CMD" ]; then
    "$AAPT_CMD" dump badging "$APK_PATH" | head -20
else
    echo "Could not analyze APK - aapt not found"
fi

echo ""
echo "=== Test Complete ==="
