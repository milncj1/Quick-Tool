# Quick security policy

Thank you for helping keep Quick safe.

## Supported versions

Security fixes are prioritised for the latest maintained Quick Community release once public APK distribution starts. The original personal Quick build is not a public-release target.

## Report a vulnerability privately

Please **do not** include exploitable details, private notes, real backups or credentials in a public GitHub Issue. If GitHub's private vulnerability reporting option is enabled for this repository, use **Security → Report a vulnerability**. Otherwise, contact the repository owner privately via the GitHub profile to arrange a secure channel.

Helpful reports include the Android version, Quick version, reproduction steps with fictional data and the expected/actual outcome.

## Handling sensitive data

Quick stores notes and attachments locally. Its manually exported ZIP backups are **not encrypted by Quick** and may contain readable capture text, image files and documents. Treat them as sensitive. Android speech recognition is provided by the installed device service; that service's processing and network behaviour are outside Quick's control.

Signing credentials must never be put in source, issue discussions, APK metadata or public Action logs. Public Community releases use their own private signing key and a separate application ID.

For source-code security questions, please also see [PRIVACY.md](PRIVACY.md) and [COMMUNITY_RELEASE.md](COMMUNITY_RELEASE.md).
