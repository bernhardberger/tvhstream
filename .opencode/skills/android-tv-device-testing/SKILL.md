---
name: android-tv-device-testing
description: Use for Android TV or TCL ADB testing, APK installation, playback checks, remote keys, HOME, GUIDE, standby/wake, reboot, and device diagnostics in this TVHStream fork.
---

# Android TV Device Testing

Use the repository's bounded `./tools/device` wrapper instead of ad-hoc broad ADB
dumps whenever it supports the required operation.

## Before touching the device

1. Read `AGENTS.md` and the runtime criteria in
   `docs/appliance-mode-spec.md`.
2. Confirm the source tree is clean or identify the exact uncommitted slice.
3. Run the relevant JVM test, then `./tools/verify` before installing.
4. Configure the ADB serial through ignored `.tvhstream-device.json`,
   `TVHSTREAM_ADB_SERIAL`, or `--serial`. Never commit a household device
   address as a required default.
5. Confirm the package under test. The appliance default is
   `at.leoville.tvhstream`; rollback clients use different package IDs.

## Safe sequence

```bash
./tools/device doctor
./tools/device install-debug
./tools/device force-stop
./tools/device launch
./tools/device current
./tools/device package-info
```

Use named key commands rather than numeric key codes:

```bash
./tools/device key channel-up
./tools/device key channel-down
./tools/device key guide
./tools/device key home
./tools/device key back
./tools/device key power
```

## Verification matrix

For an appliance-affecting change, record the applicable checks:

- APK installs on the 32-bit `armeabi-v7a` Android TV target.
- Fresh launch connects and reaches the expected UI or live channel.
- Progressive playback remains smooth.
- Interlaced sports playback passes direct human motion-quality review.
- Physical `CH+` and `CH-` zap correctly and wrap.
- Last played channel survives force-stop and relaunch.
- Back exits playback without an autoplay loop.
- HOME reaches the expected appliance entry path.
- GUIDE/TV is intercepted only when the accessibility service is enabled.
- Standby/wake behavior is correct.
- Cold reboot retains the chosen HOME and accessibility state.
- Google Basic TV and rollback clients still launch directly.

When a check requires a physical remote action or human-visible judgment, ask
one focused question and wait. ADB subscription or decoder counters do not prove
acceptable motion quality.

## Secret and privacy boundary

- Do not dump UI hierarchies on connection/settings/password screens.
- Do not print SharedPreferences, DataStore, Android Keystore entries, app-private
  files, full `dumpsys`, or unrestricted `logcat` output.
- Do not type credentials through an uncertain focus state.
- Do not add exported debug components to inject credentials.
- If a secret appears in output, stop, rotate it, verify the old value is
  rejected, and remove the exposure path before continuing.
