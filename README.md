# mjeans — Android WebView App

A signed Android APK that wraps `https://mjeans.ir/` in a native WebView.

> 🤖 **Automated releases via GitHub Actions** — push a tag like `v1.6` and a signed APK is built and published automatically.

---

## 🚀 How to release a new version

### Option 1 — Push a tag (recommended)

```bash
# 1. Make sure main is clean and at the commit you want to release
git checkout main
git pull

# 2. Create and push a tag with the version number
git tag v1.10
git push origin v1.10
```

That's it! GitHub Actions will:
1. Build a signed release APK using the keystore from GitHub Secrets
2. Verify the APK signature (V1 + V2 schemes)
3. Verify keystore continuity (SHA-256 must match `b794027e...`)
4. Compute versionCode from the version name (`v1.10` → versionCode `110`, `v2.0` → `200`)
5. Create a GitHub Release with the APK + `assetlinks.json` as downloadable assets

Total time: ~3-5 minutes.

The release will appear at: https://github.com/AiPharmasy/mjeans-android/releases

### Option 2 — Manual trigger via Actions tab

1. Go to https://github.com/AiPharmasy/mjeans-android/actions
2. Select "Build & Release APK" workflow
3. Click "Run workflow"
4. Optionally override the version name (e.g. `1.10`) and toggle auto-increment of versionCode

> ⚠️ Manual triggers will increment versionCode from the value in `app/build.gradle`. For consistent versioning, prefer tag-based releases.

---

## 📦 Latest release

