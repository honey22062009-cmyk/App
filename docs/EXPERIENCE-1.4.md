# Strict Blocker 1.4.0

Application ID remains `com.aistudio.strictblocker.kzvwpq`; versionCode is 5.

## Experience changes

- Rose is the default accent and Light is the default mode where a mode was never selected. Lilac/Peach and Light/Dark/System remain explicit settings. All Material surface roles now use rose/plum neutrals; the launcher and splash identity match.
- The dock is genuinely translucent with layered rim highlights and an animated selection pill. It is an original Android treatment, not a claim of Apple's rendering implementation. Dock destinations now include Home, Block, Progress, Pro and Settings. Dock navigation slides between screens; Reduce Motion removes the slide. Dock stays hidden during setup and entry sheets.
- Home is bounded to a readable tablet width and adapts companion/streak content vertically on phones. Daily entry has a seven-day strip, a persistent saved receipt, rating/sleep details and a recorded-day count. Saving twice updates one calendar-day record. This is separate from the existing elapsed-time streak, whose first day correctly starts at zero completed 24-hour periods.
- Recording waits for the DataStore transaction. The sheet stays open on a write error, ignores duplicate save taps and preserves cancellation. The current date is obtained at save time, so a sheet left open past midnight writes to the new day.
- First Home visit offers three short, skippable tips. Finishing or skipping persists; Settings → About replays them. No extra form fields or forced permission grant.
- Pro has a primary dock destination, feature explanations and direct links to the unlocked tools. Verified owners retain free Pro. Existing UPI review and paid-entitlement logic remain; a payment submission is not treated as an approved purchase. Removed the inaccurate “best value” claim from the yearly price, which costs more than twelve monthly plans at current prices.

## Companion

Luna uses a continuous smooth body mesh from torso through neck/head, connected shoulder pivots, ivory fur, blush ears, pink paw pads and an embedded heart charm. Ears, arms, body and mouth react to pet, wave, jump, spin, dance, feeding, breathing and talk actions. Other animals retain their IDs and gain the same controls. Drag/pinch remain available. GPU uniforms are cached; visible animation is capped near 30fps and respects pause/reduced-motion settings.

Four original bundled PCM effects work without downloads. Sound defaults to off and is persisted. “Say something” uses an installed, non-network Android TTS voice, with captions and a clear fallback if none is installed. No microphone permission, cloud voice API or network-required voice is used. Audio stops on pause, mute and disposal.

## Browser Guard replaces the VPN

The VPN service, manifest declaration, systemExempted foreground permission, DNS wire code, probes and startup hooks are removed. Existing stored DNS flags are inert. The app makes no VPN connection and cannot display a VPN connection indicator.

The Accessibility service checks only visible browser address/search fields in a matching foreground browser window. It skips passwords, hidden nodes, other packages, non-search editable fields, arbitrary page text and private-mode labels. It discovers installed web handlers and Opera variants. A restricted domain or explicit search token triggers HOME and the reason screen. Opening a browser/private tab is not a violation. No package-wide adult cooldown reasserts over later clean browsing; explicit app schedules still apply independently.

The feature requires separate in-app disclosure and affirmative consent before browser text inspection. Browser URLs/searches are not saved or uploaded; only generic block events/counts are retained locally. The page shows configured versus connected state, field inspection counts and a safe offline phrase check. Google/Bing buttons request filter parameters; they do not claim system-wide SafeSearch locking. Hidden browser chrome, private-window restrictions and inaccessible UI still limit coverage; the app is not an image/video classifier.

## Validation and distribution

The automated suites cover daily entry save/edit/recreation, setup persistence, Pro discoverability, theme persistence, quick-tour persistence, absence of a VPN manifest service, visible native 3D rendering and a scheduled background notification. Native browser fixtures exercise the real Accessibility service: normal/private-labelled pages stay open, an explicit address search is interrupted and clean browsing resumes. JVM/Robolectric tests cover browser-field scope and boundaries, the continuous bunny mesh, saved data and reminder alarms on Android 8/15. CI runs Android 15 tablet and Android 16 phone instrumentation and produces APK, reports, screenshots and an unsigned release AAB.

The AAB is not a published or Play-approved app. Public distribution still needs:

1. A retained release/upload signing key and Play App Signing. Historical CI debug APKs use different ephemeral keys; without the original key they cannot be updated in-place. Do not uninstall an existing app with needed data merely to work around a certificate mismatch.
2. A Play-compliant billing configuration. The existing sideload build's manual UPI flow is not automatically approved for Play. Digital feature purchases normally require Play Billing, or enrollment in a qualifying alternative-billing program. The dormant billing helper alone does not provide production backend purchase verification.
3. Play Console Accessibility/foreground-service declarations and disclosure demonstration, accurate Data Safety/privacy/refund/support information, content rating and current release-policy review.
4. Physical Samsung and low-memory device testing, verified owner-account/real-payment testing, an internal Play test and pre-launch reports. Emulator passes cannot establish reliability for millions of devices.

Primary policy references checked 2026-10-28:
- https://support.google.com/googleplay/android-developer/answer/10964491
- https://support.google.com/googleplay/android-developer/answer/9858738
- https://developer.android.com/reference/android/speech/tts/TextToSpeech
