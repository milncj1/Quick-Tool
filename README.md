# Quick

**Capture now. Decide later.**

Quick is a lightweight personal capture inbox for Android. Save something the moment it occurs to you, deal with it when you have time, and keep the interface deliberately simple.

## How it works

**Capture → Waiting → Done → deleted after seven days (on the next app refresh)**

Capture text or something shared from another Android app, return to your Waiting list later, and mark it Done when it no longer needs your attention. Completed captures remain available in Review for seven days and are permanently purged when the app state next refreshes (including when you reopen the app).

## Features

- **Fast capture:** typed text, voice-to-text using Android's speech-recognition facilities, photographs, and incoming Android shares.
- **Links and files:** shared webpages, images, PDFs and other supported files; one attachment per capture.
- **Waiting inbox:** mark Done with a swipe, undo an accidental completion, and edit, share or delete captures.
- **Review and search:** search Waiting and recent Done items; filter by status and optional custom category.
- **Categories:** create, rename, reorder and remove categories without deleting the captures assigned to them.
- **Light-touch reminders:** optional daily reminder when Waiting is not empty, with configurable time and older-item notice.
- **Fast access:** Android home-screen widget and launcher shortcuts.
- **Local backup and restore:** manually export and restore your captures and supported attachments.
- **Light and dark appearance:** designed for quick use on a phone.

No projects, due dates, task hierarchies, AI categorisation or cloud account are required.

## Data and privacy

Quick does not require an account or provide cloud synchronisation. Its capture database and attached files are managed locally by the Android app. Android's automatic app backup is disabled in the application manifest. Backups you manually export are **unencrypted ZIP archives** containing readable notes and attachments; store them securely. See [privacy details](PRIVACY.md).

Voice-to-text uses the Android speech-recognition facilities; whether voice processing occurs on-device depends on your device and speech-recognition provider. This should not be mistaken for a guarantee that voice processing is offline.

**Important:** Done captures are eligible for permanent removal after seven days, and the purge runs when Quick refreshes. Export a backup or copy anything you need to keep.

## Build from source

For developers, the project uses Kotlin, Jetpack Compose and Gradle, targeting Android.

- JDK 17
- Gradle 9.6
- Android SDK Platform 36 and Build Tools 36.0.0

With those tools installed, run:

```sh
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

For source development, install a locally compiled **debug** APK. The public distribution variant is `gradle :app:assembleCommunity`, producing an unsigned package with a separate Android application ID. See [Community build and signing instructions](COMMUNITY_RELEASE.md). Debug builds are not trusted public product releases.

## Public downloads

**No approved public Quick Community APK has been published yet.** A community build variant and owner-only manual signing workflow have been prepared, but a private signing key, tested candidate and owner acceptance are still required before we offer APK downloads.

The original personal development build is not the public product-signing mechanism; this public repository contains only the separate owner-signed Community release workflow.

Public and original installations have different application IDs, so the two apps can coexist without changing an existing Quick installation. The separate app will have its own local data; a tested manual backup/restore is required for any migration.

See the [Community release guide](COMMUNITY_RELEASE.md) and [release checklist](RELEASE_CHECKLIST.md).

## Project status and feedback

The project is based on the tested Quick 1.0 feature baseline; public-distribution readiness remains a separate acceptance step.

When this project is public, please use GitHub Issues for actionable bug reports or suggested improvements. Avoid sharing real captures, private backup archives or identifying information in issues.

## Licensing

Quick source is released under the [MIT License](LICENSE). Third-party Android/Kotlin/Jetpack dependencies retain their own licences. Do not reuse photographs, captures or proprietary assets that you do not have permission to redistribute.

Contributions are welcome under the [contribution guidelines](CONTRIBUTING.md). Please see the [security policy](SECURITY.md) for private vulnerability reporting.
