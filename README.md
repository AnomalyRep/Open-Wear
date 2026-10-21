# OpenWear - Wear Level Insight v5.0.0 Kotlin/XML port

OpenWear is a clean-room Kotlin + XML reconstruction of **Wear Level Insight
5.0.0** (`id.vern.wearlevelinsight`), built from static analysis of the APK
supplied by the project owner.

Repository: <https://github.com/AnomalyRep/Open-Wear>

This is **not** the original APK and it is **not** a repacked or patched copy of
it. No original DEX, native library, `assets/main.jar`, or third-party UI
library code is shipped. The resource layer and observable behavior are ported;
the application logic is a new Kotlin implementation.

> Authorized personal reverse engineering. Do not redistribute the original
> application or its artwork.

## v5.0.0 update

The v5.0.0 work is kept on the local `v5.0.0` branch. Nothing has been pushed,
tagged, or published.

v5.0.0 adds:

* **Access Mode** - choose Root or Non-root during onboarding and in Settings.
  Non-root mode uses public sysfs nodes and disables raw partition operations.
* **Battery Health** - state of health, design and estimated capacity, cycle
  count, voltage, charger status, and power. A custom design capacity can be set
  from Settings.
* **Wear Level A / B / Pre-EOL** - pSLC and TLC/MLC wear plus JEDEC
  `PRE_EOL_INFO` and spare-block status.
* **Appearance** - MD3 Expressive or Liquid Glass, with Flat Color or Animated
  Orbs backgrounds. Liquid Glass uses the v5 reference wallpaper for Flat Color,
  the aurora mesh for Animated Orbs, translucent cards, 1dp rims, and 32dp
  dialogs.
* **Compact widgets** - dashboard and wear widgets switch to compact layouts
  when the launcher gives them a small width.
* **Update check** - checks the latest OpenWear GitHub release.
* **v5 disclaimer** - updated privacy, terminology, accuracy, liability,
  unofficial-source, and agreement sections.

The v5 UI is a Compose rewrite in the upstream APK. This port keeps the existing
Kotlin/XML shell and backports the v5 behavior and the most visible v5 concepts;
it is not a pixel-for-pixel Compose recreation.

## Build and install

| Tool | Version |
| --- | --- |
| JDK | 17 (`JAVA_HOME`) |
| Android SDK | Platform 36, Build-Tools 35+ |
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

| Field | Original v5 | OpenWear |
| --- | --- | --- |
| applicationId | `id.vern.wearlevelinsight` | `id.openwear.diskhealth` |
| namespace | `id.vern.wearlevelinsight` | `id.openwear.diskhealth` |
| versionName | `5.0.0` | `5.0.0` |
| versionCode | `5` | `5` |
| minSdk | 29 | 29 |
| targetSdk | 36 | 36 |

The different applicationId means it installs alongside the original. Debug
builds use the local debug key; release builds use the dedicated OpenWear key,
so neither can update the original application.

## Local v5.0.0 release artifact

Built and signed locally. This file has not been uploaded to GitHub.

| Field | Value |
| --- | --- |
| File | `dist/OpenWear-5.0.0-release.apk` |
| Package | `id.openwear.diskhealth` |
| Version | 5.0.0 (versionCode 5) |
| Size | 14,367,389 bytes |
| SHA-256 | `FC9DEA08A349856CBE9C684E66C7810C3F660A8820DAA8BD1C1AAC158E2E611C` |
| Signature | APK Signature Scheme v3 |
| Certificate SHA-256 | `8fd9d94f32760fc5df0891b8b59a9dfa2e2a708c7de081e0cf1a44dd9b8b2dcc` |
| Certificate DN | `CN=OpenWear, OU=OpenWear, O=OpenWear, L=Unknown, ST=Unknown, C=ID` |

This local artifact includes the two v5 reference wallpapers for the owner's
parity build. They are gitignored and must not be redistributed. When the files
are absent, the app falls back to the procedural ambient background.

The v4.0.1 release remains at
<https://github.com/AnomalyRep/Open-Wear/releases/tag/v4.0.1> and is unchanged:
size 14,231,713 bytes, SHA-256
`1544C8C26A88895E5C3C225A8EBE619FFB7AFF9CDC939E2112493691DECA9EC8`.

## Implemented screens and behavior

* **Splash / onboarding** - agreement countdown and scroll gate, access-mode
  choice, root check when Root is selected, all-files / unknown-sources /
  notification permissions, purchase-date picker, credits page, then dashboard
  hand-off.
