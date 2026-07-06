# mjeans — Android WebView App

A signed Android APK that wraps `https://mjeans.ir/` in a native WebView.

> 🔒 **Private repo** — contains the source code, keystore (via GitHub Secrets), and CI workflow for building releases automatically.

---

## 📦 What this repo does

Whenever a tag like `v1.6` is pushed (or the workflow is manually triggered), GitHub Actions:

1. Builds a signed release APK using the keystore stored in GitHub Secrets
2. Verifies the APK signature (V1 + V2 schemes)
3. Verifies keystore continuity (SHA-256 must match `B7:94:02:...`)
4. Creates a GitHub Release with the APK + `assetlinks.json` as downloadable assets

---

## 🚀 Triggering a new release

### Option 1 — Push a tag (recommended)

```bash
# Bump versionCode + versionName in app/build.gradle first
git add app/build.gradle
git commit -m "Bump to v1.6"

# Push the commit
git push origin main

# Create and push the tag
git tag v1.6
git push origin v1.6
```

The workflow will run automatically. Once finished (3-5 minutes), a new release appears at:
```
https://github.com/AiPharmasy/mjeans-android/releases
```

### Option 2 — Manual trigger via Actions tab

1. Go to https://github.com/AiPharmasy/mjeans-android/actions
2. Select "Build & Release APK" workflow
3. Click "Run workflow"
4. Optionally override the version name and toggle auto-increment of versionCode

---

## 🔑 Signing setup (already configured)

The keystore is stored as a base64-encoded GitHub Secret named `MJEA_NS_KEYSTORE_BASE64`. The CI workflow decodes it on every build.

### Keystore info

| Field | Value |
|---|---|
| Package name | `ir.chabooksaz.mjeans` |
| Keystore file (in secret) | `mjeans-release.keystore` |
| Keystore password (secret) | `MJEA_NS_STORE_PASSWORD` |
| Key alias (secret) | `MJEA_NS_KEY_ALIAS` |
| Key password (secret) | `MJEA_NS_KEY_PASSWORD` |
| Algorithm | RSA 2048-bit |
| Validity | 25 years (until 2051) |
| **SHA-256 (locked)** | `B7:94:02:7E:64:56:64:C1:7E:2F:1E:D1:64:F7:D0:92:98:78:32:F1:AA:FD:C8:A8:93:ED:6F:7C:11:BA:5C:CD` |
| SHA-1 | `C1:6D:56:21:AD:45:56:AE:64:BD:D3:C9:7B:FE:4D:2D:6D:82:55:B8` |

> ⚠️ The CI workflow **fails the build** if the SHA-256 doesn't match `B7:94:02:...`. This prevents accidental keystore regeneration from breaking update continuity.

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
| **1.6+** | **7+** | **Built automatically via GitHub Actions** |

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

## 🔗 Related repos

- **`AiPharmasy/uploadZContent` (branch `mjeans-releases`)** — Older bundle-based distribution (kept for history). New releases will go to this repo's Releases tab instead.

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

---

## 📄 License

Proprietary. All rights reserved.
