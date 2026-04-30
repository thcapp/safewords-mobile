# Safewords v1.1.4 — Play Store upload package

Generated 2026-04-28. Everything Play Console needs to publish v1.1.4 to the internal testing track is in this directory. The only remaining steps require you to log in to play.google.com/console.

## Files

| File | Purpose |
|---|---|
| `safewords-1.1.4.aab` | **Signed release bundle.** This is what you upload to Play Console. versionCode 7, 16 MB. |
| `safewords-1.1.4-debug.apk` | Sideload-able debug build. Use for personal testing on a phone. |
| `safewords-1.1.0.aab` | Older signed bundle (versionCode 3). Kept for reference. Do not upload — version is lower. |
| `privacy.html` | Privacy policy archive copy. **Live public URL: https://safewords.io/privacy** — use this in Play Console. |
| `listing-copy.md` | All Play Store listing text — title, short, full description, release notes, content rating answers. |
| `screenshots/` | 17 phone screenshots (1080×2400) captured from the live v1.1.4 emulator. See screenshot guide below. |
| `README.md` | This file. |

## Screenshot guide

Pick **2–8** for the Play listing. Recommended order, all 1080×2400:

| Pick | File | Why it sells |
|---|---|---|
| ★ | `05-home.png` | The hero — countdown ring, live word, brand identity in one frame. |
| ★ | `12-generator.png` | "Look, it works without a group too." Shows the new generator. |
| ★ | `06-qr-display.png` | The "no server" pitch made visual: invite via QR, no account. |
| ★ | `17-plain-home.png` | Accessibility / High Visibility mode for grandparents. |
| ◎ | `01-welcome.png` | First-impression hook: "One word between trust and deception." |
| ◎ | `08-verify-wrong.png` | Shows the verify-on-call flow with a real "wrong word" state. |
| ◎ | `09-settings-top.png` | Demonstrates rotation interval picker — proves it's configurable. |

★ = "must include", ◎ = strong supporting shot.

`02-paths.png`, `03-create-form.png`, `04-create-filled.png`, `15-plain-onboarding-1.png`, `16-plain-onboarding-2.png` are useful for documentation but less compelling for the storefront. `10-settings-mid.png`, `11-settings-bot.png`, `13-generator-regenerated.png`, `14-settings-top-after.png` are extras.

## Upload checklist (when you log into Play Console)

1. **Create app** (skip if already created)
   - App name: `Safewords`
   - Default language: English (United States)
   - App or game: App
   - Free or paid: Free
   - Declarations: confirm Developer Program Policies + US export laws
2. **App content** section — answer all of these from `listing-copy.md`:
   - Privacy policy URL (paste the public URL after hosting `privacy.html`)
   - Ads: No
   - App access: All functionality available without restrictions (no login)
   - Content rating: complete IARC questionnaire (all "No" — see `listing-copy.md`)
   - Target audience: All ages
   - News app: No
   - COVID-19 contact tracing: No
   - Data safety: "No data collected or shared" — see `listing-copy.md`
   - Government app: No
   - Financial features: None
3. **Main store listing** — paste from `listing-copy.md`:
   - App name, short description, full description, what's new
   - App icon: 512×512 PNG (regenerate from `output/icon/safewords-icon-foreground.svg`; see `output/icon/`)
   - Feature graphic: 1024×500 PNG (**not yet generated** — easiest with the existing icon SVG + Ink theme background)
   - Phone screenshots: upload at least 2 from `screenshots/`
   - Category: Tools
   - Tags: privacy, security, family, accessibility
4. **Internal testing track**
   - Create a new release
   - Upload `safewords-1.1.4.aab`
   - Release notes: copy from `listing-copy.md` "What's new" section
   - Add yourself as a tester via email (or paste your email into a 1-person tester list)
   - Roll out
5. **After internal testing** — promote to closed → open → production once you're satisfied.

## What's still blocking production launch

- [x] Public privacy policy URL — **live at https://safewords.io/privacy**
- [ ] 512×512 app icon PNG (raster export of the existing SVG)
- [ ] 1024×500 feature graphic
- [ ] Play Console: app created, content questionnaires answered

## What's done

- [x] Signed release bundle at versionCode 7 / 1.1.4
- [x] Working debug APK for sideload testing
- [x] Privacy policy text
- [x] Listing copy (title, short, long, release notes, all questionnaire answers)
- [x] 17 screenshots captured from the live emulator
- [x] Custom adaptive app icon (foreground SVG + dark background — see `output/icon/`)
- [x] Every screen wired to real logic (no placeholder onClick handlers)
- [x] Discoverable Plain mode exit
- [x] Single-use word generator
- [x] Generator on emergency override word setting
