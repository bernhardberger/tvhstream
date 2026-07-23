# Leoville release signing and deployment

The production `at.leoville.tvhstream` package uses a private Leoville signing
key outside Git. Never commit the keystore, certificate lineage, passwords, or
generated APKs.

## Current production identity

- First production source commit: `e18d4492b9112da2a51b1a015f73774c2b336596`
- Version: `1.0.0-leoville.1` / version code `1`
- Production certificate SHA-256:
  `2C:04:EC:AC:19:B3:47:F3:D7:C9:98:45:0F:C8:A1:0E:4E:B6:AA:1A:94:C4:F4:DB:62:3E:92:E1:50:10:4B:0E`
- First production APK SHA-256:
  `c6bc6fdcfc9edb5865b11ee0228cb0815914f37df58ff7ec51192846eb91168b`
- Android 14 installation verified on TCL V655/G08 with `armeabi-v7a` selected.

The first production install rotated from the exact retained Android debug
certificate to the private Leoville certificate through APK Signature Scheme
v3/v3.1. Rotation starts at API 33, grants installed-data migration, and does
not grant rollback authority to the old debug certificate. This preserved the
TVHeadend configuration, accessibility consent, and TCL `AUTO_START` allowance.

## Runtime-only signing material

The canonical build host is LXC 106. Its runtime files are:

```text
/root/.config/tvhstream/signing.env
/root/.config/tvhstream/release.jks
/root/.config/tvhstream/signing-lineage.bin
/root/.android/debug.keystore
```

All files containing private material are mode `0600`. LXC 106 is covered by
the `tomlin-lxc-core` PBS job. The untracked homelab operator mirror also holds
the passwords and base64 recovery copies under `TVHSTREAM__RELEASE_*`,
`TVHSTREAM__SIGNING_LINEAGE_*`, and `TVHSTREAM__LEGACY_DEBUG_*`; it is recovery
material only and must never be sourced by the app or committed.

The old debug keystore remains required to produce the API 28-32 signer in the
certificate lineage. It is not authorized to roll back the production signer
on the Android 14 appliance.

The first known-good production APK is retained outside the checkout at:

```text
/root/.config/tvhstream/releases/leoville-tv-1.0.0-leoville.1-e18d449.apk
```

## Build and verify

```bash
./tools/build-release
```

The command runs the JVM tests, builds the non-debuggable release APK, applies
the retained signing lineage, runs `apksigner verify`, and prints the final
SHA-256. The output is:

```text
app/build/outputs/apk/release/app-release-production.apk
```

Increment `versionCode` for every subsequent production update and update the
version name when appropriate.

## Deploy

```bash
./tools/device doctor
adb -s 192.168.8.183:5555 install -r \
  app/build/outputs/apk/release/app-release-production.apk
./tools/device package-info
```

The address is now an OpenWrt reservation, but verify device identity before
installing. A failed `install -r` leaves the existing package intact. After a
successful update, verify the installed APK hash and production certificate,
then run the appliance-entry and playback smoke tests.

## Rollback

Keep the last known-good production APK before each update. Roll back by
installing that APK with `adb install -r` when Android permits the version code,
or rebuild the prior source with the same production key and a higher version
code. Do not uninstall the package unless app-data loss and credential
re-entry are explicitly accepted. The old debug-signed APK is not an in-place
rollback after production key rotation.
