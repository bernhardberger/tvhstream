# Spec: Leoville TV appliance mode

## Objective

Turn the GPLv3 TVHStream Android TV client into a single-purpose live-TV
appliance for a household user who should not need to navigate Google TV.

The app must keep TVHeadend's existing `mberger` account local to the device,
play the last selected channel after an appliance launch, support physical
channel and number buttons, reclaim TCL's globally intercepted TV/GUIDE button,
and enter live TV after boot or display wake through a narrowly scoped
accessibility service.

The upstream-shaped channel list, EPG, and settings remain available as an
operator path. The app must not immediately restart playback after the user
backs out to those screens.

## Tech stack

- Android API 28 minimum / API 36 target
- Kotlin 2.3.10 and Jetpack Compose
- AndroidX Media3 / ExoPlayer 1.9.2
- TVHeadend HTSP through the existing custom extractor
- Preferences DataStore for non-secret last-channel state
- Android `AccessibilityService` for GUIDE filtering plus boot/wake appliance
  entry, without subscribing to accessibility events or window content
- GPL-3.0; the public fork retains upstream copyright and license material

## Commands

```bash
./gradlew testDebugUnitTest assembleDebug --no-daemon
adb -s 192.168.8.183:5555 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 192.168.8.183:5555 shell monkey \
  -p at.leoville.tvhstream \
  -c android.intent.category.LEANBACK_LAUNCHER 1
```

Final release builds must use the private Leoville signing key and must not rely
on the Android debug keystore.

## Project structure

- `app/src/main/java/.../player/` — Media3 playback and channel switching
- `app/src/main/java/.../stores/` — persisted last-channel selection
- `app/src/main/java/.../ui/` — launch policy and normal TV UI
- `app/src/main/java/.../accessibility/` — GUIDE filtering and boot/wake entry
- `app/src/main/res/xml/` — accessibility-service declaration
- `app/src/test/` — pure launch/navigation policy tests
- `docs/` — appliance behavior, build, and rollback documentation

## Code style

Match the existing Kotlin/Compose style. Keep appliance decisions explicit and
testable rather than scattering key codes or intent checks through composables.

```kotlin
fun adjacentChannelId(
    orderedIds: List<Int>,
    currentId: Int,
    direction: Int,
): Int? = when {
    orderedIds.isEmpty() -> null
    else -> orderedIds[(orderedIds.indexOf(currentId).coerceAtLeast(0) +
        direction).floorMod(orderedIds.size)]
}
```

## Testing strategy

- Unit-test channel navigation, wrapping, number entry, and launch-policy
  decisions.
- Run the complete existing JVM unit-test suite and build the APK.
- Install beside both stock Headent and the temporary upstream-package
  TVHStream diagnostic build.
- Runtime-test progressive and interlaced playback, `CH+`, `CH-`, 1- to 3-digit
  channel entry, persistence across force-stop, normal Back navigation, HOME
  launch, GUIDE/TV key, standby/wake, and a cold reboot.
- Treat visible motion quality on an interlaced sports broadcast as a mandatory
  human verification gate.

## Boundaries

### Always

- Keep credentials in app-private storage; never hardcode or commit them.
- Preserve a route to channel list, EPG, and settings through Back navigation.
- Keep Google Basic TV and stock Headent installed until all runtime checks pass.
- Use a distinct Leoville application ID and stable signing key.
- Consume only Android GUIDE and the captured TCL TV key code in the
  accessibility service; boot/wake entry must not subscribe to accessibility
  events or inspect window content.

### Ask first

- Disabling any TCL or Google package.
- Removing either rollback client.
- Adding server-side transcoding or changing TVHeadend stream profiles.
- Publishing a signed release or upstream pull request.

### Never

- Embed TVHeadend credentials, signing material, or Firebase secrets in Git.
- Turn the accessibility service into general UI automation or key logging.
- Autoplay again merely because the player was closed with Back.
- Make server, tuner, OSCam, storage, or recording changes for this client work.

## Success criteria

1. The Leoville package installs and runs on the TCL's 32-bit `armeabi-v7a`
   Android TV 12 environment.
2. Interlaced-channel playback remains at least as good as the accepted
   TVHStream diagnostic result.
3. Physical `CH+` and `CH-` switch to adjacent visible channels and wrap at the
   ends of the list.
4. Physical `0`-`9` keys show a channel-number overlay and select the matching
   visible channel after 1 to 3 digits.
5. The last successfully selected channel survives process death and reboot.
6. A fresh app, HOME, boot, wake, or GUIDE-appliance launch waits for connection
   and channel data, then plays the persisted channel or the first channel. If
   playback is already visible, the entry intent must not restart it.
7. Back exits playback to the normal TVHStream UI without an autoplay loop.
8. The accessibility service ignores every key except Android GUIDE and the
   captured TCL TV key code, does not subscribe to accessibility events or
   window content, and does not interfere with keys while disabled.
9. Google Basic TV, Headent, and the diagnostic TVHStream package remain
   available as rollback paths during validation.
10. Unit tests pass, the release APK is signed with the stable private key, and
   installed package/signature/version details are recorded without secrets.

## Open questions

- How to make Leoville the TCL's HOME app without disabling Google Basic TV.
  The firmware gives its system launcher priority `2`, caps third-party HOME
  candidates to priority `0`, and ignores both shell and user role selection.
  Google must remain enabled until a safe reversible path is proven.

## TCL deployment findings

- TCL Safety Guard must allow the appliance package's hidden `APP_AUTO_START`
  app-op before the user-approved accessibility service will bind. The enabled
  service and app-op survived three standby/wake cycles and one Android reboot.
- The physical TV button emits Linux `KEY_EPG`, but this firmware delivers TCL
  private Android key code `4001` to accessibility services. The service also
  accepts standard Android `KEYCODE_GUIDE` for non-TCL input paths.
- Initial channel metadata is delivered without lossy buffering and staged until
  `initialSyncCompleted`, so the UI receives one complete channel snapshot rather
  than unstable partial lists (previously observed around 30, 84, and 50 channels).
  Recheck the stable count on the TCL before final appliance deployment.
