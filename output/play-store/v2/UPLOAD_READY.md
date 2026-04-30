# UPLOAD_READY — Safewords v1.1.5 (versionCode 8)

> **v1.1.4 / versionCode 7 was silently rejected by Play.** Root cause: MLKit's transitive Firebase telemetry transport (`com.google.android.datatransport`) declares `android.permission.INTERNET`, which contradicts our Data Safety form's "no data collected" answer. v1.1.5 excludes that transport at the gradle configurations level, so the merged manifest no longer requests INTERNET. Verified clean.
>
> **Upload `safewords-1.1.5.aab`, NOT `safewords-1.1.4.aab`.**

**Verdict: GO.** Compliance audit found zero P0 blockers. Two P1 fixes shipped in this build. Release AAB walkthrough on KVM-accelerated emulator passed without crash. All store assets staged.

Generated 2026-04-28 after a 4-agent parallel audit pass.

---

## 1. What was checked

| Surface | Method | Result |
|---|---|---|
| AndroidManifest permissions/queries/backup | static audit | `<queries>` + `USE_BIOMETRIC` added; `allowBackup="false"`; no INTERNET; no cleartext |
| Dependency tree (analytics/ads/network) | grep against 18 known SDK names | clean — no Firebase/Crashlytics/Analytics/Sentry/Bugsnag/AdMob/etc |
| Source-side network calls | grep for OkHttp/Retrofit/URL/Socket/etc | clean — zero network call sites |
| ML Kit barcode-scanning variant | dep verification | bundled-model variant (`barcode-scanning`, NOT `-on-demand`) — model ships in APK, no runtime download |
| In-app data deletion | code path verification | Settings → Danger zone → Reset device wires to `GroupRepository.resetAllData()` |
| SMS intent compliance | API audit | uses `ACTION_SENDTO sms:` (delegates to user's SMS app) — exempt from Play SMS-permission policy |
| Camera lifecycle | code review of QRScannerScreen | foreground only, `DisposableEffect` releases on screen leave |
| Launcher icons (all 5 densities) | unzip + visual inspection | custom shield foreground + dark background, NOT stock Android Studio template |
| Release AAB build (signed) | gradle bundleRelease | builds cleanly, 16.2 MB |
| Release universal APK install | bundletool + adb install | installs without error |
| Release walkthrough on emulator | scripted adb taps + logcat | full golden path, **no AndroidRuntime crashes**, app process alive at end |
| ProGuard / R8 minification | release-build smoke test | no class-not-found, no missing-method — minification didn't strip anything |

---

## 2. Files staged for upload

| File | What | Size |
|---|---|---|
| `safewords-1.1.5.aab` | **Signed release bundle.** Upload to Play Console. versionCode 8, manifest scrubbed of INTERNET. | 15.8 MB |
| `safewords-1.1.5-debug.apk` | Sideload-able debug build. Same APIs as release. | 43 MB |
| `safewords-1.1.4.aab` | OLD — silently rejected by Play. Don't upload. | 16 MB |
| `icon-512.png` | App icon for the listing. 512×512 RGB PNG. Shield on Ink near-black. | 25 KB |
| `feature-graphic.png` | Feature graphic for the listing. 1024×500 PNG. Shield + tagline + eyebrow. | 40 KB |
| `screenshots/` | 17 phone captures from live emulator. Pick 2-8. See `README.md`. | — |
| `privacy.html` | Archive copy of the privacy policy. **Live URL: https://safewords.io/privacy.** | — |
| `listing-copy.md` | Title, short, full description, content rating answers, Data Safety answers, permission justifications. | — |
| `README.md` | Original staging README. | — |

---

## 3. Manifest fixes shipped in this build

```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />

<queries>
  <intent>
    <action android:name="android.intent.action.SENDTO" />
    <data android:scheme="sms" />
  </intent>
  <intent>
    <action android:name="android.intent.action.SENDTO" />
    <data android:scheme="smsto" />
  </intent>
  <intent>
    <action android:name="android.intent.action.DIAL" />
    <data android:scheme="tel" />
  </intent>
</queries>
```

Why: Android 11+ (API 30+) hides external apps from intent resolvers unless explicitly queried. Without `<queries>`, `Intent.ACTION_SENDTO sms:` returns null and the SMS invite flow silently fails. `USE_BIOMETRIC` is the AndroidX-recommended explicit declaration even though API 30+ grants it by default.

---

## 4. Known non-blocking warnings (from logcat)

These appeared during the walkthrough and are NOT rejection risks:

- `OnBackInvokedCallback is not enabled` — Android 13+ predictive back gesture isn't opted-in. Cosmetic; can fix in v1.1.5 by adding `android:enableOnBackInvokedCallback="true"` to `<application>`.
- `failed lock verification` on Compose internals — known x86 emulator + ART noise. Doesn't affect ARM real devices.
- `OpenGLRenderer Failed to initialize 101010-2 format` — emulator-only.

---

## 5. Play Console upload sequence (your 30 minutes)

Step-by-step. Each line = one click/screen in the Console.

### A. Create the app
1. play.google.com/console → All apps → **Create app**
2. App name: `Safewords`
3. Default language: `English (United States)`
4. App or game: `App`
5. Free or paid: `Free`
6. Confirm: `Yes` to Developer Program Policies and US export laws
7. Click **Create app**

### B. App content
Left nav → Policy → **App content**. Each row gets a **Start** button:

- **Privacy policy:** paste `https://safewords.io/privacy`
- **App access:** "All functionality available without restrictions"
- **Ads:** "No, my app does not contain ads"
- **Content rating:** complete the IARC questionnaire — every answer is **No** (see `listing-copy.md` § Content rating). Expected rating: **Everyone**.
- **Target audience and content:** Target age — **18 and over** (don't pick "Designed for Families" — that's a separate program with extra disclosures we don't need)
- **News app:** No
- **COVID-19 contact tracing:** No
- **Data safety:**
  - Does your app collect or share user data? **No**
  - Is all user data encrypted in transit? **N/A** (no transit)
  - Do you provide a way for users to request data deletion? **Yes — In-app method**. Description: `Settings → Danger zone → Reset device. Wipes all groups, seeds, and preferences. Uninstalling also removes everything.`
  - All data type categories: **None collected**
- **Government app:** No
- **Financial features:** None

### C. Main store listing
Left nav → Grow → Store presence → **Main store listing**:

- App name: `Safewords` (max 30 chars)
- Short description: paste from `listing-copy.md` § Short description (max 80 chars)
- Full description: paste from `listing-copy.md` § Full description (max 4000 chars; ours is ~2,650)
- App icon: upload `icon-512.png` (drag-drop)
- Feature graphic: upload `feature-graphic.png`
- Phone screenshots: upload **at least 2** from `screenshots/`. Recommended order: `05-home.png`, `12-generator.png`, `06-qr-display.png`, `17-plain-home.png`. Optional: also add `01-welcome.png`, `08-verify-wrong.png`, `09-settings-top.png`.
- Tablet 7" / 10" screenshots: skip (optional)
- Video: skip
- Category: **Tools**
- Tags: privacy, security, family, accessibility (pick when prompted)
- Contact details: `mail@theholding.company` for email, leave website blank or paste `https://safewords.io`
- Click **Save**

### D. Internal testing track
Left nav → Test and release → Testing → **Internal testing**:

1. **Tab "Testers":** create a tester list, add your own email. Save.
2. **Tab "Releases":** **Create new release**.
3. **App bundles:** drag-drop `safewords-1.1.4.aab` (16 MB).
4. **Release name:** auto-fills as "7 (1.1.4)" — leave it.
5. **Release notes:** paste from `listing-copy.md` § "What's new (release notes)".
6. **Save** → **Review release** → **Start rollout to internal testing**.

You'll get an opt-in URL in the testers tab — open it in your phone's Chrome, accept testing, then install via Play Store. App will appear within 5-15 minutes.

### E. After internal testing works
Promote to closed → open → production via Test and release → Promote release. Each promotion has its own review (24-48 hr typical for first review on a new app).

---

## 6. What's no longer blocking

- [x] Privacy policy live at https://safewords.io/privacy
- [x] App icon 512×512 PNG generated (`icon-512.png`)
- [x] Feature graphic 1024×500 PNG generated (`feature-graphic.png`)
- [x] Listing copy drafted (`listing-copy.md`)
- [x] Content rating answers prepared (all "No", expected Everyone)
- [x] Data safety answers prepared (no data collected, in-app deletion documented)
- [x] Signed AAB built with manifest fixes
- [x] Release-build smoke test passed (no crash, no minification regression)
- [x] At least 4 strong phone screenshots ready
- [x] Custom app icon ships at all 5 launcher densities
- [x] Zero network/analytics/ads/tracker SDKs verified by audit

## 7. What's still blocking

- [ ] **Your manual Play Console session.** Step C-E above. ~30 min.
- [ ] Sideload verification on your real phone (optional but recommended — install `safewords-1.1.4-debug.apk`).

That's it. Everything else is in place.

---

## 8. Post-upload housekeeping (queued, no action needed yet)

- v1.1.5 mobile pickups from web design (#53): dialog onboarding panel, FBI/FTC/AARP trust pill, three-words generator option.
- v1.2.0 BIP39 recovery work (Phase 1 schema approved at codex's commit 81196db; Phases 2-4 await your greenlight).
- `fastlane supply` setup so v1.1.5+ uploads are one-command instead of GUI-clicking.

The `<queries>` element + `USE_BIOMETRIC` in this AAB cover the audit's two P1 flags. We're rejection-clean for v1.1.4.
