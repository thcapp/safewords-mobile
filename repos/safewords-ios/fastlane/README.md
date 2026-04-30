# Safewords iOS fastlane pipeline

This scaffold supports the zero-Mac release path:

1. GitHub Actions runs on `macos-latest`.
2. XcodeGen creates the Xcode project from `project.yml`.
3. fastlane syncs signing assets from a private match repo.
4. fastlane builds the IPA and uploads it to TestFlight or App Store Review.

No credentials are committed. Local fallback credentials live under `output/app-store/` and are already ignored.

## Required GitHub secrets

Add these before running `.github/workflows/ios-release.yml`:

| Secret | Purpose |
|---|---|
| `APP_STORE_CONNECT_API_KEY_ID` | App Store Connect API key ID, currently `355BYQMBRM` |
| `APP_STORE_CONNECT_API_ISSUER_ID` | App Store Connect issuer UUID |
| `APP_STORE_CONNECT_API_KEY_BASE64` | Base64-encoded `.p8` private key |
| `APPLE_TEAM_ID` | Apple Developer Team ID for signing and match |
| `MATCH_PASSWORD` | Encryption password for the fastlane match repo |
| `MATCH_GIT_URL` | Private git URL for the match repo |
| `MATCH_GIT_BASIC_AUTHORIZATION` | HTTPS basic auth for the match repo |

If the match repo uses SSH instead of HTTPS, set `MATCH_GIT_PRIVATE_KEY` instead of `MATCH_GIT_BASIC_AUTHORIZATION`.

To produce the API-key secret from the staged key on Linux:

```bash
base64 -w0 output/app-store/AuthKey_355BYQMBRM.p8
```

On macOS:

```bash
base64 -i output/app-store/AuthKey_355BYQMBRM.p8 | tr -d '\n'
```

## Apple Developer setup blockers

The app target is `app.thc.safewords`; the widget extension is `app.thc.safewords.widget`.

Before the first signed build, Developer Portal identifiers and profiles must support the entitlements already in `project.yml`:

- App Groups: `group.app.thc.safewords`
- Keychain Sharing: team-prefixed `group.app.thc.safewords`
- Widget extension identifier: `app.thc.safewords.widget`

If these capabilities are not enabled before `fastlane ios match_setup`, signing profiles can be created without the required entitlements and the archive will fail.

## Lanes

| Command | What it does |
|---|---|
| `bundle exec fastlane ios validate` | Confirms the API key can see `app.thc.safewords` in App Store Connect |
| `bundle exec fastlane ios match_setup` | Creates/refreshes App Store signing profiles in the configured match repo |
| `bundle exec fastlane ios build` | Builds a signed App Store IPA at `output/app-store/ios/Safewords.ipa` |
| `bundle exec fastlane ios beta` | Builds and uploads the IPA to TestFlight |
| `bundle exec fastlane ios metadata` | Pushes listing metadata, age rating, and screenshots when present |
| `bundle exec fastlane ios release` | Builds, uploads metadata/binary, and submits for App Store Review |
| `bundle exec fastlane ios snapshot_setup` | Confirms the UI-test screenshot scaffold exists |
| `bundle exec fastlane ios screenshots` | Captures App Store screenshots through fastlane snapshot |
| `bundle exec fastlane ios privacy` | Uploads App Privacy nutrition, but requires Apple ID owner/admin auth |

## Typical first release flow

1. Fill the GitHub secrets above.
2. Ensure the app and widget identifiers have App Groups and Keychain Sharing enabled.
3. Run the workflow manually with lane `validate`.
4. Run the workflow manually with lane `match_setup` after the private match repo exists.
5. Run the workflow manually with lane `beta` to create the first TestFlight build.
6. After TestFlight passes, run lane `metadata` and then lane `release`.

Pushes to `release/ios-*` branches default to `beta`. Tags matching `ios-v*` default to `release`.

## Privacy and compliance

The project sets `ITSAppUsesNonExemptEncryption = false` because Safewords does not use non-exempt encryption for export-compliance purposes.

The App Store listing should use:

- Privacy policy: `https://safewords.io/privacy`
- Data collection: none
- Tracking: no
- Analytics, ads, telemetry: none

`fastlane/app_privacy_details.json` contains the no-data answer. fastlane's official docs currently state that App Privacy upload cannot use an App Store Connect API key, so the CI lanes do not attempt it. Use App Store Connect UI or the `privacy` lane with an owner/admin Apple ID if Apple requires the privacy form to be refreshed.
