# TVHStream Appliance Engineering Guide

This repository is a public GPLv3 fork of
[`Preclikos/tvhstream`](https://github.com/Preclikos/tvhstream). The fork adds a
Leoville appliance mode while preserving upstream history, attribution, and a
clean path for contributing generic improvements back upstream.

## Start here

Before non-trivial work:

1. Read `docs/appliance-mode-spec.md`.
2. Read `docs/device-targets.md` before any build, install, ADB, or physical-TV
   work.
3. Read `docs/appliance-mode-plan.md` and identify the current task.
4. Run `git status -sb` and inspect the recent log.
5. Fetch both remotes before changing code:
   `git fetch origin && git fetch fork`.
6. Run `./tools/check-ai-harness` when changing agents, skills, commands, or
   OpenCode configuration.

`origin` is the upstream repository. `fork` is Bernhard's public fork. Never
push appliance work to `origin`; push normal development branches to `fork`.

## Current product boundary

The target is a single-purpose Android TV live-TV appliance for a household
user who should not need to navigate Google TV. The accepted playback baseline
is upstream TVHStream's Media3/HTSP path.

Current fork identity:

- Application ID: `at.leoville.tvhstream`
- Label: `Leoville TV`
- Minimum SDK: 28
- Target/compile SDK: 36
- Java toolchain: 21
- Required device ABI: `armeabi-v7a`

Do not alter the HTSP extractor, Media3 playback path, or decoder behavior as a
side effect of appliance-shell work. Progressive and interlaced playback are
regression gates.

## Engineering workflow

- State assumptions before ambiguous or architectural work.
- Keep `docs/appliance-mode-spec.md` and `docs/appliance-mode-plan.md` current
  when requirements or architecture change.
- Implement one small slice at a time.
- For behavior changes, write a failing test first, then the minimum code to
  make it pass.
- Run the focused test while iterating and `./tools/verify` before committing.
- Review every diff for secrets, unrelated churn, upstreamability, and GPLv3
  attribution before pushing.
- Do not spawn background or automatic subagents. The project agents are for
  direct selection and explicit review sessions.

Use the project-local skills when relevant:

- `android-tv-device-testing` for ADB, TCL runtime checks, remote keys, playback,
  HOME, standby/wake, and reboot validation.
- `tvhstream-upstream-contribution` when syncing upstream or preparing a generic
  change for an upstream pull request.

## Commands

```bash
# Full local verification
./tools/verify

# AI harness/config validation
./tools/check-ai-harness

# Device wrapper help
./tools/device --help

# Configure a local device without committing its address
cp .tvhstream-device.example.json .tvhstream-device.json
./tools/device doctor
```

The underlying build command is:

```bash
./gradlew testDebugUnitTest assembleDebug --no-daemon
```

## Code layout

- `app/src/main/java/cz/preclikos/tvhstream/` — application source
- `app/src/test/` — JVM unit tests for pure policy and navigation logic
- `app/src/androidTest/` — device/instrumentation tests
- `docs/` — appliance specification, plan, and engineering/operator notes
- `tools/` — repeatable local build and device workflows
- `.opencode/` — project agents, skills, commands, and OpenCode configuration

Keep policy code independent from Android UI where practical so it can be
covered by fast JVM tests. Match the existing Kotlin and Compose style; do not
introduce abstractions for a single use.

## Device and credential safety

- TVHeadend credentials belong only in Android app-private storage. Never put
  them in Git, Gradle properties, scripts, command history, issue text, logs, or
  screenshots.
- Never use `uiautomator dump`, broad `dumpsys`, unrestricted `logcat`, or app
  data exports while a credential field or secret-bearing screen may be
  present. Prefer bounded commands in `./tools/device`.
- Do not add debug-only exported receivers, activities, services, or content
  providers for credential injection.
- Never commit signing keys, keystores, key passwords, service-account JSON, or
  Firebase configuration.
- Runtime device addresses belong in ignored `.tvhstream-device.json` or the
  `TVHSTREAM_ADB_SERIAL` environment variable.
- The dining-room G10 is the current development target and may use local role
  `test`. The bedroom G08 is handed-over production and must use role
  `production`. The exact identities and lifecycle rules are in
  `docs/device-targets.md`.
- Do not modify TVHeadend server accounts, tuners, OSCam, recording storage,
  stream profiles, TCL/Google packages, or network infrastructure from this
  repository unless the user explicitly approves that separate operation.
- Keep Google Basic TV, Headent, and the upstream-package diagnostic client as
  rollback paths until appliance validation is complete.

For physical tests that require the user to press a remote button, observe the
TV, or act within a time window, ask one focused question and wait for the
result. Do not infer a human-visible motion-quality pass from ADB counters.

## Appliance behavior invariants

- Persist only a channel that was actually sent to the player; UI focus alone
  must not change the last-played channel.
- `CH+` and `CH-` wrap through the current ordered channel list.
- Appliance launches are one-shot requests consumed only after channel data is
  available.
- Back from playback returns to the normal UI and must not trigger an autoplay
  loop.
- The accessibility service, when implemented, consumes only GUIDE and must not
  inspect window content or become general UI automation.
- Google/TCL packages remain installed and reversible throughout validation.

## Upstreamability and licensing

Classify each change before implementation:

- **Generic:** useful to TVHStream users without Leoville assumptions. Keep it
  separable and suitable for an upstream pull request.
- **Appliance-specific:** HOME behavior, Leoville identity, TCL GUIDE handling,
  household defaults, deployment, or signing. Keep it in the fork.
- **Mixed:** split the generic foundation from the appliance integration before
  committing.

Do not rewrite upstream attribution or imply that this fork is wholly original.
Distributed combined binaries and corresponding source remain GPLv3.

## Git discipline

- Branch from the appropriate upstream/fork base; do not develop on `master`.
- Keep commits small, buildable, and independently reviewable.
- Do not force-push, rewrite published history, or amend commits unless the user
  explicitly asks.
- Before committing, inspect `git status`, `git diff`, and recent history.
- Before an upstream pull request, compare the proposed commit range against
  `origin/master` and remove Leoville-only assumptions from that range.
