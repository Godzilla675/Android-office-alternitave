# Test Report

## Summary
- Command: `./gradlew test`
- Result: ✅ All unit tests passed

## Details
- `:app:testReleaseUnitTest`: 560 tests passed
- `:app:testDebugUnitTest`: 548 tests passed

## Notes
- Tests executed in headless CI environment; no manual device/emulator verification performed.
- Instrumentation/UI flows were not exercised. Build with Android Studio on a device or emulator for end-to-end validation.
- To run instrumentation/UI tests locally, attach a device or emulator and execute `./gradlew connectedAndroidTest`.
