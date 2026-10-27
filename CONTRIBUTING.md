# Contributing to Quick

Thank you for your interest in Quick. This is a deliberately small, offline-first Android inbox.

## The product principles

- Keep **Capture → Waiting → Done** obvious and fast.
- Keep local data and user control at the centre.
- Fix bugs and improve accessibility, reliability, battery use and Android compatibility before adding new features.
- Do not quietly introduce accounts, ads, trackers, server-side AI, subscriptions, projects, priorities or due-date systems.
- Preserve the seven-day Done-retention contract unless an explicit design change is agreed.
- Never break an existing installation through an unnoticed change to package identity or release signing.

## Proposing changes

For a bug, open an issue with clear reproduction instructions using **fictional data**, Android version and app version. For a feature, describe the real user problem and why a small solution is preferable.

Before a pull request: run `gradle :app:testDebugUnitTest`, and if you have an Android SDK, compile `gradle :app:assembleDebug`. UI, attachments, reminders, accessibility and data upgrades require physical-device checks. Maintainers may ask for changes or decline additional features that dilute the app's purpose.

Please do **not** include personal notes, exports, signing keystores, passwords or other people's artwork in pull requests or issue attachments.

## Releases

Contributions do not automatically trigger release builds. Release signing and APK publication are separate, owner-controlled acceptance steps. See [COMMUNITY_RELEASE.md](COMMUNITY_RELEASE.md).

Code contributions to the repository are submitted under the project [MIT licence](LICENSE). You must have the rights to submit any code or assets you provide.
