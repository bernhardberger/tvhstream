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
- Use the HOME role for boot/wake entry. Do not add a permanent foreground
  service or disable the Google/TCL launcher.
- Use an accessibility service only for TCL's globally intercepted
  `KEYCODE_GUIDE`; all other keys pass through unchanged.

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

### Checkpoint: Channel behavior

- Full unit suite passes and APK builds.
- Progressive ORF1 and interlaced ServusTV still play.
- Physical channel buttons work repeatedly.

## Phase 3: Appliance entry

### Task 4: Add one-shot autoplay launch requests

**Acceptance criteria:**

- Fresh process and explicit appliance intents wait for non-empty channels,
  then navigate to the persisted/first channel.
- A new request while settings/channel UI is visible starts playback.
- Back from player returns to UI and does not autoplay again.

**Verification:** launch-policy unit tests and ADB launch/Back/force-stop tests.

**Files:** launch-policy helper/test, `MainActivity`, and `AppRoot`.  
**Dependencies:** Task 2.

### Task 5: Register and validate the HOME role

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

**Acceptance criteria:**

- Service declares key filtering without window-content access.
- GUIDE down launches/reorders Leoville TV and GUIDE up is consumed.
- Every non-GUIDE key returns `false`.

**Verification:** key-policy unit tests, Android service inspection, and the
physical TV button from Google Home, channel UI, and playback.

**Files:** service class, policy/test, manifest, XML metadata, and strings.  
**Dependencies:** Task 4.

### Checkpoint: Appliance behavior

- HOME and TV key both reach live TV.
- CH+/CH- work in live playback.
- Back still reaches operator UI.
- Google Basic TV and both rollback clients still launch directly.

## Phase 5: Durable release and deployment

### Task 7: Sign and install the release build

**Acceptance criteria:**

- Release APK uses a stable private Leoville key outside Git.
- Release package upgrades over itself and remains 32-bit compatible.
- APK SHA-256, signing fingerprint, source commit, and rollback commands are
  documented without secrets.

**Verification:** unit suite, release build, `apksigner verify`, install/upgrade,
and complete TCL runtime matrix.

**Files:** non-secret signing configuration support and deployment docs.  
**Dependencies:** Tasks 1-6.

## Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| TCL resets HOME or accessibility after reboot | High | Keep Google/Headent rollback; test a true cold reboot before declaring success |
| Accessibility service receives GUIDE too late | High | Validate before package changes; dreamLauncher proved the same API path on this model |
| Autoplay loops after Back | High | One-shot request counter with tests; never key autoplay directly to lifecycle resume |
| Persisted channel disappears | Medium | Validate against current channel IDs and fall back to first channel |
| Media3 playback regresses | High | Do not alter extractor/rendering code; replay progressive and interlaced channels at each checkpoint |
| Fork diverges from upstream | Medium | Keep appliance changes narrow and maintain the upstream remote |

## Final checkpoint

- All spec success criteria pass.
- Repo diff has been reviewed for secrets, GPL compliance, and surgical scope.
- Source is committed and pushed to the public fork.
- Canonical homelab docs record the deployed package and rollback path.
