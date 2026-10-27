# Quick Tool — Generate your OWN private Android release signing key.
# Run on a trusted Windows computer. This file never sends the key anywhere.
# The secret must be entered into GitHub directly by the owner.
$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "Quick Tool — Community APK signing setup" -ForegroundColor Cyan
Write-Host "This makes a new signing key for Quick Community, NOT your existing Quick app."
Write-Host ""

$keytool = Get-Command "keytool" -ErrorAction SilentlyContinue
if ($null -eq $keytool) {
    throw "Java JDK 17+ keytool is not available. Install JDK 17, then open PowerShell again."
}

$directory = Join-Path $env:USERPROFILE "Quick Community Signing"
$keyFile = Join-Path $directory "quick-community-release.p12"
if (Test-Path -LiteralPath $keyFile) {
    Write-Host "A Quick Community signing key already exists at: $keyFile" -ForegroundColor Yellow
    Write-Host "KEEP this file. Never overwrite it, or future app updates may be impossible."
    exit 1
}

New-Item -ItemType Directory -Path $directory -Force | Out-Null
Write-Host "The next step asks for a private password twice. Keep it in a password manager."
Write-Host "It will NOT be shown or stored in this script."
Write-Host ""

& $keytool.Source -genkeypair -storetype PKCS12 -keystore $keyFile -alias "quick-community" -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Quick Community, O=Independent Developer, C=GB"
if ($LASTEXITCODE -ne 0 -or !(Test-Path -LiteralPath $keyFile)) {
    throw "Key generation failed. No release signing configuration has been uploaded."
}

Write-Host ""
Write-Host "SIGNING KEY CREATED" -ForegroundColor Green
Write-Host "Location: $keyFile"
Write-Host "SHA-256 file hash: $((Get-FileHash -LiteralPath $keyFile -Algorithm SHA256).Hash)"
Write-Host ""
Write-Host "IMPORTANT: Back up this .p12 file to an offline USB drive, and retain its password."
Write-Host "Never commit it to GitHub or share it in a chat. Do not delete this original key."
Write-Host ""
Write-Host "Open https://github.com/milncj1/Quick-Tool/settings/secrets/actions"
Write-Host "Add four repository Actions secrets:"
Write-Host "  QUICK_COMMUNITY_KEYSTORE_BASE64   = private keystore data (clipboard helper below)"
Write-Host "  QUICK_COMMUNITY_STORE_PASSWORD   = the password you chose"
Write-Host "  QUICK_COMMUNITY_KEY_ALIAS        = quick-community"
Write-Host "  QUICK_COMMUNITY_KEY_PASSWORD     = the same password you chose (PKCS12)"
Write-Host ""
$copy = Read-Host "Copy keystore data to the clipboard now? (Y/N)"
if ($copy -match '^[Yy]') {
    [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($keyFile)) | Set-Clipboard
    Write-Host "Keystore data copied. Paste it ONLY into the GitHub secret QUICK_COMMUNITY_KEYSTORE_BASE64."
    Read-Host "After saving that secret in GitHub, return here and press ENTER to clear the clipboard" | Out-Null
    "" | Set-Clipboard
    Write-Host "Clipboard cleared."
}
Write-Host ""
Write-Host "Once all four secrets are set, keep the keystore and password secure."
Write-Host "Do NOT run a public release until the Android candidate is tested and accepted."
