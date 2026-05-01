# Release State Snapshot

Last verified: 2026-05-01

This is an audit-able snapshot of where the apps are in the release pipeline, queried directly against Play and TestFlight APIs. Update by re-running the queries below.

## Android (Google Play)

| Track | Status | Version | Notes |
|---|---|---|---|
| Internal | completed | 1.3.1 (versionCode 15) | Live to internal testers |
| Closed (alpha) | empty | — | Not started |
| Open (beta) | draft | 1.1.5 (versionCode 8) | Stale; should be dismissed before production submission |
| Production | empty | — | Not submitted |

### Outstanding gates for production submission

1. **Fresh v1.3 screenshots** — current 7 phone screenshots (`1_home.png`..`7_settings.png`) are from v1.0/v1.1 and don't show: Plain-as-default, demo mode, primitives toggles in Settings, Safety Cards browser, ChallengeSheet, override reveal. Google's screenshot policy is strict; submit-as-is will likely be rejected.
2. **Listing copy refresh** — current short/full description still uses v1.0 framing. v1.3 features (override, challenge/answer, numeric, demo mode) aren't surfaced.
3. **Console-only declarations** (no API):
   - Privacy policy URL (`https://safewords.io/privacy` is live; needs to be set in App content)
   - Content rating (IARC questionnaire)
   - Data safety form (declared "no data collected")
   - Target audience + content
   - Ads declaration (no)
   - App access (no login required)
   - Permissions declaration (camera = QR scanning)
4. **Optional cleanup**:
   - Dismiss the v1.1.5 stale beta draft
   - Pre-launch report check on Play Console
   - Legacy "Emergency override word" Settings entry (superseded by static override primitive but not removed)

THC Media LLC is an organization-account developer, so the 14-day-closed-testing-with-12-testers requirement (which only applies to *personal* accounts created after Nov 2023) does **not** apply.

### How to query Play state

```bash
ssh u5 'ruby -e "
require \"google/apis/androidpublisher_v3\"
require \"googleauth\"
S = Google::Apis::AndroidpublisherV3
auth = Google::Auth::ServiceAccountCredentials.make_creds(
  json_key_io: File.open(\"/home/ultra/code/safewords-mobile/output/play-store/v2/play-service-account.json\"),
  scope: [\"https://www.googleapis.com/auth/androidpublisher\"])
svc = S::AndroidPublisherService.new
svc.authorization = auth
edit = svc.insert_edit(\"app.thc.safewords\")
%w[internal alpha beta production].each do |t|
  begin
    info = svc.get_edit_track(\"app.thc.safewords\", edit.id, t)
    info.releases.each { |r| puts \"#{t}: #{r.status} #{r.version_codes.inspect} #{r.name}\" }
  rescue => e
    puts \"#{t}: empty or err\"
  end
end
"'
```

## iOS (App Store Connect / TestFlight)

| Track | Status | Version | Notes |
|---|---|---|---|
| TestFlight (internal) | active | 1.3.1 / build 7 | In Beta Build Review |
| TestFlight (external) | not configured | — | Not used |
| App Store production | empty | — | Not submitted |

Earlier TestFlight builds (1.1.0/3, 1.2.0/4, 1.3.0/5) cleared review and are still in the build list. v1.3.1/7 is the latest.

### Outstanding for App Store production submission

App Store and Play Store gating requirements differ. iOS submission requires:
- Marketing description, keywords, support URL (in `repos/safewords-ios/fastlane/metadata/en-US/`)
- Screenshots for all device sizes (6.7", 6.5", 5.5") — same staleness issue as Android
- Privacy policy URL
- Age rating questionnaire (Apple's, distinct from IARC)
- Export compliance answer (`ITSAppUsesNonExemptEncryption` already set to false in Info.plist for the symmetric HMAC use case)
- App Privacy nutrition labels (declared "no data collected")
- App Review submission

iOS export compliance is simpler than Android because we set `ITSAppUsesNonExemptEncryption = false`; Apple accepts the self-declaration that we're not using restricted crypto.

## Cross-platform parity check

Both apps must shipping the same v1.3.1 contract:
- Same primitives (rotating word, numeric, static override, challenge/answer)
- Same shared vectors at `/shared/primitive-vectors.json`
- Same demo mode (seed bytes match exactly)
- Same migration semantics for v1.2 groups

Verifiable by:
1. Install both, both in demo mode, on same date → home-screen word should match
2. Install one, scan QR from the other to join the same real group → both phones show the same rotating word
3. Run unit tests on both: Android `:app:testDebugUnitTest`, iOS `xcodebuild test -scheme Safewords` — both must pass against the shared vector files

## Build/release pipeline endpoints

### Android (run on `u5`)
```bash
cd /home/ultra/code/safewords-mobile/repos/safewords-android
./gradlew :app:testDebugUnitTest      # tests
./gradlew :app:assembleDebug          # debug APK for sideload
fastlane build                         # signed AAB only
fastlane internal                      # build + push to internal track
fastlane closed                        # build + push to closed/alpha
fastlane production                    # build + push to production (with confirm)
fastlane promote_to_prod               # promote latest internal release
fastlane metadata                      # listing copy + screenshots only
```

Local build tooling lives on `u5` (gradle wrapper, Android SDK, keystore, fastlane gem). The primary VM has neither gradle nor JDK; sync via `rsync` before invoking.

### iOS (run on GitHub Actions macOS runner)
```bash
gh workflow run "iOS Release" --ref main -f lane=beta         # archive + TestFlight upload
gh workflow run "iOS Release" --ref main -f lane=validate     # diagnostic run, no upload
```

Triggered manually (`workflow_dispatch`) or auto on push to `main` for `beta` lane. The macOS runner has Xcode 26.3.0, fastlane match, and the encrypted certificates repo. Local iOS builds are not possible from the primary VM — only the runner has Xcode.

## Recovery from a stuck state

If a track's draft release is stuck (orphaned, can't promote, can't replace):
- Use `fastlane validate` (Android) to confirm the AAB is accepted by the API
- Use Play Console UI to manually discard the draft if the API rejects with "version already used"
- For iOS, App Store Connect's "Discard this build" or upload a higher build number

The release-pipeline gotchas doc (`docs/release-pipeline-gotchas.md`) has the full set of failure modes and their fixes from setting up these pipelines.
