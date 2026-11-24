# Local Call

Voice calls between phones on the same Wi-Fi or hotspot. No internet, no accounts, no servers.

## How it works

| Layer | Package | Job |
|---|---|---|
| Network | `network/` | UDP broadcast discovery (`PeerDiscovery`) and call signaling (`ControlChannel`, port 47474) |
| Audio | `audio/` | Mic capture with echo cancellation, playback, UDP voice stream (port 47475), speaker routing |
| Call | `call/` | `CallEngine` state machine: ring, accept, reject, hang up, connection watchdog |
| Service | `service/` | Foreground service so a call keeps running in the background |
| UI | `ui/` | Jetpack Compose: peers list, call screen (mute, speaker, timer), permission handling |

Audio is 16 kHz mono 16-bit PCM in 20 ms frames (about 256 kbit/s, uncompressed).

## Features

- Automatic peer discovery on Wi-Fi or when one phone is the hotspot host
- Incoming call screen with accept and decline
- Mute, speaker toggle, call timer, end call
- Echo cancellation and noise suppression where the device supports them
- Editable display name
- Detects dropped connections and busy peers

## Requirements and limits

- **minSdk 21 (Android 5.0).** Jetpack Compose cannot run below API 21, so Android 4.x is not possible with Compose.
- Calls are unencrypted and unauthenticated, so use it on networks you trust.
- Some routers enable "client isolation" (AP isolation), which blocks phone-to-phone traffic. Discovery will find nothing there.
- Release builds use R8 (code shrinking, optimization, obfuscation) and resource shrinking.
- Build tooling (Gradle, Android Gradle plugin, Kotlin) is the newest. App libraries (Compose, lifecycle, activity, core) stay on versions that still support Android 5.0, because newer lifecycle releases require Android 6.0.
- The CI APK is signed with the debug key so it installs directly. Add a real keystore before distributing.

## Build

Push to `main` or `master`, or run the workflow manually, and download the `local-call-apk` artifact from the Actions run.

Locally: `gradle assembleRelease` (latest Gradle, JDK 17).
