# Quick Community 1.0.0 — DRAFT release notes

**NOT YET PUBLISHED — FOR OWNER REVIEW**

Quick Community is a small, offline-first Android capture inbox.

**Capture now. Decide later.**

## Features
- Fast text capture with optional categories
- Voice-to-text using Android's speech recognition
- Photo and document attachments (one per capture)
- Share text, websites, photos and PDFs into Quick from other apps
- Waiting list with swipe-to-Done and Undo
- Review, full-text search and filters
- Optional daily Waiting reminder
- Android 2×2 home-screen widget and launcher shortcuts
- Manual local backup and restore
- Light and dark themes

Completed notes are automatically removed when Quick next refreshes its state after the seven-day retention period. Backups are **not encrypted by Quick** and must be stored securely.

## Independent install

Package: `uk.co.milncj.quick.community`.

This is a separate app from the original personal Quick (`uk.co.milncj.quick`), so it has its own local data. For data migration, export a backup from the original and restore into Quick Community only after validating backup compatibility.

## Download and verify — add only after release acceptance

The public GitHub Release should contain:
- `quick-community-1.0.0.apk` (signed with the owner's **private** release key)
- `quick-community-1.0.0.sha256` (matching SHA-256 checksum)

Do not offer test-key or unsigned APKs as public releases.

## Final release approval

- [x] Source build and available unit tests green ([GitHub run](https://github.com/milncj1/Quick-Tool/actions/runs/40570908546))
- [ ] Signed APK certificate reviewed
- [ ] Owner installed alongside original Quick without losing data
- [ ] Capture / voice / photo / share / widget / notifications / backup tested on real phone
- [ ] Own-use icon and assets permissions confirmed
- [ ] APK and checksum uploaded to GitHub Releases, with these notes edited from DRAFT

## Support

File bug reports on GitHub Issues using fictional example notes, not private user data. Contributions are welcomed under MIT.
