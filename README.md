<div align="center">
  <img src="docs/icon.png" width="96" alt="Power Menu icon" />

  # Power Menu

  Open your phone's power off / restart menu without the physical power button.

  [![Download APK](https://img.shields.io/badge/Download-APK-FF3B30?style=for-the-badge&logo=android&logoColor=white)](https://github.com/moansari1234/PowerMenu/releases/latest/download/PowerMenu.apk)
  [![Debug APK](https://img.shields.io/badge/Debug-APK-555555?style=for-the-badge&logo=android&logoColor=white)](https://github.com/moansari1234/PowerMenu/releases/latest/download/PowerMenu-debug.apk)
  [![Latest release](https://img.shields.io/github/v/release/moansari1234/PowerMenu?style=for-the-badge)](https://github.com/moansari1234/PowerMenu/releases/latest)
</div>

## What it does

- One big button opens Android's own **power dialog** (Power off, Restart, Emergency), handy when the power button is broken, worn or hard to reach.
- Works through an **accessibility service** that only performs the "show power dialog" action. It does not read your screen, keystrokes or notifications.
- Use Android's **accessibility shortcut** (floating button or volume-key shortcut) to open the power menu from anywhere.
- **Built-in updates** from GitHub: the app checks for a new release (at most twice a day, can be turned off) and installs it in two taps.
- About 1 MB, no ads, no tracking. The only network request is the update check to GitHub.

## Install

1. Download **[PowerMenu.apk](https://github.com/moansari1234/PowerMenu/releases/latest/download/PowerMenu.apk)** on your phone.
2. Open it. If Android asks, allow your browser or file manager to **install unknown apps**.
3. Open **Power Menu** and tap **Grant Access In Settings**.
4. In Accessibility settings, find **Power Menu** (under *Downloaded apps* or *Installed apps*) and turn it on.
5. Go back to the app and tap the red button.

Requires Android 7.0 (API 24) or newer. On Android 13+, sideloaded apps may first need **App info → ⋮ → Allow restricted settings** before the accessibility switch can be turned on.

### Release vs debug APK

| APK | Use it for | Package |
| --- | --- | --- |
| `PowerMenu.apk` | Everyday use. Minified, about 1 MB. | `com.aistudio.powermenu.qzpxrt` |
| `PowerMenu-debug.apk` | Testing and bug reports. Installs next to the release build. | `com.aistudio.powermenu.qzpxrt.debug` |

Each release also lists `SHA256SUMS.txt` so you can check a download.

## Updating

Open the app and tap the **gear icon** (top right) → **Check for updates** → **Update to vX.Y.Z**, then confirm on Android's install screen. The first time, Android asks you to allow the app to install updates.

With **Check for updates automatically** on, the app looks for a new version when it opens (at most every 12 hours) and offers it in a dialog. Release builds update from the release APK, debug builds from the debug APK.

## Permissions

| Permission | Why |
| --- | --- |
| Accessibility service | Open the system power dialog (`GLOBAL_ACTION_POWER_DIALOG`); it reads no window content |
| Internet | Check GitHub for updates and download them |
| Install packages | Hand a downloaded update to Android's installer (you always confirm) |

## Build from source

```bash
git clone https://github.com/moansari1234/PowerMenu.git
cd PowerMenu
gradle assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
gradle assembleRelease      # app/build/outputs/apk/release/app-release.apk
```

Needs JDK 21 and Gradle 9.3+ (or open the folder in Android Studio). Kotlin, Jetpack Compose, Material 3.

## Releasing a new version

Push a tag, or run the **Release** workflow from the Actions tab with a version number:

```bash
git tag v1.0.1 && git push origin v1.0.1
```

The workflow builds both APKs, signs them and publishes the GitHub release that the in-app updater reads. Versions must be `X.Y.Z`; the version code is derived from it, so every release must be higher than the last.

**Signing.** Updates only install over an app signed with the same key. Published APKs are signed with a private key in `keystore/release.p12`, encrypted with a random password kept only in the `SIGNING_PASSWORD` Actions secret, so nobody else can sign an update. Certificate SHA-256: `88:BF:46:1A:11:68:A5:05:07:D5:F1:6C:BF:FD:C9:6D:2D:05:6A:D2:70:1F:CC:D4:58:A5:16:7C:1B:9F:78:D1`. Local builds without that secret use the standard Android debug key and won't update a published install. APKs from v1.0.x used an old public key: uninstall them once before installing v1.1.0 or later.

## Testing

The **Emulator test** workflow boots an Android 15 emulator, installs the debug APK, enables the accessibility service and checks that the real power dialog opens (`scripts/emulator-test.sh`). Run it with an **old version** (for example `0.9.0`) to test the updater end to end (`scripts/update-test.sh`).

## Project layout

```
app/src/main/java/com/example/
  MainActivity.kt                       setup guide and power button
  PowerMenuAccessibilityService.kt      opens the power dialog
  update/                               GitHub updater, Settings screen, install receiver
```
