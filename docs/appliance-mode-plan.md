# Implementation plan: Leoville TV appliance mode

## Architecture decisions

- Keep upstream's Media3/HTSP playback path unchanged because the live TCL test
  passed the human motion-quality gate.
- Use a distinct `at.leoville.tvhstream` application ID so the accepted
  diagnostic build and Headent remain installable rollback clients.
- Keep UI focus selection in memory, but persist the last channel actually sent
  to the player in Preferences DataStore.
- Represent appliance launches as one-shot requests owned by `MainActivity`.
  Compose consumes a request only after channels are available. Closing the
  player with Back does not create a new request.
- Register for the HOME role without disabling the Google/TCL launcher. TCL's
  firmware currently blocks selection of third-party HOME apps with a
  privileged launcher priority, so boot/wake entry needs a separate safe
  follow-up before it can be considered complete.
- Use one consented accessibility service to filter TCL's globally intercepted
  `KEYCODE_GUIDE` and enter the app when the service connects or the display
  wakes. TCL exposes the physical button to Android as private key code `4001`,
  so the service recognizes that captured code too. It subscribes to no
  accessibility events or window content, and all other keys pass through.
- Treat initial channel metadata as one atomic snapshot: HTSP control delivery
  applies backpressure instead of dropping messages, and the repository publishes
  channels only after `initialSyncCompleted`.
- Use AndroidX per-app locales for the operator UI. The empty locale list follows
  the device language, while explicit English, German, and Czech choices persist
  through the platform-compatible locale store.
- Preserve TVHeadend `channelNumber` metadata through the client model and use
  it for display and direct entry. Fall back to list positions only when the
  server supplies no channel numbers at all.
- Recover the existing Media3 session from player errors, unexpected stream end,
  and persistent initial buffering with bounded retry backoff. Recovery releases
  the failed HTSP subscription first and never changes the extractor or decoder.

## Phase 1: Reproducible private build identity

### Task 1: Create the Leoville build identity

**Acceptance criteria:**

- Package ID is `at.leoville.tvhstream` and label is `Leoville TV`.
- Firebase/Crashlytics and the Google services build requirement are absent.
- The GPLv3 license and upstream attribution remain intact.

**Verification:**

```bash
./gradlew testDebugUnitTest assembleDebug --no-daemon
aapt dump badging app/build/outputs/apk/debug/app-debug.apk
```

**Files:** Gradle configuration and string resources.  
**Dependencies:** None.

## Phase 2: Channel behavior

### Task 2: Persist the last played channel

**Acceptance criteria:**

- Starting or switching playback stores the channel ID in app-private
  Preferences DataStore.
- UI focus changes alone do not overwrite the last played channel.
- Missing/stale IDs fall back to the first current channel.

**Verification:** unit tests plus force-stop/relaunch on the TCL.

**Files:** a new store, Koin wiring, and player integration.  
**Dependencies:** Task 1.

### Task 3: Add physical channel-key switching

**Acceptance criteria:**

- `KEYCODE_CHANNEL_UP` selects the next channel and wraps at the end.
- `KEYCODE_CHANNEL_DOWN` selects the previous channel and wraps at the start.
- Other player keys preserve current behavior.

**Verification:** pure navigation unit tests and physical remote checks.

**Files:** navigation helper/test and `VideoPlayerScreen`.  
**Dependencies:** Task 2.

### Task 3a: Add direct channel-number entry

**Status:** Implemented; revised TVHeadend-number handling needs TCL revalidation.

**Acceptance criteria:**

- Top-row and numpad `0`-`9` key codes build a visible 1- to 3-digit overlay
  during playback.
- Entered numbers select the matching TVHeadend channel number; if no channels
  have server numbers, they select the matching 1-based ordered-list position.
- One- and two-digit entries tune after 1.5 seconds or immediately on OK; a
  complete three-digit entry remains visible briefly before tuning.
- Back cancels pending entry, and invalid or out-of-range numbers do not change
  the current channel.

**Verification:** pure navigation unit tests and physical remote checks with
1-, 2-, and 3-digit channel numbers.

**Files:** navigation helper/test and `VideoPlayerScreen`.
**Dependencies:** Task 3.

### Checkpoint: Channel behavior

