# TROY "Şansına Merhaba"

An interactive in-store campaign game for TROY (Apple Premium Partner, Türkiye), built by [RozyLabs](https://rozylabs.com). The app runs full-screen on a retail robot's tablet display: customers tap the screen, pick one of four cards, and win a discount they redeem in-store via QR code.

**Downloads**

| Asset | Link |
|---|---|
| 📱 Android APK (latest) | [Troy_Sansina_Merhaba.apk](https://github.com/kcandost/troy-sansina-merhaba/releases/latest/download/Troy_Sansina_Merhaba.apk) |
| 📄 Store Operations Guide (Turkish, PDF) | [Troy_Sansina_Merhaba_Kullanim_Kilavuzu.pdf](docs/Troy_Sansina_Merhaba_Kullanim_Kilavuzu.pdf) |

---

## Overview

The experience is a six-step loop that runs unattended all day:

1. **Invite** — "Bugünkü şansına merhaba demek ister misin?" with an animated call to action and a periodic robot-voice prompt
2. **Card selection** — four TROY cards slide in; the customer taps one
3. **Shuffle** — ~2.5 seconds of card shuffling builds anticipation
4. **Flip** — the chosen card turns with a light-burst effect
5. **Reward** — the winning amount, confetti, and the campaign message
6. **QR & reset** — a QR code holds for a configurable interval, then the screen returns to the invite

Prize amounts and their odds are fully configurable from a hidden, PIN-protected settings panel (amounts, weighted probabilities, QR dwell time, card-back art, and per-amount win statistics).

## Robot Pause-on-Touch

The kiosk shares its chassis with a Saha Robotik cleaning robot. So the robot never drives off mid-game, the app halts it whenever a visitor is interacting — the same mechanism Rozy Assistant uses:

- Any screen touch pauses the robot (`POST <robot>/api/v1/tasks/pause`), sent once per engagement
- The robot resumes (`POST <robot>/api/v1/tasks/resume`) 60 seconds after the **last** touch — a sliding window, restarted on every tap
- Resume retries with escalating backoff and then indefinitely; a persisted crash-recovery flag resumes the robot on next app start if the process died while it was paused
- The robot's local API answers HTTP 200 even when it refuses a command, so confirmation is read from the JSON envelope's `success` field, not the status code

Every robot serves this API at the same fixed address on its own local network, so there is nothing to configure and nothing to mistype in the field: the address is the `RobotPause.BASE_URL` constant. The settings panel's **Robot** section offers a read-only connection test against `GET /api/v1/status`, which reports the robot's state, battery, and any condition that would keep it stationary after a resume (e-stop, blocked navigation, charging).

## Design

The UI is a faithful implementation of the agency's final campaign designs (`TROY_ROZYLABS` Figma file, frames 5–11): the TROY-blue diagonal light-beam wash, the SEKIL wave pattern, ARTI plus marks, the framed idle screen, and the exact type ramp. Vector assets are exported straight from the Figma source and shipped as Android vector drawables.

The agency typeface (BR Candor) is licensed; the app bundles [Nunito](https://fonts.google.com/specimen/Nunito) as the closest open substitute. Drop the licensed TTFs into `android/app/src/main/res/font/` to swap it in.

Five earlier design-exploration themes (A–E) remain selectable in settings for preview purposes.

## Repository Layout

```
android/            Kotlin / Jetpack Compose app (single module)
brand/              TROY brand assets, curated product imagery, Figma reference exports
design-system/      Design tokens (JSON source of truth + CSS/TS builds) and specimen page
design-explorations/ Early concept canvases
docs/               Brand audit, design principles, component specs, and the client guide (PDF)
requirements.md     Product and motion requirements
```

## Building

Requirements: JDK 17+, Android SDK 35, Gradle 8.13.

```bash
cd android
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Target device is a 1920×1200 (16:10) landscape tablet; `minSdk 26`.

### Robot voice (optional)

The invite and win announcements are pre-baked ElevenLabs lines shipped as assets. To regenerate them:

```bash
ELEVENLABS_API_KEY=... ELEVENLABS_VOICE_ID=... python3 android/scripts/generate_voice.py
```

## Operations

Store staff access settings through an invisible touch target inside the top-left corner of the frame, gated by a PIN. The full walkthrough — game flow, settings reference, and operational notes — is in the [store operations guide](docs/Troy_Sansina_Merhaba_Kullanim_Kilavuzu.pdf).

---

© RozyLabs. TROY brand assets belong to their respective owners and are included for this campaign only.
