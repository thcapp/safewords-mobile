# Release Pipeline Gotchas — Postmortem

Hard-won lessons from setting up Android (Play Store) and iOS (App Store)
shipping pipelines for Safewords. Read this before touching either pipeline.

---

## Cross-platform setup

### gh CLI auth is project-scoped on this box

Each project under `/data/code/` has its own `.auth/gh/hosts.yml`. Authing in
one Claude Code session does NOT propagate to another project's session.

**To use an existing token in a new project:**
```bash
cp /data/code/<project-with-auth>/.auth/gh/hosts.yml /data/code/<this-project>/.auth/gh/hosts.yml
```

**Token scopes matter — verify before relying on them:**
```bash
gh api -i / 2>&1 | grep -i x-oauth-scopes
```

The default token from gh CLI's OAuth app gets `gist, read:org, repo` —
NOT `workflow`. Pushing `.github/workflows/*` files requires `workflow` scope.
Adding it via `gh auth refresh -s workflow` may be blocked by org-level OAuth
restrictions. Workaround: paste workflow YAML via the GitHub web UI directly.

### Repo creation under an org needs the right org name

`thcllc` (the user account) and `thcapp` (the org) are different. User
preferred org-owned for branding. Always confirm with the user.

### Pre-push secret audit

Before every first push, sweep for credentials:
```bash
find . -type f \( -name "*.jks" -o -name "*.keystore" -o -name "*.p8" \
  -o -name "*.p12" -o -name "*service-account*.json" -o -name "*.password" \
  -o -name "AuthKey_*" \)
git check-ignore -v <each-found-file>
```

`output/` directories are dev convenience but contain secrets. Make sure
the .gitignore covers `**/AuthKey*.p8`, `**/*service-account*.json`,
`**/*.jks`, `**/*.password`, `**/credentials.json`.

---

## Android / Play Store

### MLKit pulls in Firebase telemetry → INTERNET permission

`com.google.mlkit:barcode-scanning` transitively depends on
`com.google.android.datatransport:transport-backend-cct` which declares
`android.permission.INTERNET`. This contradicts a "no data collected"
Data Safety form and Play silently rejects the upload.

**Fix:** in `app/build.gradle.kts`:
```kotlin
configurations.all {
    exclude(group = "com.google.android.datatransport")
    exclude(group = "com.google.firebase", module = "firebase-encoders")
    exclude(group = "com.google.firebase", module = "firebase-encoders-json")
}
```
Plus `proguard-rules.pro`:
```
-dontwarn com.google.android.datatransport.**
-dontwarn com.google.firebase.encoders.**
-dontwarn com.google.firebase.components.**
```

### Manifest `<queries>` required for SMS / DIAL intents (API 30+)

Without `<queries>`, the app can't resolve external intents on Android 11+.