- Full unit suite passes and APK builds.
- Progressive ORF1 and interlaced ServusTV still play.
- Physical channel buttons work repeatedly.
- Numeric entry visibly accepts and tunes 1-, 2-, and 3-digit channel numbers.
- Displayed and entered numbers match TVHeadend numbers, including numbering
  gaps.

## Phase 3: Appliance entry

### Task 4: Add one-shot autoplay launch requests

**Status:** Implemented and verified on the TCL on 2026-07-23.

The activity owns an identified pending request and creates a replacement only
for a new explicit launch. Compose waits for persisted state and non-empty
current channels, then consumes the matching request before navigating to the
player. Recomposition, resume, and player Back do not generate requests.

Runtime verification confirmed that force-stop plus launch restored ORF1 HD,
Back stopped playback without replay while the UI remained open, and a new
explicit launcher intent started ORF1 HD exactly once. ServusTV HD Oesterreich
also passed the direct human interlaced-motion regression check.

**Acceptance criteria:**

- Fresh process and explicit appliance intents wait for non-empty channels,
  then navigate to the persisted/first channel.
- A new request while settings/channel UI is visible starts playback.
- Back from player returns to UI and does not autoplay again.

**Verification:** launch-policy unit tests and ADB launch/Back/force-stop tests.

**Files:** launch-policy helper/test, `MainActivity`, and `AppRoot`.  
**Dependencies:** Task 2.

### Task 5: Register and validate the HOME role

**Status:** Candidate registration implemented; TCL selection blocked.

The packaged activity appears in Android's HOME candidate list. On the TCL,
both `cmd package set-home-activity` and affirmative selection in Android's Home
app screen store Leoville as preferred, but Google Basic TV still resolves and
opens. The system launcher has privileged priority `2`, while Android caps the
third-party Leoville filter to `0`. Google remains enabled and selected; no
HOME-role standby/wake or cold-reboot success is claimed. The separate
accessibility entry fallback is validated under Task 6.

**Acceptance criteria:**

- Android lists Leoville TV as a HOME candidate.
- ADB can select it as HOME without disabling Google Basic TV.
- HOME, standby/wake, and cold boot enter playback through the one-shot launch
  policy.

**Verification:** package resolver, HOME key, standby/wake, and approved cold
reboot tests.

**Files:** Android manifest only unless TCL runtime behavior requires a bounded
receiver fallback.  
**Dependencies:** Task 4.

## Phase 4: TCL TV key

### Task 6: Add the scoped GUIDE accessibility service

**Status:** Implemented and verified on the TCL on 2026-07-23.

TCL initially stored the user-approved service component but left global
accessibility off because Safety Guard rejected the app's hidden
`APP_AUTO_START` operation. Setting that app-op to `allow` for Leoville only and
repeating the Android consent toggle bound the service. The setting, app-op, and
live autoplay then survived three standby/wake cycles from Google Home and one
approved Android reboot while Google remained the default HOME.

The physical remote reports Linux `KEY_EPG` with scan code `0x0c005b`, but TCL's
Android callback exposes private key code `4001`, not standard `KEYCODE_GUIDE`
(`172`). The service recognizes both codes. Physical TV launched playback from
TCL UI and Leoville's operator UI, and pressing it during playback no longer
restarts the player. The final metadata requests key filtering but no
accessibility events or window-content access.

**Acceptance criteria:**

- Service declares key filtering without window-content access or accessibility
  event subscriptions.
- GUIDE down launches/reorders Leoville TV and GUIDE up is consumed.
- An entry intent while the player is already visible does not restart playback.
- Every key other than standard GUIDE and captured TCL code `4001` returns
  `false`.
- Service connection after boot and `SCREEN_ON` after standby each create a
  coalesced appliance launch through the existing one-shot policy.
- Enabling requires affirmative selection in Android accessibility settings and
  the in-app disclosure describes the exact scope.

**Verification:** key-policy unit tests, Android service inspection, and the
physical TV button from Google Home, channel UI, and playback; then standby/wake
and an approved cold reboot with enabled-service and app-op state rechecked.

**Files:** service class, policy/test, manifest, XML metadata, and strings.  
**Dependencies:** Task 4.

### Checkpoint: Appliance behavior

