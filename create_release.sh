#!/bin/bash

# Eiskalt Android App - Test Release Creation Script
# This script demonstrates the steps to create a test release
# Note: Requires Java JDK 17 and Android build tools

heading() {
    local char="$1"
    local text="$2"
    local len=${#text}
    local sep=$(printf '%*s' "$len" '' | tr ' ' "$char")
    echo ""
    echo "$sep"
    echo "$text"
    echo "$sep"
}

heading "*" "*** Eiskalt Android App - Test Release Creation ***"

# Step 1: Check prerequisites
heading "=" "Step 1/7: Checking prerequisites..."
if command -v java &> /dev/null; then
    echo "✓ Java is installed: $(java -version 2>&1 | head -n 1)"
else
    echo "✗ Java is not installed. Please install Java JDK 17 first."
    echo "   On macOS: brew install --cask temurin17"
    exit 1
fi

if [ ! -f "gradlew" ]; then
    echo "✗ Please run this script from the project root directory"
    exit 1
fi

# Step 2: Clean the project
heading "=" "Step 2/7: Cleaning the project..."
./gradlew clean

# Step 3: Build debug version first (for testing)
heading "=" "Step 3/7: Building debug APK..."
./gradlew assembleDebug

# Step 4: Build release APK
heading "=" "Step 4/7: Building release APK..."
./gradlew assembleRelease

# Step 5: Build app bundle (recommended for Play Store)
heading "=" "Step 5/7: Building app bundle..."
./gradlew bundleRelease

# Step 6: Show output locations
heading "=" "Step 6/7: Release artifacts created:"
echo "  - Debug APK: app/build/outputs/apk/debug/app-debug.apk"
echo "  - Release APK: app/build/outputs/apk/release/app-release.apk"
echo "  - App Bundle: app/build/outputs/bundle/release/app-release.aab"

# Step 7: Verify build
heading "=" "Step 7/7: Verifying build..."
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo "✓ Release APK created successfully"
    ls -lh app/build/outputs/apk/release/app-release.apk
else
    echo "✗ Release APK creation failed"
fi

if [ -f "app/build/outputs/bundle/release/app-release.aab" ]; then
    echo "✓ App Bundle created successfully"
    ls -lh app/build/outputs/bundle/release/app-release.aab
else
    echo "✗ App Bundle creation failed"
fi

echo ""
echo "=== Test Release Creation Complete ==="
echo ""
echo "Next steps:"
echo "1. Test the release APK on your devices"
echo "2. Upload the app bundle to Google Play Console for testing"
echo "3. Share the APK with your testers"

