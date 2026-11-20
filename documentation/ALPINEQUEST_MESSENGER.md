# AlpineQuest Pro companion messenger integration

## Scope

AlpineQuest Off-Road Explorer is a third-party closed-source application. This repository does not modify or embed code into its APK. DirectRadio is implemented as a companion Android app with its own messenger screen.

## Supported hand-off

AlpineQuest documents the global Android intent action
`net.psyberia.intent.action.DISPLAY_LOCATION`. A companion app can use this action to display a WGS84 point in AlpineQuest Pro. The documented package is `psyberia.alpinequest.full`; the documented activity is `psyberia.alpinequest.full.AlpineQuestActivity`.

Required extras:
- `net.psyberia.intent.extra.LATITUDE` — Double
- `net.psyberia.intent.extra.LONGITUDE` — Double

Optional extras include `NAME`, `COMMENT`, `DESCRIPTION`, `ICON` and `MOVE_TO`.

## User experience planned

1. User opens DirectRadio Messenger from the Android launcher or a home-screen shortcut.
2. Users discover and connect to one another over Wi-Fi Direct.
3. Connected users exchange text messages without mobile data or an internet server.
4. A future location-sharing feature will add a "Show in AlpineQuest" action to a received location message.
5. DirectRadio will launch the documented AlpineQuest location intent with the received coordinates.

## Current implementation status

- Implemented in this branch: Compose chat screen, outgoing and incoming text message UI, newline-delimited TCP message transport over an established Wi-Fi Direct group.
- Current transport limitation: one connected peer per group owner; no mesh forwarding, multi-peer fan-out, delivery receipts, durable history, identity verification, or application-layer end-to-end encryption.
- Not implemented yet: GPS location sharing and the AlpineQuest intent hand-off button.
- The chat transport has not been built or tested on physical devices in this change.

## Important limitation

GPS only receives navigation signals; it does not transmit chat messages. Wi-Fi Direct is the current local data transport. It is short-range and does not provide a guaranteed 20 km link. A 20 km link would require separate compatible radio or satellite communication hardware and an appropriate network design.

Do not use this prototype for emergency, safety-critical, or sensitive communications.