- TV, boot, and wake reach live TV; direct HOME remains blocked by TCL.
- CH+/CH- work in live playback.
- Back still reaches operator UI.
- Google Basic TV and both rollback clients still launch directly.

## Phase 5: Durable release and deployment

### Task 6a: Add German operator UI and language selection

**Status:** Implemented locally; device validation pending.

**Acceptance criteria:**

- All user-facing resource strings have context-appropriate German translations.
- Settings lists system default and every supported language and applies changes
  without restarting the process manually.
- The selected app language persists across relaunches.

**Verification:** JVM locale-policy test, full debug build, and remote-control
selection on the TCL.

**Files:** locale resources and metadata, a language settings screen, and
localized navigation labels.

**Dependencies:** None.

### Task 6b: Add resilient client-side TV operation

**Status:** Implemented locally; device fault validation pending.

This is a TVHeadend-integration and appliance-shell change only. It does not
modify TVHeadend accounts, channels, profiles, tuners, or other server state.

**Acceptance criteria:**

- Channel list, EPG, player overlay, and number entry use TVHeadend channel
  numbers, with list-position fallback only for an entirely unnumbered server.
- Appliance startup shows a persistent localized recovery screen until channels
  arrive; Back cancels the pending autoplay request and exposes the complete UI.
- Media3 error, unexpected end, or persistent initial buffering retries the same
  service with bounded 1/2/5/10/30-second backoff.
- A retry releases its failed HTSP subscription, successful playback resets the
  backoff, and explicit playback exit cancels pending recovery.
- Normal root Back does not call `exitProcess`; EPG and settings retain useful
  Back navigation, and HOME remains available for deliberate app exit.
- Settings is isolated at the bottom of the main rail and opens on Language;
  connection credentials require an explicit second navigation choice.
- An Options page can hide or show the main-menu EPG entry; it defaults to shown
  and does not disable EPG data used elsewhere.
- The playback controls do not initially focus Stop or another action, while all
  existing player controls remain available.

**Verification:** JVM policy tests, full debug build, startup with TVHeadend
temporarily unavailable, playback interruption/restoration, root/settings Back,
real sparse channel numbers, and progressive/interlaced playback on the TCL.

**Files:** channel model/navigation/UI, player session, connection status UI,
localized strings, and policy tests.

**Dependencies:** Tasks 3a, 4, and 6a.

### Task 7: Sign and install the release build

**Acceptance criteria:**

- Release APK uses a stable private Leoville key outside Git.
- Release package upgrades over itself and remains 32-bit compatible.
- APK SHA-256, signing fingerprint, source commit, and rollback commands are
  documented without secrets.

**Verification:** unit suite, release build, `apksigner verify`, install/upgrade,
and complete TCL runtime matrix.

**Files:** non-secret signing configuration support and deployment docs.  
**Dependencies:** Tasks 1-6b.

## Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| TCL resets HOME or accessibility after reboot | High | `APP_AUTO_START=allow` plus the user-enabled service survived three wake cycles and one reboot; retain rollback and recheck after firmware updates |
| TCL changes its private TV key mapping | High | Recognize standard GUIDE plus captured Android code `4001`; recapture after firmware or remote changes |
| Autoplay loops after Back | High | One-shot request counter with tests; never key autoplay directly to lifecycle resume |
| Persisted channel disappears | Medium | Validate against current channel IDs and fall back to first channel |
| Channel synchronization exposes partial lists | High | Lossless control delivery and atomic initial publication are implemented with JVM regressions; recheck a stable count across reconnects on the TCL |
| Media3 playback regresses | High | Do not alter extractor/rendering code; replay progressive and interlaced channels at each checkpoint |
| Temporary stream failure leaves a black screen | High | Retry the same service with bounded backoff, release failed subscriptions, show persistent recovery state, and cancel on explicit exit |
| Sparse TVHeadend numbers tune the wrong channel | High | Carry server channel numbers through the UI and unit-test gaps plus unnumbered-server fallback |
| Fork diverges from upstream | Medium | Keep appliance changes narrow and maintain the upstream remote |

## Final checkpoint

- All spec success criteria pass.
- Repo diff has been reviewed for secrets, GPL compliance, and surgical scope.
- Source is committed and pushed to the public fork.
- Canonical homelab docs record the deployed package and rollback path.
