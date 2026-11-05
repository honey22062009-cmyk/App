# Guardian protection and encrypted backup setup

## What is implemented

- The app inventory scanner reads installed package metadata and requested permissions locally. Permission declarations are capability signals, not proof that an app is harmful or that a permission is currently active.
- Guardian package controls use Android DevicePolicyManager to suspend selected packages and block their uninstall. These controls fail closed unless Android reports this app as the device owner.
- Google sign-in authenticates with Firebase Authentication. The account UID scopes each backup to that account.
- Cloud backups encrypt the snapshot on-device with AES-256-GCM. A recovery phrase derives the key using PBKDF2-HMAC-SHA256. The phrase and plaintext snapshot are not uploaded.
- Cloud restore includes app settings, managed-app selections, schedules, app groups, and the latest 100 generic protection events. Account credentials, local profile identity, device-local image URIs, one-time unlock state, and highly sensitive personal tracker records are deliberately excluded.

## Firebase project setup required

1. Confirm the Android app is registered in the Firebase project used by google-services.json.
2. Enable Google as a Firebase Authentication provider.
3. Create or enable Cloud Firestore in that project.
4. Deploy the repository's firestore.rules ruleset before releasing cloud backup. The rules permit an authenticated user to access only their own users/{uid}/backups/current encrypted record. All other paths remain denied.
5. Configure GOOGLE_WEB_CLIENT_ID as the existing GitHub Actions secret for builds that need Google sign-in.

The Firebase Console setup and rules deployment require the project owner's explicit action. A successful Android build alone does not mean the cloud service is enabled.

## Guardian-managed device setup

Device Owner is not an in-app permission switch. A guardian must provision the phone through Android's supported managed-device setup process, normally during initial device setup or after a factory reset. The guardian should retain control of the provisioning account and recovery process. The app reports ordinary-install status and does not claim that uninstall protection is active unless Android confirms Device Owner status.

Do not use this feature to secretly manage someone else's phone. The device owner and user should be informed which packages are restricted and what controls are active. Keep an explicit guardian recovery path before enforcing restrictions on essential apps.

## Google Play package-visibility review

The app declares QUERY_ALL_PACKAGES because installed-app inventory is part of its app-management and protection functionality. Google Play treats the installed-app list as sensitive data and may require a permissions declaration and approval. If the app is distributed through Play, complete the declaration accurately and remove broad visibility if the review does not approve it. Inventory data must not be sold or used for advertising or analytics monetization.

## Recovery phrase

The recovery phrase is not stored by the app or sent to Firebase. If it is lost, the encrypted backup cannot be decrypted by support or by the server. Keep it in a guardian-controlled password manager or another secure offline place. Use a long, unique phrase and never reuse an account password.

## Remaining validation

- Test Firebase Authentication and Firestore against the real configured project.
- Test backup and restore on a clean install using a test account and verify wrong phrases fail without modifying local data.
- Test Device Owner provisioning and package suspension on a dedicated test device, including packages Android refuses to suspend.
- Test uninstall blocking and the guardian's removal and recovery process before production rollout.
- Review QUERY_ALL_PACKAGES, Accessibility, Usage Access, VPN and other sensitive permission declarations against current Play policy.