* **Dashboard** - device panel (`Build`, `/proc`, GPU sysfs, `StatFs`,
  charge-full and `PowerProfile`), battery health row and detail dialog,
  storage card with pie diagram, dynamic wear rows A/B/Pre-EOL/spares, history
  chart, I/O benchmark, reports table, recent-backup list.
* **Wear scan** - eMMC plus modern and vendor UFS. Root mode uses `su`; non-root
  mode uses the public shell and world-readable sysfs nodes.

```text
# 1. eMMC: sysfs first, old /dev path as fallback
find /sys/block/mmcblk0/device/ -type f -name 'life_time*' | sed -e ':a;N;$!ba;s/\n/ /g'
find /dev/block/mmcblk0/device/ -type f -name 'life_time*' | sed -e ':a;N;$!ba;s/\n/ /g'

# 2. UFS: health attributes, usually below /sys/devices/platform/soc
#    DeviceLifeTimeEstA: 0x02
#    DeviceLifeTimeEstB: 0x01
#    PRE_EOL_INFO: 0x01
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

The UFS `health` parser reads `DeviceLifeTimeEstA` / `DeviceLifeTimeEstB` and
`PRE_EOL_INFO`. `0X2`, `2` and `0x02` are normalised to the same lowercase code
before mapping. Codes map `0x01..0x0a` to `100..10 %` and `0x0b` to `1 %` (`EOL`
label). Pre-EOL uses `Normal`, `Warning`, `Urgent`, or `EOL` labels. Results are
stored as `disk_hex_1a/1b/2a/2b`, `disk_single_hex`, `disk_node_a/b`,
`disk_pre_eol`, `disk_single_node`, and `disk_health_type`; stale scan keys are
cleared first.
* **Battery health** - reads BatteryManager properties and public sysfs nodes
  (`cycle_count`, `charge_full`, `charge_full_design`, `soh`, `current_now`).
  Health is native SOH when exposed, otherwise estimated capacity divided by
  design capacity. Custom capacity is stored as an integer preference.
* **History** - `wear_history_a` / `wear_history_b` JSON points
  (`{"t":timestamp,"v":percent}`), initialised from the selected purchase date.
* **I/O benchmark** - 100 MB write/read plus 4 KiB latency; previous results
  are persisted and shown on the card and gauge, matching the original card.
* **Settings** - real `PreferenceFragmentCompat`; access mode, custom battery
  capacity, theme chooser, app appearance, background style, saved preferences,
  dependency list, GitHub source, update check, display-only Telegram row, and
  custom credits card. The GitHub row opens
  <https://github.com/AnomalyRep/Open-Wear>; the Telegram row is explicitly
  non-selectable and launches no intent.
* **Backup / restore** - root mode only. By-name partition enumeration, default
  partition set (`boot`, `fsc`, `fsg`, `dtbo`, `vbmeta`, `modemst1`,
  `modemst2`), backup to `/storage/emulated/0/Backup`, restore with the original
  boot-critical block list (`boot`, `dtbo`, `vbmeta`, `splash`, `logo`), delete,
  progress dialog, reboot prompt. Work runs on `Dispatchers.IO`.
* **Widgets** - dashboard and wear-level widgets, with full and compact layouts
  selected from the launcher width. Broadcast action names remain under
  `id.vern.wearlevelinsight.widgets...` for v4/v5 behavior fidelity.
* **Crash screen** - global uncaught-exception handler opens the crash layout
  with the stack trace; copy, report to GitHub issues, or close.

## Port decisions

| Original dependency / asset | OpenWear |
| --- | --- |
| MPAndroidChart `LineChart` | `ui/view/HistoryChartView.kt` |
| Custom `PieDiagram` / `GaugeView` | Kotlin custom views, same visual contract |
| Coil remote device artwork | Bundled `res/drawable-nodpi/device_*.webp` + `ui/Artwork.kt` |
| libsu root shell | `data/Shell.kt`, direct `su -c` helper |
| LogWire provider | Omitted (diagnostic logging only) |
| `assets/main.jar` (libsu root service + LG Integrity Binder) | Omitted; not needed by the clean-room implementation |
| Compose MD3 / Liquid Glass UI | XML shell with MD3 styling and optional gradient/orb backgrounds |
| v4 hard-coded `#b6202020` text | Static v4 palette with theme-aware text colors |
| v4 Material 1.14.0-alpha09 | Material 1.12.0 (stable, same screens) |

