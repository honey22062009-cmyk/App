# Strict Blocker 1.3.1 reliability and interface changes

The application ID remains `com.aistudio.strictblocker.kzvwpq`. Existing Room data, DataStore keys, Firebase rules, payment verification, schedule enforcement and paid features are retained.

## What changed

- Four primary destinations: Home, Block, Progress, Settings. Home keeps the protection status, current streak and recording action. Detailed graphs, calendar, milestones, history and Pro layout controls live in Progress.
- Personal setup writes the profile and completion in one DataStore edit. Complete legacy profiles recover a missing completion flag. Route resolution reads local state before any network work. Draft fields survive Activity recreation. Personal setup and entry sheets hide the dock; the dock is also hidden while the keyboard is open.
- A short recording sheet saves a rating and optional sleep together. Reset details are optional. Save failures leave the sheet open with an error; existing sleep notes are retained.
- Six native, lit 3D animal companions replace the line illustration. They use local OpenGL ES meshes, depth testing and lighting. Drag rotates, pinch zooms, tapping pets, and buttons trigger wave/jump/spin reactions. Thumbnail scenes render on demand; active scenes pause with the Activity lifecycle and respect Reduce motion.
- An evergreen and cream theme and vector shield identity replace the previous launcher image. Transitions, entry expansion, streak changes and companion poses provide motion.
- Verified owners receive the same Pro entitlement path used throughout the app, without creating a payment. Verification requires the current Firebase UID to have an `admins` document fetched from the server. It is rechecked after process restart and cleared on sign-out or auth-user change; an email or local boolean cannot grant access. Owner console sessions restore without typing the password on every visit.

## Browser and DNS fixes

The SafeSearch preference previously had no consumer in the enforcement path. It now controls DNS responses mapping Google search domains to `forcesafesearch.google.com` and Bing search domains to `strict.bing.com`, including country search hosts. A/AAAA answers preserve the original question and transaction, and HTTPS/SVCB shortcuts receive no-data responses for mapped hosts. Other engines are not advertised as forced SafeSearch.

The DNS TUN descriptor now uses blocking I/O. Previously the default nonblocking read could exit the worker before traffic arrived. The UI only marks the filter connected after the interface and protected upstream socket are established, handles revocation, reports live query counts and reads Android's actual always-on state. A DNS-only tunnel is not a full-traffic VPN; Android's “Block connections without VPN” setting is explicitly discouraged for this implementation.

Browser discovery includes all `com.opera.*` variants, Firefox/Chrome variants, and dynamically resolved HTTP(S) handlers. The manifest declares browser intent visibility. Noneditable toolbar URL fields are inspected, active private-mode controls and private-only browsers are checked, and asynchronous inspection owns its AccessibilityEvent copy. Inspection stays within the event browser package, skips passwords and excludes unrelated editable fields.

The protection page offers two distinct checks:

1. DNS-only probes for a clean domain, a restricted-domain NXDOMAIN response and a Google SafeSearch CNAME, with confirmation that the app's tunnel processed queries.
2. A safe `example.com` browser URL carrying a restricted test query. A working accessibility guard should interrupt the browser. Opening a blocker Activity directly is no longer treated as an enforcement test.

This does not guarantee every page in every browser. Browser VPN/proxy features, encrypted DNS, concealed UI, cached content, permission revocation and Android/OEM background policies can reduce coverage. The page shows these limits; it does not label a saved switch as proof of enforcement. Device Owner / network management would be a separate deployment, not a claim made by this build.

## Reminder fixes

The scheduling and receiver path uses one-shot exact alarms when allowed, serialized rescheduling, rescheduling after firing and time/permission changes, and `goAsync()` to finish receiver work before Android releases it. Disabled stale alarms do not post notifications. UI controls request notification permission, inspect app/channel blocks, show the next date and time, link to exact-alarm access and schedule a real one-minute test. The test uses the same AlarmManager/receiver/channel path as daily reminders.

A selected time two minutes ahead schedules for today. A time already passed schedules for tomorrow. Without exact-alarm access delivery is explicitly described as approximate. Force-stop and Samsung sleeping-app policies still need device-side configuration.

## Verification

The workflow runs Android lint, builds the APK, executes JVM/Robolectric tests, and runs the six-step setup and recording UI regressions on an Android 15 tablet emulator. The tablet job captures setup, companion, Home, recording, Progress and Settings screenshots as `tablet-ui-evidence`.

Regression tests cover profile reopening/sign-out, completion recovery, atomic daily recording with sleep-note preservation, the 22:15 → 22:17 reminder case, actual alarm registration/cancellation, malformed DNS packets, A/AAAA/CNAME response construction, lookalike domain exclusions, Opera variants and private-mode signals.

Physical Samsung delivery, each browser's current UI, built-in VPN/secure DNS modes, owner credentials and UPI transfers require real-device/account verification. A successful build or screenshot alone is not evidence for those paths.

## Primary implementation references

- Android alarms: https://developer.android.com/develop/background-work/services/alarms
- Android VPN routing and always-on: https://developer.android.com/develop/connectivity/vpn
- Google SafeSearch DNS: https://support.google.com/websearch/answer/186669
- Bing SafeSearch DNS: https://support.microsoft.com/en-us/bing/blocking-explicit-content-with-safesearch
