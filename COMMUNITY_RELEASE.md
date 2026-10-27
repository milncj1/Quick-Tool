# Installing and building Quick Community

Quick Community is the distribution build for public users. It is deliberately separate from the original personal Quick installation.

## Two Android identities

| Build | Application ID | Role |
| --- | --- | --- |
| Existing original Quick | `uk.co.milncj.quick` | Original, retained app. Its accepted installation and update/signature contract must not be changed. |
| Quick Community | `uk.co.milncj.quick.community` | Public distribution: separate installation and app data, signed using a private community-release key. |

**Quick Community cannot automatically see or reuse the original Quick app's database.** If a person wants to migrate, export a backup through the original Quick settings and restore from within Quick Community after installing it, subject to normal backup/restore compatibility and device validation. Do not uninstall the original app until a separate, verified backup exists.

## Public download

No approved public APK has yet been published. When one has passed signing and phone acceptance, download the APK from this project's GitHub **Releases** page and verify its SHA-256 against the published checksum. Android may require permission to install from your browser or file manager. Only install APKs you trust.

## Developer build

Install JDK 17, Android SDK Platform 36, Build Tools 36.0.0, and Gradle 9.6. In the repository root:

```sh
gradle :app:testDebugUnitTest
gradle :app:assembleCommunity
```

This command creates an **unsigned** Community build; it is not an installable trusted public release until signed. The normal debug build uses the original app's package name and is for development/testing only.

The current build workflow reconstructs the project-specific launcher icon from retained base64 chunks before Android compilation. Developers building locally can either use the provided XML fallback artwork, or reconstruct the exact approved PNG:

```sh
mkdir -p app/src/main/res/drawable-nodpi
cat notes/icon_parts/part*.b64 | tr -d '\r\n' | base64 -d > app/src/main/res/drawable-nodpi/ic_quick_launcher_final.png
rm app/src/main/res/drawable/ic_quick_launcher_final.xml
```

The optional PNG preparation removes the XML fallback so Android's resource resolver uses the PNG. It changes only the local working tree; do not commit the deleted XML.

## Signing — owner-only, once

**Never upload the release keystore to GitHub source files, attach it to an issue, or paste it into an AI chat.**

On your own Windows computer, use JDK 17's `keytool` from PowerShell to create a private signing keystore:

```powershell
keytool -genkeypair -storetype PKCS12 -keystore "$HOME\Documents\quick-community-release.p12" -alias quick-community -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Quick Community, O=Independent Developer, C=GB"
```

Choose a strong passphrase when prompted and retain it securely. Store a **second offline backup** of the file and password: losing them may prevent compatible updates to published installations.

In GitHub, open this repository's **Settings → Secrets and variables → Actions → New repository secret**. Create:

- `QUICK_COMMUNITY_KEYSTORE_BASE64`: the base64 text of the entire keystore file, copied on your own computer. In PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("$HOME\Documents\quick-community-release.p12")) | Set-Clipboard`; paste into GitHub's secret form, not in chats.
- `QUICK_COMMUNITY_STORE_PASSWORD`: the chosen keystore password.
- `QUICK_COMMUNITY_KEY_ALIAS`: `quick-community`.
- `QUICK_COMMUNITY_KEY_PASSWORD`: the key password (normally the same as the keystore password for a PKCS12 keystore).

Only when the source is accepted and secrets have been set, use **Actions → Build Quick Community release candidate → Run workflow** on `main`. The workflow runs tests, builds, privately signs, verifies and uploads a versioned APK plus checksum as a short-retention **private candidate artifact**. It deliberately does not automatically publish a GitHub Release.

## Public-release acceptance

Before public distribution, inspect the signed APK certificate and package ID, install it on an Android phone without replacing the original Quick app, and verify capture, share-in, widget, notifications, attachments, backup/restore and the seven-day completed-item purge. Then create the public GitHub Release with the approved APK and checksum. Refer to [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md).

## Important

The signing key stored as a GitHub Actions secret protects confidentiality only while access to repository settings and Actions is controlled. Keep public pull requests away from release workflows, never print secret values and require manual release. For extra assurance, signing may also be performed entirely offline by the owner instead of storing signing credentials in GitHub.
