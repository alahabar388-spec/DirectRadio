# DirectRadio

Android offline communication prototype and companion messenger for AlpineQuest Pro.

## Current milestone: 0.3.0-rc1 (feature/alpinequest-messenger)

- Jetpack Compose interface
- Wi-Fi Direct peer discovery and connection
- TCP text-message transport over an established Wi-Fi Direct group
- Incoming and outgoing chat bubbles
- Nearby participant list and connection status
- AlpineQuest Pro companion integration plan documented

## Important limitations

- The current text transport supports one peer per Wi-Fi Direct group owner; mesh forwarding and multi-peer fan-out are not implemented.
- PTT remains a UI placeholder; voice is not transmitted.
- Application-layer end-to-end encryption, identity verification, delivery receipts, and durable chat history are not implemented.
- GPS location sharing and the Show in AlpineQuest hand-off are planned but not implemented in this milestone.
- Wi-Fi Direct is a local short-range transport and cannot guarantee a 20 km connection.
- This code change has not yet been built or tested on physical devices. Do not use this prototype for emergency, safety-critical, or sensitive communications.

## AlpineQuest integration

AlpineQuest is a third-party closed-source app, so DirectRadio is a companion app rather than a modified AlpineQuest APK. See documentation/ALPINEQUEST_MESSENGER.md for the documented Android intent hand-off and implementation status.

## Build

Open this project in Android Studio with JDK 17 and Android SDK 35. Allow Gradle to sync, then build the debug APK.

See documentation/P2P_TEST_GUIDE.md for two-phone testing.
