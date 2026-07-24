# Leoville TV

Leoville TV is the current working identity for an experimental, remote-first
live TV client for Android TV and Google TV. It connects directly to a
TVHeadend server over HTSP and uses AndroidX Media3 for playback.

The final public product name has not been selected. Package identity, artwork,
and repository branding will change before the first stable release.

## Current capabilities

- TVHeadend channel synchronization and live playback over HTSP
- Channel list and electronic programme guide
- D-pad, channel-up/down, and direct channel-number navigation
- Audio track, subtitle, aspect-ratio, and stream-profile controls
- Encrypted app-private password storage
- Last-played-channel restoration
- Optional household appliance entry through a narrowly scoped accessibility
  service that handles only GUIDE/TV entry and does not inspect screen content

## Status

The project is under active hardening and is not a stable release. The accepted
playback baseline is tested on an Android TV 12 TCL device, including progressive
and interlaced broadcasts, but routine development must use a separate test TV.

The intended distribution path is signed GitHub releases first while retaining
a path to Google Play requirements. No deployment workflow is currently enabled.

See:

- `docs/appliance-mode-spec.md` for current appliance behavior
- `docs/appliance-mode-plan.md` for implementation and release gates
- `docs/codebase-audit-2026-07-23.md` for the technical audit
- `docs/product-identity-plan.md` for naming and migration decisions

## Build and verify

Requirements are Android SDK 36 and Java 21.

```bash
./tools/verify
```

The verifier runs JVM tests, Android lint, instrumentation-test compilation,
debug APK assembly, and package/SDK/ABI metadata checks.

Device operations use an ignored local configuration and the bounded wrapper:

```bash
cp .tvhstream-device.example.json .tvhstream-device.json
./tools/device doctor
```

Never put TVHeadend credentials, signing keys, or private device addresses in
Git. Direct HTSP traffic is not encrypted and should be used only on a trusted
LAN or through a protected tunnel.

Designated test devices can be configured non-interactively through the bounded,
debug-only workflow in `docs/test-device-credential-provisioning.md`. It remains
blocked for production and unclassified devices.

## Fork and upstream

This repository is a GPLv3 fork of
[Preclikos/tvhstream](https://github.com/Preclikos/tvhstream). It retains the
upstream history and attribution. Generic improvements are kept separable for
possible upstream contribution; Leoville identity, TCL integration, and
household appliance policy remain fork-specific.

Upstream TVHStream acknowledges ideas and code from
[TVHClient](https://github.com/rsiebert/TVHClient). Native decoder binaries also
require corresponding source and third-party notices before distribution; that
provenance work is tracked in the hardening plan.

## License

The combined work is licensed under the GNU General Public License v3.0. See
`LICENSE`. Distribution of binaries must be accompanied by the corresponding
source and applicable third-party license material.
