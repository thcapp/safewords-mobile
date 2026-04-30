# Safewords — fastlane release pipeline

Drives Android releases to Google Play via the Play Developer Publishing API.

## One-time setup

1. Service account JSON exists at `output/play-store/v2/play-service-account.json` (relative to repo root). The `Appfile` references it.
2. Keystore exists at `~/keystores/safewords-release.jks` and the password at `~/keystores/safewords-release.password`.
3. Fastlane is installed: `sudo gem install fastlane --no-document`.

## Lanes

| Command | What it does |
|---|---|
| `fastlane validate` | Dry-run upload — confirms credentials work without changing anything on Play |
| `fastlane build` | bundleRelease, stage AAB at `../../../output/play-store/v2/app-release.aab` |
| `fastlane internal` | build + push to internal testing track |
| `fastlane closed` | build + push to closed (alpha) testing track |
| `fastlane production` | build + push to production at 10% staged rollout (asks for confirmation) |
| `fastlane promote_to_prod` | promote latest internal release to production at 10% staged rollout (asks for confirmation, no rebuild) |
| `fastlane metadata` | push only listing copy + screenshots, no AAB |

## Typical release flow

```bash
cd repos/safewords-android

# After every code change
fastlane internal

# Once internal testers confirm it works
fastlane promote_to_prod   # answers "y" to the confirmation prompt

# After Play approves and the 10% rollout looks healthy (no spike in
# crashes), bump rollout in Play Console to 50% then 100%, OR re-run
# `fastlane promote_to_prod` with rollout: "0.5" / "1.0"
```

## Listing assets

Fastlane reads listing copy + images from `metadata/android/en-US/`:
- `title.txt`, `short_description.txt`, `full_description.txt` — copy
- `images/icon/1.png` — 512×512 store icon
- `images/featureGraphic/1.png` — 1024×500 feature graphic
- `images/phoneScreenshots/{1..7}_*.png` — 7 phone screenshots
- `changelogs/{versionCode}.txt` — release notes for each AAB

To update listing copy without releasing a new build: edit those files then `fastlane metadata`.

## Adding a tester to internal testing

Internal track testers are managed via Play Console (Test and release → Testing → Internal testing → Testers tab). Fastlane doesn't manage tester lists. After a new internal release, existing testers see the update via the Play Store within ~15 minutes.

## What fastlane does NOT do

- App Content forms (Privacy policy, Data Safety, Content Rating, Advertising ID) — these live in Play Console UI and only need to be filled once. They follow the listing, not individual releases.
- Pricing & territories — Play Console UI.
- Initial app creation — Play Console UI (already done).

If a Play review escalates a question, that's a user decision; don't auto-respond to it.