```xml
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

### Widget receiver class must be in the app module's dex

If the widget code lives in a separate `:widget` Gradle module that the
`:app` module doesn't depend on, the receiver class won't ship in the APK.
Android registers the widget but can't instantiate the receiver →
widget stuck on the initial loading layout forever.

**Fix:** put widget Kotlin files directly in `app/src/main/kotlin/.../widget/`
and add Glance + WorkManager deps to `app/build.gradle.kts`.

### Glance widget tap-to-open

Need both an `actionStartActivity` modifier on the root composable AND
the actual Intent constructed from the GlanceAppWidget context (the
generic version `actionStartActivity<MainActivity>()` requires more setup;
explicit Intent is more reliable):

```kotlin
val launchIntent = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
}
Column(modifier = GlanceModifier.clickable(actionStartActivity(launchIntent))) { ... }
```

### Widget realtime ticking is impossible

Android caps periodic widget updates at 30 minutes (`updatePeriodMillis`
≥ 1800000). For UX as close as realtime as possible:
- WorkManager periodic at 30 min calling `SafewordsWidget().updateAll()`
- Runtime BroadcastReceiver in `Application.onCreate()` for
  `ACTION_USER_PRESENT` + `ACTION_SCREEN_ON` (these can NOT be declared
  in manifest on Android 8+)

### Play Console first upload errors

| Error | Cause | Fix |
|---|---|---|
| "You need to upload an APK or Android App Bundle" | Browser UI shows file but upload didn't actually process | Drop file, wait for green checkmark + version row before clicking Save |
| "Permissions require privacy policy" | CAMERA permission declared, no privacy URL | Set URL at App content → Privacy policy AND publish (not just save) |
| Silent upload rejection | INTERNET permission in manifest contradicts Data Safety form | See MLKit telemetry section above |
| "Version code N has already been used" | Even failed/draft uploads consume version codes | Bump versionCode in build.gradle.kts |
| "Only releases with status draft may be created on draft app" | App not yet published to production | `release_status: "draft"` in fastlane lanes until first prod release |

### Play Console "Service accounts" tab varies

In some Console layouts, service account permissions are managed via a
"Service accounts" tab. In the modern layout, they're managed under
**Users and permissions** as if they were team members — paste the
service account email into "Invite new users".

### Play Developer API access blocked for new accounts

Google gates the Play Developer API behind a 14-day account-age requirement
for new developer accounts (anti-fraud measure added 2023). Until that
window passes, the API access page may not be accessible.

---

## iOS / App Store

### Apple Developer Program enrollment

- Personal: ~immediate
- Organization: 1-2 day verification window with DUNS validation
- $99/year both ways

### App Store Connect entry creation prerequisites

You CANNOT create an ASC entry until the Bundle ID exists at
`developer.apple.com/account/resources/identifiers/list`. Register the
**App ID** there first as type "App ID > App", explicit Bundle ID,
matching `app.thc.safewords` or whatever your namespace is.

### App name on App Store must be globally unique

If your preferred name is taken, append a tagline. Apple separates the
ASC listing name from the home-screen icon label — `CFBundleDisplayName`
in Info.plist controls what users see under the icon, independent of the
ASC listing name.

For Safewords, listing = `Safewords - Verify Identity`, home icon = `Safewords`.

### "Keychain Sharing" is NOT a Developer Portal capability

It's an entitlement set in `*.entitlements` (or via Xcode's Signing &
Capabilities → +Capability). The Developer Portal only has these capabilities
in its App ID list. Do NOT spend time hunting for a "Keychain Sharing"
checkbox there.

### App Groups requires three things at Apple's portal

1. The **App Group identifier** must exist:
   `Identifiers` dropdown → switch to **App Groups** → register
   `group.app.thc.safewords` (or whatever your group ID is)
2. **Each App ID** that uses the group must have the **App Groups capability**
   enabled in its config (edit App ID, check the box, save)
3. **The specific group** must be assigned to each App ID (App Groups
   detail page on the App ID, check the group, save)

Skipping #3 is the most common foot-gun — App Groups capability is enabled
but the actual group identifier isn't linked.

### App Store Connect API key

Generate at App Store Connect → **Users and Access → Integrations → App Store
Connect API → +**. Required: name + role (use **App Manager**).

After generation, you get **three values**:
- Key ID (10-char string visible in the table row)
- Issuer ID (UUID at the top of the page)
- The `.p8` private key file (one-time download — Apple won't let you
  re-download)

The filename Apple gives you embeds the Key ID: `AuthKey_355BYQMBRM.p8`.

### iOS pipeline GitHub Actions secrets

Use `gh secret set NAME -R org/repo -b "value"` (works with `repo` scope
alone — no `workflow` scope needed):

| Name | Value |
|---|---|
| `APP_STORE_CONNECT_API_KEY_ID` | 10-char Key ID |
| `APP_STORE_CONNECT_API_ISSUER_ID` | UUID Issuer ID |
| `APP_STORE_CONNECT_API_KEY_BASE64` | `base64 -w0 AuthKey_*.p8` |
| `APPLE_TEAM_ID` | 10-char Team ID from Membership page |
| `MATCH_PASSWORD` | random 256-bit password (`openssl rand -base64 32`) |
| `MATCH_GIT_URL` | HTTPS URL of the certs repo (no embedded creds — see Fastfile) |
| `MATCH_GIT_BASIC_AUTHORIZATION` | `base64 -w0 "thcllc:gho_..."` of GitHub token + username |

### fastlane Spaceship token construction

`app_store_connect_api_key()` returns a **Hash**, not a `Spaceship::ConnectAPI::Token`.
Code that does `Spaceship::ConnectAPI.token = app_store_connect_api_key(...)`
will fail with `undefined method 'in_house' for an instance of Hash`.

**Fix:** construct the token directly:
```ruby
require "spaceship"
require "base64"
token = Spaceship::ConnectAPI::Token.create(
  key_id: ENV["APP_STORE_CONNECT_API_KEY_ID"],
  issuer_id: ENV["APP_STORE_CONNECT_API_ISSUER_ID"],
  key: Base64.decode64(ENV["APP_STORE_CONNECT_API_KEY_BASE64"]),
  duration: 1200,
  in_house: false
)
Spaceship::ConnectAPI.token = token
```

### fastlane API key conflicts in CI

If you pass `api_key:` (Hash) to `match()` AND have
`APP_STORE_CONNECT_API_KEY_PATH` env var set, match's internal `cert`
sub-action errors with "Unresolved conflict between options:
'api_key_path' and 'api_key'".

**Fix:** in CI, don't pass `api_key:`. Let the path env be picked up
automatically. Locally, use the Hash.

### `APP_STORE_CONNECT_API_KEY_PATH` expects JSON wrapper, not raw .p8

Modern fastlane reads this env var as a path to a **JSON wrapper file**:
```json
{
  "key_id": "...",
  "issuer_id": "...",
  "key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----",
  "in_house": false
}
```

If you point it at the raw `.p8` PEM, you get
`JSON::ParserError: invalid number: '-----BEGIN' at line 1 column 1`.

**Fix:** in the Fastfile, materialize a JSON wrapper before match runs:
```ruby
raw_path = ENV["APP_STORE_CONNECT_API_KEY_PATH"]
if !raw_path.empty? && File.exist?(raw_path)
  pem = File.read(raw_path)
  json_path = raw_path.sub(/\.p8\z/, "") + ".json"
  File.write(json_path, JSON.generate({
    "key_id" => ENV["APP_STORE_CONNECT_API_KEY_ID"],
    "issuer_id" => ENV["APP_STORE_CONNECT_API_ISSUER_ID"],
    "key" => pem,
    "in_house" => false
  }))
  ENV["APP_STORE_CONNECT_API_KEY_PATH"] = json_path
