# Persistent Android APK signing

Android only accepts an APK as an update when it is signed with the same private key as the installed app. The CI workflow therefore publishes a Samsung APK only when a persistent release key is configured. It never publishes the temporary debug APK as an installable release.

## One-time setup

Create a release keystore on a trusted computer and keep a backup somewhere private:

```sh
keytool -genkeypair -v -keystore cookie-calm-release.jks -alias cookie-calm -keyalg RSA -keysize 4096 -validity 10000
```

Choose a strong keystore password and key password, and keep both safe. Losing this keystore means Android will reject future app updates.

Convert the keystore to one-line Base64 (Python 3):

```sh
python -c "import base64,pathlib; print(base64.b64encode(pathlib.Path('cookie-calm-release.jks').read_bytes()).decode())"
```

In the GitHub repository, open **Settings → Secrets and variables → Actions → New repository secret** and add these four repository secrets:

| Secret name | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The one-line Base64 output above |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | `cookie-calm` |
| `ANDROID_KEY_PASSWORD` | Key password |

Do not commit or upload the keystore or passwords into the repository. The workflow uses these secrets to sign release APKs. After the first signed APK is installed, future signed APKs from the workflow will install as updates.

If the existing phone installation was signed with an earlier temporary debug key, Android requires uninstalling that copy once before installing the first APK signed with this permanent key. The old key cannot be recovered from an APK.
