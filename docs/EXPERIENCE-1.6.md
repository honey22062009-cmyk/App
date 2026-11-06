# Companion experience 1.6.0

Version name: 1.6.0  
Version code: 7  
Application ID: `com.aistudio.strictblocker.kzvwpq` (unchanged)

## Companion models and behaviour

- The rabbit retains its continuous seated body mesh, layered facial details, tapered ears and fur-oriented lighting.
- The other companions now use distinct proportions and silhouette cues for the bear, cat, panda, owl and fox rather than relying on one identical body layout.
- Arms use broader curved shoulder-to-wrist meshes with overlapping shoulder sockets, formed paws and small toe/paw details. Panda limbs and facial patches have their own contrasting palette.
- The different companions have individual tail shapes, ear proportions and secondary details.
- The automatic idle loop now selects from a larger set of small reactions, including a paw wave, happy hop, wiggle, looking up, listening, ear twitch, stretch, glance and tail movement. These play without the user needing to press an action button.
- Reduced-motion mode continues to suppress continuous autonomous movement. Voice remains optional and user-controlled.

This remains native procedural OpenGL ES geometry, not a downloaded rigged character asset. Visual quality will still vary with the device GPU and screen.

## Dedicated focus room

- The small timer has been removed from the home companion panel. The home shortcut opens a separate full-screen focus room and hides the dock while the session is open.
- Suggested durations: 15, 25, 45 and 60 minutes. Custom duration: 10–120 minutes in five-minute increments.
- Session type can be marked as Study, Revision, Practice or Reading, with an optional task label.
- The focus room includes the animated selected companion, changing companion cues, animated progress ring, remaining-time display, pause/resume and explicit end-session flow.
- The session pauses when the app leaves the foreground. It is an in-app foreground timer, not a background service and not a notification-based timer.
- Completed sessions and sessions ended early are both saved locally. Session records retain start time, planned duration, actual duration, activity/task label and completion status.

## Progress history

- Progress contains a dedicated **Focus session history** card with total sessions, completions, minutes and this-week count.
- Opening the card shows a separate history view with filters for all, completed and ended-early sessions.
- Focus sessions use their own DataStore key and record model. They do not enter the streak graph, reset timeline or streak calculations.
- The local history retains up to 1,000 recent sessions and remains on-device across app updates.

## Android update identity and signing

- The application ID remains `com.aistudio.strictblocker.kzvwpq`; version code increases to 7. This preserves the package identity.
- Android can replace an installed APK in place and preserve app data only when the new APK has the same application ID, a higher version code and a compatible signing certificate.
- The CI workflow can build a release APK signed with a retained key when these GitHub Actions secrets are configured: `ANDROID_KEYSTORE_BASE64`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_PASSWORD`, and optionally `ANDROID_KEY_ALIAS` (defaults to `upload`). The key must be the same key that signed the already-installed APK if that existing installation must update in place.
- Without a retained signing key, CI continues to make a debug APK using the runner's debug key. A newly generated runner key is not a safe update path for an APK signed by a different certificate.
- Android does not permit changing an installed app to a different signing certificate as an ordinary update. The previous certificate's private key cannot be recovered from the APK. Therefore, a build is not claimed to be a seamless update unless the signature matches.

## Validation

The repository CI runs Android lint, debug build, JVM/Robolectric unit tests, phone/tablet emulator UI regressions and the release bundle build. The focus workflow also has a UI regression for saving a session and opening its separate Progress history. Emulator checks do not replace real-device checks for GPU rendering, voice packs, timing and touch response.