end
```

### GitHub Actions `actions/checkout@v4` clobbers match's git auth

`actions/checkout@v4` sets a global `http.https://github.com/.extraheader`
config with `GITHUB_TOKEN`. That token is scoped only to the current repo
and CANNOT clone the private certs repo. When match clones, this header
overrides match's `MATCH_GIT_BASIC_AUTHORIZATION` and git fails with
`could not read Username for 'https://github.com'`.

**Fix:** in the match-calling helper, before match runs:
```ruby
sh("git config --global --unset-all http.https://github.com/.extraheader || true")
sh("git config --global --remove-section http.https://github.com/ 2>/dev/null || true")
sh("git config --global --unset-all credential.helper || true")
ENV["MATCH_GIT_BASIC_AUTHORIZATION"] = nil  # clear, use URL-embedded instead
ENV["GIT_CONFIG_PARAMETERS"] = "'credential.helper='"  # force-disable all helpers
ENV["GIT_TERMINAL_PROMPT"] = "0"
```

Then construct a URL with embedded credentials at runtime:
```ruby
basic = ENV["MATCH_GIT_BASIC_AUTHORIZATION"].to_s  # before clearing
token = Base64.decode64(basic).split(":", 2).last.strip
url = ENV["MATCH_GIT_URL"].to_s.sub(
  "https://github.com/",
  "https://x-access-token:#{token}@github.com/"
)
match(git_url: url, ...)
```

### Provisioning profiles cache stale capabilities

