# Quick — privacy notes

Quick is designed as a local capture inbox rather than an online account service.

## What Quick handles

- Captured text, category, created/completed timestamps and state are stored in an app-local SQLite database.
- Shared attachments, photos and documents are copied into app-managed local files when saved.
- Reminder and category settings are held on-device.
- The app contains no developer account service, cloud sync or built-in analytics/advertising integration in the reviewed Quick 1.0 code.
- Android automatic backup is disabled in the app manifest; manually exported backups remain the user's responsibility.

## Voice capture

Voice input starts the Android speech-recognition mechanism supplied by the user's device or provider. Some providers may process voice information online. Quick does not guarantee offline recognition; users should check their device's speech service settings.

## Backups

The export is a **ZIP archive containing readable JSON and attachments; it is not encrypted by Quick.** Avoid placing backups on shared/public drives unless they are protected separately. Restoring a backup is a destructive data-management operation and should be tested with safe sample information first.

## Retention and deletion

A capture marked Done is eligible for permanent deletion after seven days. Quick checks and purges expired Done captures **when its app state is refreshed**, including app opening; the precise deletion time is not guaranteed while Quick is closed. Waiting items remain until marked Done or manually deleted. Purging removes the associated attachment stored by Quick.

Android and other apps/services may maintain their own caches, copies, cloud history or shared-file versions. Quick cannot purge copies outside its own managed storage.

## Permissions and services

Quick requests notification permission when reminders are enabled on supported Android versions. Camera, shared file and speech actions call the user's Android apps/providers rather than requiring a Quick-hosted account. These external services maintain their own privacy practices.

Last reviewed against the existing Quick 1.0 source: 27 October 2026.
