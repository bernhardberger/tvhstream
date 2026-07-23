# Spec: Leoville TV appliance mode

## Objective

Turn the GPLv3 TVHStream Android TV client into a single-purpose live-TV
appliance for a household user who should not need to navigate Google TV.

The app must keep TVHeadend's existing `mberger` account local to the device,
play the last selected channel after an appliance launch, support the physical
channel buttons, and reclaim TCL's globally intercepted TV/GUIDE button through
a narrowly scoped accessibility service.

The upstream-shaped channel list, EPG, and settings remain available as an
operator path. The app must not immediately restart playback after the user
backs out to those screens.

## Tech stack

- Android API 28 minimum / API 36 target
- Kotlin 2.3.10 and Jetpack Compose
- AndroidX Media3 / ExoPlayer 1.9.2
- TVHeadend HTSP through the existing custom extractor
- Preferences DataStore for non-secret last-channel state
- Android `AccessibilityService` only for the intercepted GUIDE key
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
- `app/src/main/java/.../accessibility/` — GUIDE-key interception only
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

- Unit-test channel navigation, wrapping, and launch-policy decisions.
- Run the complete existing JVM unit-test suite and build the APK.
- Install beside both stock Headent and the temporary upstream-package
  TVHStream diagnostic build.
- Runtime-test progressive and interlaced playback, `CH+`, `CH-`, persistence
  across force-stop, normal Back navigation, HOME launch, GUIDE/TV key,
  standby/wake, and a cold reboot.
- Treat visible motion quality on an interlaced sports broadcast as a mandatory
  human verification gate.

## Boundaries

### Always

- Keep credentials in app-private storage; never hardcode or commit them.
- Preserve a route to channel list, EPG, and settings through Back navigation.
- Keep Google Basic TV and stock Headent installed until all runtime checks pass.
- Use a distinct Leoville application ID and stable signing key.
- Consume only the known TCL GUIDE key in the accessibility service.

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
4. The last successfully selected channel survives process death and reboot.
5. A fresh app, HOME, boot, wake, or GUIDE-appliance launch waits for connection
   and channel data, then plays the persisted channel or the first channel.
6. Back exits playback to the normal TVHStream UI without an autoplay loop.
7. The accessibility service ignores every key except `KEYCODE_GUIDE` and does
   not interfere with keys while disabled.
8. Google Basic TV, Headent, and the diagnostic TVHStream package remain
   available as rollback paths during validation.
9. Unit tests pass, the release APK is signed with the stable private key, and
   installed package/signature/version details are recorded without secrets.

## Open questions

- Whether TCL retains the selected HOME app and enabled accessibility service
  across a true cold reboot. This must be answered by runtime testing, not by
  assumption.
- Whether the GUIDE event reaches the custom accessibility service before TCL's
  native TV handler. dreamLauncher previously proved the mechanism on this TV,
  but the new service still requires direct validation.
