# Strict Blocker 1.5.0

Version code: 6. Application ID remains `com.aistudio.strictblocker.kzvwpq`.

## Companion and character

- Luna's seated body is a single lofted mesh with a narrower shoulder/neck transition and rounded haunch silhouette. Tapered ear meshes replace the old long oval ear pieces; the inner-ear mesh is inset into the outer ear.
- Rabbit details include layered eyes and catchlights, a split muzzle, nose and mouth, fine whiskers, cheek colour, forefeet and toe pads, and an animated cotton tail.
- A softer shader adds restrained strand-like surface variation, broad mottling, diffuse wrap light and less glossy highlights. Articulated arms now use a tapered curved limb mesh instead of two obvious ellipsoid joints.
- Idle movement includes subtle looking, breathing, blinking, ear motion and tail motion. New reactions include cheerful hops, listening and thinking. Reduced-motion settings continue to suppress continuous animation.
- The other selected companions retain their established species models. The rabbit-specific mesh is used for Luna.

This is procedural real-time 3D geometry, not a downloaded photorealistic character asset. Rendering quality remains dependent on the Android GPU and display.

## Voice and dialogue

- Dialogue has been expanded across greeting, play, care, focus, tips, check-ins, goals, progress, celebrations and reminders.
- Voice selection prefers a fully installed offline English TTS voice, ranking Indian English first when available, then other English voices, while retaining fallback to the device language or another installed offline voice.
- The speech profile is slightly warmer and slower. Voice status reports the selected locale. Existing sound-effects and sound-off defaults remain unchanged.
- No cloud TTS service, microphone permission, voice recording or network voice is added. The actual voice timbre and available locale depend on the TTS engine and voice packs installed on the user's Android device.

## More useful companion panel

- The companion displays a context-aware nudge based on the saved check-in, current streak, personal best and saved goal.
- Quick actions open the real daily-record sheet and goal editor instead of presenting decorative controls.
- A 25-minute focus timer can be started, paused, resumed and reset in the companion panel. It is an in-app timer only and pauses when the activity leaves the foreground; it does not claim to run as a background service or send a notification.
- Automatic idle cues remain silent and do not interrupt the user with speech. Speech stays user-initiated and is optional.

## Identity and product UI

- Splash and adaptive launcher marks are updated to a bunny-shield identity in the app's rose/plum and soft pink palette.
- Existing translucent dock, warm pastel theme and responsive phone/tablet layouts are preserved.
- App version is 1.5.0 (versionCode 6).

## Validation

Automated validation is run by the repository's Android CI workflow for this branch:
- Android lint
- Debug APK build
- JVM/Robolectric unit tests, including companion behaviour and 3D geometry checks
- Release AAB build
- Phone and tablet emulator UI regression jobs

A successful CI run is not a substitute for testing the bunny's appearance, installed TTS voice, sound levels and touch response on the user's actual Samsung device.