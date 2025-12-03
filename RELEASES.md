# Creating Releases

This document explains how to create public releases for the Office Suite Android app.

## Automatic Releases (Recommended)

The repository is configured to automatically create GitHub Releases when you push a version tag.

### Steps to Create a Release

1. **Update version in `app/build.gradle`**
   ```gradle
   versionCode 2
   versionName "1.1.0"
   ```

2. **Commit the version change**
   ```bash
   git add app/build.gradle
   git commit -m "Bump version to 1.1.0"
   ```

3. **Create and push a version tag**
   ```bash
   git tag v1.1.0
   git push origin v1.1.0
   ```

4. **The release workflow will automatically:**
   - Build the signed release APK
   - Create a GitHub Release with the tag
   - Upload the APK to the release
   - Make it publicly available for download

### Version Tag Format

Version tags must follow the format: `v*.*.*` (e.g., `v1.0.0`, `v1.2.3`, `v2.0.0-beta`)

## Manual Release Workflow

Alternatively, you can use the manual "Update All Devices" workflow:

1. Go to **Actions** → **Update All Devices**
2. Click **Run workflow**
3. Enter the version name (e.g., `1.1.0`)
4. Enter release notes
5. Click **Run workflow**

This will create a release with custom release notes.

## Viewing Releases

All releases are publicly available at:
https://github.com/Godzilla675/Android-office-alternitave/releases

Users can download the latest APK from:
https://github.com/Godzilla675/Android-office-alternitave/releases/latest

## Release APK Details

- **Signed**: Yes, using debug keystore (suitable for open-source distribution)
- **Min SDK**: Android 8.0 (API 26)
- **Package**: com.officesuite.app
- **File naming**: `officesuite-v{VERSION}.apk`

## Notes

- The APK is signed with a debug keystore to allow easy distribution without requiring production signing keys
- Users can install the APK directly on their devices (may need to enable "Install from unknown sources")
- Previous installations will be updated automatically when installing a newer version