The resource set is based on the decoded v4/v5 APKs. A few duplicated Material
and AppCompat library layouts were removed so Gradle can use the AAR copies; the
app does not use those dialogs directly.

## Verification

* `.\gradlew.bat assembleDebug` - **BUILD SUCCESSFUL**.
* `aapt2 dump badging` confirms `id.openwear.diskhealth`, versionCode 5,
  versionName 5.0.0, compileSdk 36, minSdk 29, targetSdk 36.
* `apksigner verify --print-certs` confirms the v3 signature and the OpenWear
  certificate listed above.
* Release APK SHA-256, size, and certificate are recorded in the local release
  artifact table. Nothing is pushed until the project owner explicitly approves
  it.

## Known limitations

* **No on-device runtime test was possible.** `adb devices` was empty and no
  AVD was installed in this environment. Everything above is static/build
  verification plus a code review. Please test on a device you own.
* The upstream v5 UI is Compose. This port keeps the v4 XML screen structure and
  backports v5 behavior instead of recreating every Compose animation and glass
  effect pixel-for-pixel.
* Non-root mode can only read nodes the kernel exposes to the app. Some vendors
  hide wear, cycle, or battery-health data behind root; those values will show
  as unavailable instead of being fabricated.
* Partition backup and restore require Root access mode. Non-root mode opens a
  clear explanation and does not attempt raw block operations.
* UFS health attribute names and locations vary by vendor kernel. OpenWear
  covers `DeviceLifeTimeEstA/B`, SCSI `health`, vendor `life_time*`/`lifetime*`,
  `PRE_EOL_INFO`, and `/proc/ufs_health`; an exotic kernel may still need a
  device-specific path.
* Backup/restore performs raw `dd` writes. Restoring a wrong or damaged image
  can brick the device. The original boot-critical block list is enforced, but
  that is not a substitute for a full device backup.
* `compileSdk`/`targetSdk` are 36 with AGP 8.5.2 and the Gradle property
  `android.suppressUnsupportedCompileSdk=36`. AGP prints an unsupported-compile
  warning, but the build succeeds.
* Device artwork is bundled offline from the original `i.ibb.co` assets. The
  original codename-to-artwork table was R8-flattened; the offline matcher
  covers the recovered codenames and falls back to `device_unknown.webp`.
* The Settings "Library" dialog intentionally lists the original dependency
  versions for fidelity, not OpenWear's Gradle dependencies.
* The custom chart/gauge views reproduce the original contract, not necessarily
  every pixel of MPAndroidChart/GaugeView rendering.
* Some developer-facing strings remain English only, as in the original.

## Quick device test checklist

1. Install the debug APK and open it. Accept the disclaimer after scrolling.
2. Choose Root or Non-root. For Root, grant superuser access.
3. Grant all-files access, unknown-sources (for updates), and notifications.
4. Pick the real purchase date; confirm the dashboard opens.
5. Check the battery health row and dialog.
6. Check wear A/B/Pre-EOL rows and `Storage Health N %`; run the benchmark.
7. Open Settings, switch theme, appearance, and background; set a custom battery
   capacity; copy saved preferences and dependencies; open the credits card.
8. Add both widgets and resize them to confirm compact layouts.
9. Back up a non-critical partition, then restore it; verify the reboot prompt.
10. Do **not** test restore on `boot`/`dtbo`/`vbmeta`/`splash`/`logo` unless you
    have a known-good full backup and a recovery path.

## Provenance

* v5 target: `app-debug-v5.0.0.apk`
* v5 SHA-256: `4A83BE253055E553111A46CE8C742F32CBEF14207FC9939FD55BC1DADEDBFCCF`
* v5 size: 70,750,012 bytes
* v5 package: `id.vern.wearlevelinsight`, versionCode 5, versionName 5.0.0,
  targetSdk 36, minSdk 29, signed by the original KuatoDev certificate.
* v5 is a debug build and is used for static analysis only. It is not shipped or
  repacked.
* v4 target: `Wear-Level-Insight-v4.0.1.apk`
* v4 SHA-256: `E1CB553DEA1BB7A77DFD43F0D0D7E923208FB27F1B80444990CF4CD551322D3F`
* v4 size: 5,730,832 bytes
* Analysis was static only; the original APKs were never executed as part of the
  reconstruction.
* Consolidated provenance lives in `data/wearlevel-insight.json`. Earlier 3.6.0
  analysis is retained as `data/wearlevel-insight-3.6.json`.