| | |
|---|---|
| **Tag** | [v2.0](https://github.com/AiPharmasy/mjeans-android/releases/tag/v2.0) |
| **APK** | `mjeans-2.0-release.apk` |
| **Version Code** | 200 |
| **SHA-256** | `b794027e645664c17e2f1ed164f7d092987832f1aafdc8a893ed6f7c11ba5ccd` |

---

## 🔑 Signing setup (configured)

The keystore is stored as a base64-encoded GitHub Secret named `MJEA_NS_KEYSTORE_BASE64`. The CI workflow decodes it on every build.

### GitHub Secrets (already configured)

| Secret name | Purpose |
|---|---|
| `MJEA_NS_KEYSTORE_BASE64` | Base64-encoded keystore file |
| `MJEA_NS_STORE_PASSWORD` | Keystore password |
| `MJEA_NS_KEY_ALIAS` | Key alias (`mjeans`) |
| `MJEA_NS_KEY_PASSWORD` | Key password |

### Keystore info

| Field | Value |
|---|---|
| Package name | `ir.chabooksaz.mjeans` |
| Algorithm | RSA 2048-bit |
| Validity | 25 years (until 2051) |
| **SHA-256 (locked)** | `B7:94:02:7E:64:56:64:C1:7E:2F:1E:D1:64:F7:D0:92:98:78:32:F1:AA:FD:C8:A8:93:ED:6F:7C:11:BA:5C:CD` |
| SHA-1 | `C1:6D:56:21:AD:45:56:AE:64:BD:D3:C9:7B:FE:4D:2D:6D:82:55:B8` |

> ⚠️ The CI workflow **fails the build** if the SHA-256 doesn't match `b794027e...`. This prevents accidental keystore regeneration from breaking update continuity.

### If you ever need to update the keystore secret

```bash
# 1. Encode the keystore as base64
base64 -w 0 mjeans-release.keystore > keystore.b64

# 2. Copy the contents
cat keystore.b64

# 3. Go to: Settings → Secrets and variables → Actions
#    https://github.com/AiPharmasy/mjeans-android/settings/secrets/actions
# 4. Update MJEA_NS_KEYSTORE_BASE64 with the new value
# 5. Also update the EXPECTED SHA-256 in .github/workflows/build-release.yml
```

---

## 📱 App specs

| Spec | Value |
|---|---|
| Package name | `ir.chabooksaz.mjeans` |
| App name (Persian) | مرسدس |
| Min SDK | 23 (Android 6.0) |
| Target SDK | 34 (Android 14) |
| Entrypoint | `https://mjeans.ir/` |
| Signature | V1 + V2 ✓ |

---

## ✨ Feature history

| Version | versionCode | Key changes |
|---|---|---|
| 1.0 | 1 | Initial WebView wrapper |
| 1.1 | 2 | SwipeRefresh fix; App Links |
| 1.2 | 3 | Keystore finalized |
| 1.3 | 4 | Re-bundle for distribution |
| 1.4 | 5 | Multi-window support + CSS injection (introduced tab-bar position bug) |
| 1.5 | 6 | Revert sticky→fixed; keep translateZ(0) for hit-region stability |
| 1.6 | 7 | First GitHub Actions release (workflow_dispatch) |
| 1.7 | 7 | Workflow_dispatch with input versioning |
| 1.9 | 7 | First tag-triggered release (early workflow) |
| **2.0** | **200** | **versionCode derived from version name (v2.0 → 200); tag-triggered auto-release** |

---

## 🛠️ Local development

### Prerequisites

- JDK 17+
- Android SDK with `platforms;android-34` and `build-tools;34.0.0`
- (Optional) Android Studio

### Build locally

```bash
# 1. Set up environment variables for signing
export MJEA_NS_STORE_PASSWORD='Mj@ns#2026!Key'
export MJEA_NS_KEY_ALIAS='mjeans'
export MJEA_NS_KEY_PASSWORD='Mj@ns#2026!Key'

# 2. Place keystore at ./keystore/mjeans-release.keystore
mkdir -p keystore
cp /path/to/mjeans-release.keystore keystore/

# 3. Build
chmod +x gradlew
./gradlew assembleRelease

# 4. Output
ls app/build/outputs/apk/release/app-release.apk
```

---

## 🔗 App Links setup

For `https://mjeans.ir/...` links to open the app automatically, the site administrator must deploy `assetlinks.json` (attached to every release) to:

```
https://mjeans.ir/.well-known/assetlinks.json
```

See the `uploadZContent` repo's `docs/assetlinks-setup-guide.md` for full instructions.

---

## 📜 Lessons learned (do not repeat)

### v1.4 → v1.5: Tab-bar position bug
**Symptom:** Bottom bar appeared at the top of the screen.
**Cause:** Injected CSS changed `.tab-bar` from `position: fixed` → `position: sticky`. The site's body is a flex column, so `sticky; bottom: 0` on the last child sticks to the bottom of the flex container (which is above the viewport).
**Fix:** Keep `position: fixed`. Only add `transform: translateZ(0)` and `z-index` for hit-region stability.
**Lesson:** Never change the site's `position` value via injected CSS.

### v1.0 → v1.2: Keystore regeneration
**Cause:** Environment reset wiped the local keystore.
**Lesson:** Always store the keystore in a secure location (now: GitHub Secrets). The CI workflow verifies SHA-256 continuity on every build.

### CI: 'on' boolean coercion
**Cause:** YAML 1.1 parses the unquoted key `on` as boolean `True`, which broke GitHub Actions trigger parsing (job failed instantly with 0 steps).
**Fix:** Quote the key: `"on":` in the workflow YAML.

### CI: gradle-wrapper.jar wrong artifact
**Cause:** The `gradle-wrapper.jar` copied from `gradle-8.5/lib/plugins/gradle-wrapper-8.5.jar` was the wrong artifact (missing `IDownload` class).
**Fix:** Generate the wrapper properly with `gradle wrapper --gradle-version 8.5`.

### CI: Env vars in gradle.properties
**Cause:** Used `${VAR}` syntax in `gradle.properties` to reference environment variables, but Gradle treats these as project properties (literal strings), not env vars.
**Fix:** Use `System.getenv("VAR")` directly in `app/build.gradle` with fallback to `project.findProperty(...)`.

### CI: versionCode not incrementing on tag pushes
**Cause:** Each tag-push run started from a clean checkout of `app/build.gradle` (versionCode=6), bumped to 7, but didn't commit back. Every release got versionCode=7.
**Fix:** For tag pushes, derive versionCode from the version name: `major*100 + minor` (e.g. v1.9 → 109, v2.0 → 200). For manual runs, increment from the build.gradle value.

---

## 📄 License

Proprietary. All rights reserved.
