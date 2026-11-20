# DirectRadio

Android offline push-to-talk prototype.

## Current milestone: 0.2.1
- Jetpack Compose interface
- Wi-Fi Direct peer discovery
- Connection request and group-state reporting
- Runtime nearby Wi-Fi permission handling
- Project and two-phone test documentation
- Wi-Fi Direct socket permission declared for the next transport milestone

## Important
This build does not transmit voice yet. PTT is a UI placeholder. Mesh forwarding and application-layer end-to-end encryption are not implemented. Do not use this prototype for critical or sensitive communications.

## Build
Open this project in Android Studio with JDK 17 and Android SDK 35. Allow Gradle to sync, then build the debug APK.

See `documentation/P2P_TEST_GUIDE.md` for two-phone testing.
