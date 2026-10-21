# OpenWear — Wear Level Insight v4.0.1 Kotlin/XML port

OpenWear is a clean-room Kotlin + XML reconstruction of **Wear Level Insight
4.0.1** (`id.vern.wearlevelinsight`), built from static analysis of the APK
supplied by the project owner.

Repository: <https://github.com/AnomalyRep/Open-Wear>

This is **not** the original APK and it is **not** a repacked or patched copy of
it. No original DEX, native library, `assets/main.jar`, or third-party UI
library code is shipped. The resource layer (layouts, drawables, fonts,
translations, widget XML, disclaimer HTML) and the observable behavior were
ported; the application logic is a new Kotlin implementation.

> Authorized personal reverse engineering. Do not redistribute the original
> application or its artwork.

## Build and install

| Tool | Version |
| --- | --- |
| JDK | 17 (`JAVA_HOME`) |
| Android SDK | Platform 34, Build-Tools 34+ |
| Gradle | 8.7 (wrapper included) |
| AGP / Kotlin | 8.5.2 / 1.9.24 |

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
.\gradlew.bat assembleDebug
```

Debug output: `app\build\outputs\apk\debug\app-debug.apk`

Signed release builds are published on
[GitHub Releases](https://github.com/AnomalyRep/Open-Wear/releases).
The release APK is signed with a dedicated OpenWear key; the keystore is kept
outside this repository and is not committed.

Package identity:

| Field | Original | OpenWear |
| --- | --- | --- |
| applicationId | `id.vern.wearlevelinsight` | `id.openwear.diskhealth` |
| namespace | `id.vern.wearlevelinsight` | `id.openwear.diskhealth` |
| versionName | `4.0.1` | `4.0.1` (UI/credits fidelity) |
| versionCode | `2` | `2` |
| minSdk | 29 | 29 |
| targetSdk | 36 | 34 |

The different applicationId means it installs alongside the original. Debug
builds use the local debug key; release builds use the dedicated OpenWear key,
so neither can update the original application.

## Implemented screens and behavior

* **Splash / onboarding** — agreement countdown and scroll gate, root check,
  all-files / unknown-sources / notification permissions, purchase-date picker,
  credits page, then dashboard hand-off.
* **Dashboard** — device panel (`Build`, `/proc`, GPU sysfs, `StatFs`,
  charge-full and `PowerProfile`), storage card with pie diagram, eMMC/UFS wear
  groups A/B, history chart, I/O benchmark, reports table, recent-backup list.
* **Wear scan** — eMMC plus modern and vendor UFS, same preference keys as
  4.0.1:

```text
# 1. eMMC: sysfs first, old /dev path as fallback
find /sys/block/mmcblk0/device/ -type f -name 'life_time*' | sed -e ':a;N;$!ba;s/\n/ /g'
find /dev/block/mmcblk0/device/ -type f -name 'life_time*' | sed -e ':a;N;$!ba;s/\n/ /g'

# 2. UFS: health attributes, usually below /sys/devices/platform/soc
#    DeviceLifeTimeEstA: 0x02
#    DeviceLifeTimeEstB: 0x01
ls -1d /sys/devices/platform/soc/*ufshc*/health \
       /sys/devices/platform/soc/*ufs*/health \
       /sys/class/scsi_host/host*/device/health \
       /sys/class/scsi_device/*/device/health \
       /sys/block/sd*/device/health 2>/dev/null | head -n 1
cat <health path>

# 3. vendor UFS/eMMC nodes: life_time*, then lifetime*
find /sys/devices/ -type f -name 'life_time*' | sed -e ':a;N;$!ba;s/\n/ /g'
find /sys/devices/ -type f -name 'lifetime*'  | sed -e ':a;N;$!ba;s/\n/ /g'

# 4. vendor kernel module fallback
cat /proc/ufs_health
```

The UFS `health` parser reads `DeviceLifeTimeEstA` / `DeviceLifeTimeEstB`
directly, so devices that previously showed `Unknown` can now report for both
groups. `0X2`, `2` and `0x02` are normalised to the same lowercase code before
mapping. Codes map `0x01..0x0a` to `100..10 %` and `0x0b` to `1 %` (`EOL`
label), exactly like the original. Results are stored as
`disk_hex_1a/1b/2a/2b`, `disk_single_hex`, `disk_node_a/b`,
`disk_single_node`, and `disk_health_type`; stale scan keys are cleared first.
* **History** — `wear_history_a` / `wear_history_b` JSON points
  (`{"t":timestamp,"v":percent}`), initialised from the selected purchase date.
* **I/O benchmark** — 100 MB write/read plus 4 KiB latency; previous results
  are persisted and shown on the card and gauge, matching the original card.
* **Settings** — real `PreferenceFragmentCompat` using the original
  `res/xml/settings.xml`; theme chooser applies immediately, saved preferences
  and dependency list dialogs copy to the clipboard, GitHub link, display-only
  Telegram row, custom credits card. The GitHub row opens
  <https://github.com/AnomalyRep/Open-Wear>; the Telegram row is explicitly
  non-selectable and launches no intent.
* **Backup / restore** — by-name partition enumeration, default partition set
  (`boot`, `fsc`, `fsg`, `dtbo`, `vbmeta`, `modemst1`, `modemst2`), backup to
  `/storage/emulated/0/Backup`, restore with the original boot-critical block
  list (`boot`, `dtbo`, `vbmeta`, `splash`, `logo`), delete, progress dialog,
  reboot prompt. Work runs on `Dispatchers.IO`.
* **Widgets** — dashboard widget and wear-level widget, including the original
  literal broadcast action names under `id.vern.wearlevelinsight.widgets...`
  for v4 behavior fidelity.
* **Crash screen** — global uncaught-exception handler opens the 4.0.1 crash
  layout with the stack trace; copy / report to GitHub issues / close.

## Port decisions

| Original dependency / asset | OpenWear |
| --- | --- |
| MPAndroidChart `LineChart` | `ui/view/HistoryChartView.kt` |
| Custom `PieDiagram` / `GaugeView` | Kotlin custom views, same visual contract |
| Coil remote device artwork | Bundled `res/drawable-nodpi/device_*.webp` + `ui/Artwork.kt` |
| libsu root shell | `data/Shell.kt`, direct `su -c` helper |
| LogWire provider | Omitted (diagnostic logging only) |
| `assets/main.jar` (libsu root service + LG Integrity Binder) | Omitted; not needed by the clean-room implementation |
| v4 hard-coded `#b6202020` text | Static v4 palette with theme-aware text colors |
| v4 Material 1.14.0-alpha09 | Material 1.12.0 (stable, same screens) |