If you add a capability (e.g., App Groups) to an App ID at Apple's portal
AFTER match has already generated profiles, the cached profiles do NOT pick
up the new capability. Builds will fail with:
```
"<Target>" requires a provisioning profile with the App Groups feature
```

**Fix:** force regenerate via match with `force: true`:
```ruby
match(
  ...,
  force: true,                  # regenerate profiles
  force_for_new_devices: true,  # include new device IDs
  include_all_certificates: true
)
```

Or run with the `force` parameter once after capability changes.

### TestFlight Beta Build Review

First build per app goes through Apple's automated **Beta Build Review**
(~24 hr). Internal testers can't install until the build passes review.
Subsequent builds skip this step (or take minutes).

### App Store Review

First production submission: ~24-72 hr. Subsequent submissions: usually
under 24 hr. Reviewers actually run the app on a real device. Provide
clear "App Review notes" explaining how to test the verification flow,
since the value of a safeword-checker isn't obvious without context.

### Privacy nutrition labels can't be uploaded via API key

Apple requires Apple ID + 2FA for the App Privacy / nutrition label form.
The App Store Connect API key alone won't suffice. Either fill the form
once via Console UI (it's a "no data collected" wholesale answer) or
build a separate `privacy` lane that uses Apple ID auth.

### Encryption export compliance

Set `ITSAppUsesNonExemptEncryption = NO` in Info.plist. Safewords uses
HMAC-SHA256 from CryptoKit, which qualifies for the standard export
exemption (Apple-stdlib crypto only). Without this flag, every TestFlight
build prompts for a fresh declaration.

---

## Cross-cutting patterns

### Always validate before destructive operations

Every release pipeline should have a `validate` lane that:
1. Confirms credentials authenticate against the platform's API
2. Dry-runs the upload without actually committing it
3. Returns success/failure deterministically

We use `fastlane validate` on Android (Play API) and iOS (ASC API).

### Smoke test on emulator before every push

Before every Android release, the CI/release lane should:
1. Boot the dev VM AVD
2. Install the debug APK
3. Walk a scripted golden path via adb input
4. Capture logcat, fail on any AndroidRuntime crash
5. Capture screenshots for listing assets

iOS equivalent uses fastlane snapshot + simulator.

### Don't trust "Authentication complete" — verify scopes via API

`gh auth refresh -s workflow` reports success even when the requested
scope is denied at the org level. Always verify with:
```bash
gh api -i / 2>&1 | grep -i x-oauth-scopes
```

### When a workflow file change is blocked by token scope

You can't update `.github/workflows/*.yml` without `workflow` scope. If
that's blocked, paste the file via the GitHub web UI:
```
https://github.com/<org>/<repo>/new/main?filename=.github/workflows/<file>.yml
```

### Background monitor for long-running CI

Use `gh run view <id> --json status,conclusion` in a poll loop with a
Monitor task. Each state change emits an event. Don't poll synchronously
in the foreground — block other work.

---

## Iteration history (this session)

iOS pipeline took **8 iterations** to reach a working `match_setup` lane:

1. Spaceship token: Hash assigned to `Spaceship::ConnectAPI.token` → fail
2. Match git clone: extraheader from actions/checkout intercepts → fail
3. URL with embedded creds + extraheader still set → conflicts → fail
4. Plain URL + basic auth: extraheader override still wins → fail
5. Cleared extraheader globally, helped — but `MATCH_GIT_TOKEN` not in
   workflow `env:` block → URL stayed plain → fail
6. Derived token from existing `MATCH_GIT_BASIC_AUTHORIZATION` secret →
   embedded URL works, BUT `api_key_path` env conflicted with `api_key`
   Hash → fail
7. Cleared `APP_STORE_CONNECT_API_KEY_PATH` env → fastlane couldn't find
   any key → fail
8. Wrote JSON wrapper for `APP_STORE_CONNECT_API_KEY_PATH` → SUCCESS

Then the build itself failed on missing App Groups in the provisioning
profile. Force-regen of profiles via match was required (in progress at
time of writing).

**Total time: ~25 minutes of iteration.** Most of those failures were
preventable with this doc up front.
