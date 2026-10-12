# Kourosh Gram

A real Android Telegram source build with its own name, icon and package ID.
The messenger UI is the original Telegram Android UI, not a simulated Compose chat list.

## Download

Open [Releases](https://github.com/xHouman/KouroshGram/releases) and download **Kourosh-Gram.apk** from the newest **Telegram source build**.
This APK supports **arm64-v8a** Android phones (including Samsung A34) and **x86_64** emulators. Android 6+.

## First launch

1. Obtain your own `api_id` and `api_hash` at https://my.telegram.org/apps.
2. Enter them in Kourosh Gram's one-time setup screen, locally on your phone.
3. Continue to Telegram's original sign-in flow using your existing Telegram account.

Do **not** send your login code, password or API Hash in a public GitHub issue. No credentials are committed to this repository. Developer API values are stored in this app's private preferences, excluded from Android backup. They are not a bot token.

## What this build contains

- Original Telegram chat list, chats, media UI, folders, themes, animations, contacts, settings, groups and channels.
- Kourosh Gram identity, custom speech-bubble K icon, fixed branding across language packs.
- Separate `com.kouroshgram.messenger` installation alongside Telegram and the old KouroshGram prototype.
- Reproducible branding overlay and GitHub Actions APK build, signature check, and emulator startup smoke test.

## Current limits

- The APK is a **test build**, signed with a newly generated test certificate per CI run. Uninstalling an earlier test build may be needed to install a later one. A stable private release-signing key should be configured before production distribution.
- Real phone login and messaging require your own Telegram developer API credentials and network access; CI does not authenticate a Telegram user.
- This fork has **no Firebase push project, Google Maps API key, Google sign-in identity or passkeys** configured. Background push delivery and Google-backed services are not promised. Ordinary in-app Telegram networking uses upstream code.
- This is an unofficial client, not an independent messaging server and not affiliated with Telegram.
- `android-app/` and `.payload/` preserve the previous offline prototype for history. The new build does not use them.

## Source and build

Pinned upstream: [DrKLO/Telegram](https://github.com/DrKLO/Telegram/tree/f2908b14133bbffbf7ab04f641ecb5bfaf533242), Telegram Android **12.10.6**.

```sh
git clone --recurse-submodules https://github.com/xHouman/KouroshGram.git
cd KouroshGram
python3 scripts/prepare_telegram.py
python3 scripts/verify_overlay.py
# Install JDK 17, Gradle 8.13, Android SDK 36, NDK 27.2.12479018, CMake 3.22.1.
# Replace telegram/TMessagesProj/config/release.keystore with your OWN test/release key.
cd telegram
gradle :TMessagesProj_App:assembleAfatDebug
```

Run the overlay once on a clean pinned upstream checkout. The workflow is the complete build recipe. Original Telegram sources and native dependency sources are pinned Git submodules; all local changes are readable under `branding/` and `scripts/prepare_telegram.py`.

## License

GPL-2.0-or-later; see [LICENSE](LICENSE). Telegram source copyright belongs to its original authors. Kourosh Gram modifications (2026) are distributed under the same license. Upstream subcomponents retain their respective licenses and notices. API terms: https://core.telegram.org/api/terms.
