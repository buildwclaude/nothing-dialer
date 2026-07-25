# Phone — dialer app for Nothing Phone (3a)

A native Android **default Phone app** (dialpad, recents, contacts, and the in-call
screen) for the Nothing Phone (3a), built as a companion to the Messages app. Sideload only.

- **Carrier voice calls only.** Calls go over your SIM through Android's Telecom stack.
  No accounts, no servers, no VoIP.
- **No Google dependencies, no internet permission.** CI fails the build if any network
  permission is ever present in the APK.
- Becomes your default phone app via `ROLE_DIALER`; implements `InCallService` so it can
  show its own call screens. Dual-SIM aware.

## Get the APK on your phone

Open this repo on your phone → **`apk/`** folder → tap **`Phone.apk`** → Download → install.
Every push rebuilds and updates `apk/Phone.apk` automatically.

## Set as default phone app

Settings → Apps → Default apps → **Phone app** → choose **Phone**. (Sideloaded apps with
call-log/phone permissions hit the same "restricted settings" gate as the Messages app —
if a permission is greyed out, use the app-info ⋮ menu → "Allow restricted settings".)

## Build system

GitHub Actions (JDK 17, Gradle 8.13, `assembleRelease`), signed with a PKCS12 keystore in
repo secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