The resource set comes from the decoded 4.0.1 APK. A few duplicated Material /
AppCompat library layouts were removed so Gradle can use the AAR copies; the
app does not use those dialogs directly.

## Verification

* `.\gradlew.bat assembleRelease` — **BUILD SUCCESSFUL**.
* `.\gradlew.bat lintDebug` — **BUILD SUCCESSFUL** (0 errors; warnings are
  mostly inherited translation/`NewApi` notices from the ported resources).
* `aapt2 dump badging` confirms:
  * `id.openwear.diskhealth`, versionCode 2, versionName 4.0.1
  * minSdk 29, targetSdk 34
  * 5 activities, 2 widget receivers, both widget metadata files.
* Release APK: `OpenWear-4.0.1-release.apk` on
  [GitHub Releases](https://github.com/AnomalyRep/Open-Wear/releases).
  * SHA-256: `1544C8C26A88895E5C3C225A8EBE619FFB7AFF9CDC939E2112493691DECA9EC8`
  * Size: 14,231,713 bytes
  * Signed with APK Signature Scheme v3; certificate SHA-256
    `8fd9d94f32760fc5df0891b8b59a9dfa2e2a708c7de081e0cf1a44dd9b8b2dcc`
  * The release keystore is kept outside the repository.

## Known limitations

* **No on-device runtime test was possible.** `adb devices` was empty and no
  AVD was installed in this environment. Everything above is static/build
  verification plus a code review. Please test on a rooted device you own.
* The app is **root-only in practice**: reading eMMC/UFS sysfs, `/proc/ufs_health`
  and raw partitions requires a working `su`. The helper supports common `su`
  paths and `su -c`; some SELinux policies or root managers may still deny
  `dd`/`find`/`cat`. The original used libsu and `su -mm -c`; this clone does not
  ship libsu.
* UFS health attribute names and locations vary by vendor kernel. OpenWear
  covers `DeviceLifeTimeEstA/B`, SCSI `health`, vendor `life_time*`/`lifetime*`,
  and `/proc/ufs_health`; an exotic kernel may still need a device-specific path.
* Backup/restore performs raw `dd` writes. Restoring a wrong or damaged image
  can brick the device. The original boot-critical block list is enforced, but
  that is not a substitute for a full device backup.
* `targetSdk` is 34 instead of the original 36, constrained by the installed
  AGP/compile SDK. Behavior on Android 15/16 has not been exercised.
* Device artwork is bundled offline from the original `i.ibb.co` assets. The
  original codename→artwork table was R8-flattened; the offline matcher covers
  the recovered codenames and falls back to `device_unknown.webp`.
* The Settings “Library” dialog intentionally lists the **original 4.0.1**
  dependency versions for fidelity, not OpenWear’s Gradle dependencies.
* The custom chart/gauge views reproduce the original contract, not necessarily
  every pixel of MPAndroidChart/GaugeView rendering.
* Some developer-facing strings remain English only, as in the original.
* The repository has no `.git` metadata; this is a working source tree.

## Quick device test checklist

1. Install the debug APK, open it, accept the disclaimer after scrolling.
2. Grant root, all-files access, unknown-sources (for updates), notifications.
3. Pick the real purchase date; confirm the dashboard opens.
4. Check wear groups A/B and `Storage Health N %`; run the benchmark.
5. Open Settings, switch light/dark/system theme, copy saved preferences and
   dependencies, open the credits card.
6. Add both widgets from the launcher; tap the wear widget to rescans.
7. Back up a non-critical partition, then restore it; verify the reboot prompt.
8. Do **not** test restore on `boot`/`dtbo`/`vbmeta`/`splash`/`logo` unless you
   have a known-good full backup and a recovery path.

## Provenance

* Target: `Wear-Level-Insight-v4.0.1.apk`
* SHA-256: `E1CB553DEA1BB7A77DFD43F0D0D7E923208FB27F1B80444990CF4CD551322D3F`
* Size: 5,730,832 bytes
* Signature: APK Signature Scheme v3 only; original publisher certificate
  SHA-256 `40079660368dd70dc33506e4eec90df94d8cab7a49d46f55dbbf24765c84f05b`
  (historical signer DN omitted after the OpenWear rebrand).
* Analysis was static only; the original APK was never executed as part of the
  reconstruction.
* Consolidated v4.0.1 provenance lives in `data/wearlevel-insight.json`. The
  earlier 3.6.0 analysis is retained as `data/wearlevel-insight-3.6.json`.
