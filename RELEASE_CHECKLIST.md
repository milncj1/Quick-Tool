# Quick Tool — public release checklist

The source code is publicly available under MIT. **There is no endorsed installable APK yet.**

## Public source
- [x] A clean source snapshot is published separately from the original private repository's development history.
- [x] Original personal-test-key workflow and internal acceptance documents are excluded.
- [x] Privacy, security, contributing and MIT licence documentation are included.
- [ ] Independently verify ownership and redistribution rights for the launcher image and all bundled artwork.
- [ ] Add real-device screenshots featuring **fictional** captures.
- [ ] Verify the public documentation and issue templates while signed out.

## Community APK (not yet released)
- [ ] Independently compile and test the Community build in GitHub Actions.
- [ ] Generate and back up a private signing key **outside the repository**.
- [ ] Configure the owner-only GitHub Actions secrets as documented in [COMMUNITY_RELEASE.md](COMMUNITY_RELEASE.md).
- [ ] Run the manual release-candidate workflow and verify signed output and SHA-256.
- [ ] Test installing alongside the original Quick app (no replacement or data loss).
- [ ] Test text/voice/photo capture, share-in, links, attachments, widget, shortcuts, reminders, search and categories.
- [ ] Test manual backup and restore, including non-sensitive test attachments.
- [ ] Test 7-day Done retention and update-in-place with the same Community key.
- [ ] Publish a GitHub Release with the accepted signed APK, SHA-256 checksum and release notes.

Do **not** publish an unsigned APK or one signed with a publicly available Android sample/test key. Do not place keystores or passwords in Git history or issues.
