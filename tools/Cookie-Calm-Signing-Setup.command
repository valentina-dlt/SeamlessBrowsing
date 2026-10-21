#!/bin/bash
set -euo pipefail

REPO="valentina-dlt/SeamlessBrowsing"
KEY_ALIAS="cookiecalm"
KEYCHAIN_SERVICE="Cookie Calm Android Release Signing"
KEYSTORE_DIR="$HOME/Library/Application Support/Cookie Calm"
KEYSTORE="$KEYSTORE_DIR/release-key.jks"

fail() {
  printf '\nSetup stopped: %s\n' "$1"
  printf 'You can close this window after reviewing the message above.\n'
  read -r -p 'Press Return to close... ' _ || true
  exit 1
}

printf 'Cookie Calm: permanent Android signing setup\n\n'
printf 'This creates one release key on this Mac, stores its passwords in Keychain,\n'
printf 'adds the signing secrets to %s, then starts a signed Android build.\n\n' "$REPO"

command -v gh >/dev/null 2>&1 || fail "GitHub CLI (gh) is not installed. Install it, run 'gh auth login', then double-click this file again."
command -v keytool >/dev/null 2>&1 || fail "Java keytool is not available. Install a JDK, then double-click this file again."
command -v python3 >/dev/null 2>&1 || fail "python3 is not available; it is needed to generate a strong random password."
gh auth status --hostname github.com >/dev/null 2>&1 || fail "GitHub CLI is not signed in. Run 'gh auth login' in Terminal, then double-click this file again."

umask 077
mkdir -p "$KEYSTORE_DIR"
chmod 700 "$KEYSTORE_DIR"

get_secret() {
  security find-generic-password -s "$KEYCHAIN_SERVICE" -a "$1" -w 2>/dev/null || true
}

STORE_PASS="$(get_secret keystore-password)"
KEY_PASS="$(get_secret key-password)"

if [[ -e "$KEYSTORE" ]]; then
  [[ -n "$STORE_PASS" && -n "$KEY_PASS" ]] || fail "A keystore file exists but its Keychain passwords are missing. I stopped to protect the existing permanent key; do not delete or replace it."
  export STORE_PASS KEY_PASS
  keytool -list -keystore "$KEYSTORE" -storepass:env STORE_PASS -alias "$KEY_ALIAS" >/dev/null 2>&1 || fail "The existing keystore could not be opened with the saved Keychain password. I stopped without replacing it."
  printf 'Found the existing permanent key; keeping it.\n'
else
  [[ -z "$STORE_PASS" && -z "$KEY_PASS" ]] || fail "Keychain signing passwords exist but the keystore file is missing. I stopped to avoid making a different key."
  STORE_PASS="$(python3 -c 'import secrets,string; print("".join(secrets.choice(string.ascii_letters + string.digits) for _ in range(48)))')"
  KEY_PASS="$STORE_PASS"
  export STORE_PASS KEY_PASS
  keytool -genkeypair -noprompt -keystore "$KEYSTORE" -storetype JKS -alias "$KEY_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
    -dname 'CN=Cookie Calm Android Release, OU=Android, O=Cookie Calm, L=London, ST=London, C=GB' \
    -storepass:env STORE_PASS -keypass:env KEY_PASS >/dev/null 2>&1 || fail "keytool could not create the signing key."
  chmod 600 "$KEYSTORE"
  security add-generic-password -U -s "$KEYCHAIN_SERVICE" -a keystore-password -w "$STORE_PASS" >/dev/null
  security add-generic-password -U -s "$KEYCHAIN_SERVICE" -a key-password -w "$KEY_PASS" >/dev/null
  printf 'Created the permanent key and saved its passwords in macOS Keychain.\n'
fi

printf '\nAdding the four signing secrets to GitHub...\n'
gh secret set ANDROID_KEYSTORE_BASE64 --repo "$REPO" < <(base64 < "$KEYSTORE" | tr -d '\n')
gh secret set ANDROID_KEYSTORE_PASSWORD --repo "$REPO" <<< "$STORE_PASS"
gh secret set ANDROID_KEY_ALIAS --repo "$REPO" <<< "$KEY_ALIAS"
gh secret set ANDROID_KEY_PASSWORD --repo "$REPO" <<< "$KEY_PASS"

printf '\nStarting the signed Android build...\n'
gh workflow run build.yml --repo "$REPO" --ref main
printf '\nDone. GitHub Actions is building the APK signed with your permanent key.\n'
printf 'Build page: https://github.com/%s/actions/workflows/build.yml\n' "$REPO"
printf 'Keep this Mac Keychain entry and the file at:\n%s\n\n' "$KEYSTORE"
printf 'Future builds will use the same signing key so Android can update the app in place.\n'
printf 'A previously installed APK signed with a different key may still need one final replacement.\n'
read -r -p 'Press Return to close... ' _ || true
