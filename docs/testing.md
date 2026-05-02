# Testing

How we test Safewords mobile, end-to-end. Read this before adding a new
flow, debugging a flow that's failing, or wiring a release gate that
depends on Maestro.

Status: **shipped** (Maestro 2.5.1 on `u5`, five flows green, fastlane
gates wired into `:internal` and `:beta`).
Last updated: 2026-05-01.

## 1. Overview

Two layers, owned by different concerns:

| Layer | What it covers | Source of truth |
|---|---|---|
| **Unit tests** | Byte-level primitive parity. HMAC derivations, BIP39 round-trip, migration v1.2 → v1.3, QR payload parse, recovery hex parser. Pure JVM (Android) / `XCTest` (iOS). | `/shared/test-vectors.json`, `/shared/primitive-vectors.json`, `/shared/migration-vectors.json`, `/shared/recovery-vectors.json` |
| **Maestro flows** | Nav graph + integration smoke. "Did the onboarding panels still wire to the right next screen", "does demo mode still produce a word", "do the cross-platform UIs still derive the same word from the same seed". Plus the screenshot pipeline that produces fresh listing assets. | `/data/code/safewords-mobile/maestro/flows/` |

We do **not** use Maestro to validate cryptographic correctness — the
unit tests already do that against frozen vectors and they're cheap to
run. Maestro is the integration tripwire above the unit layer: if a
nav action gets disconnected, a screen renames its trigger, or a new
release breaks the demo-mode entry, Maestro catches it before we ship.

We also do **not** use Maestro for biometric-gated screens (system
sheets aren't in the accessibility tree) or for the QR scanner
(emulator camera is unreliable). Those rely on the unit tests for
their primitives plus Subagent-A spot-checks (see §11).

## 2. Running flows locally

### Android (via `u5`)

The primary VM doesn't have gradle, the Android SDK, the emulator, or
Maestro itself. All of that lives on `u5`. The pattern is: rsync
sources up, then ssh in to run.

```bash
# 1. Sync flow YAMLs and any source changes you've made.
rsync -av /data/code/safewords-mobile/maestro/ \
    u5:/home/ultra/code/safewords-mobile/maestro/

# (If you also changed Android sources, sync those too — see
# docs/developer-guide.md §7 for the full rsync recipe.)

# 2. Boot the AVD if it isn't already up. Cold boot is 60-90s; do
# this once per session, not per-flow.
ssh u5 'export ANDROID_HOME=$HOME/android-sdk && \
    export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH" && \
    nohup emulator -avd pixel -no-window -no-audio >/tmp/emu.log 2>&1 &'

# 3. Install the debug APK that the flows expect (appId
# app.thc.safewords). If you haven't built recently:
ssh u5 'cd /home/ultra/code/safewords-mobile/repos/safewords-android && \
    ./gradlew :app:assembleDebug && \
    $HOME/android-sdk/platform-tools/adb install -r \
        app/build/outputs/apk/debug/app-debug.apk'

# 4. Run a single flow.
ssh u5 'export PATH="$HOME/.maestro/bin:$PATH" && \
    export MAESTRO_CLI_NO_ANALYTICS=1 && \
    export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true && \
    cd /home/ultra/code/safewords-mobile/maestro && \
    maestro test flows/onboarding-create.yaml'

# 5. Run all flows in a directory (Maestro picks up every .yaml,
# subflows skipped if their filename starts with _).
ssh u5 'export PATH="$HOME/.maestro/bin:$PATH" && \
    export MAESTRO_CLI_NO_ANALYTICS=1 && \
    export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true && \
    cd /home/ultra/code/safewords-mobile/maestro && \
    maestro test flows/'
```

The `MAESTRO_CLI_*` env vars suppress the analytics opt-out + AI
analyze opt-in banners. They don't affect behaviour but they cut log
noise in half.

### iOS (via the macOS GitHub Actions runner)

There's no local iOS option — neither VM has macOS. iOS Maestro runs
exclusively on the `macos-latest` runner inside the
`.github/workflows/ios-release.yml` workflow. Trigger it from the
primary VM:

```bash
gh workflow run ios-release.yml -f lane=beta
gh run watch
```

The workflow boots an iOS simulator, installs the debug build, runs
the Maestro suite, and only proceeds to the TestFlight upload if the
suite passes (see §8). Local iOS Maestro debugging means iterating on
the workflow run and reading the captured artifacts.

If you need to develop iOS flow logic without burning CI minutes,
write the YAML against the Android app first (the IDs are identical
across platforms). Once it's green there, run on the macOS runner to
catch any platform-divergent UI.

## 3. The flows

All flows live at `/data/code/safewords-mobile/maestro/flows/`. The
leading-underscore file is a reusable subflow.

| File | Purpose |
|---|---|
| `_setup-plain-bypass.yaml` | Reusable subflow. Clears state, walks PlainOnboarding, flips into Advanced view, lands on the real `OnboardingScreen`. Used by every flow that needs a fresh-install starting point. |
| `onboarding-create.yaml` | Happy path — create a new group with name + creator. Ends on Advanced Home with a rotating word visible. |
| `onboarding-demo.yaml` | "Try without a group" → demo TIGER group becomes active → demo banner shows in Plain → exiting demo wipes state and lands user back on Onboarding. |
| `onboarding-restore.yaml` | Paste the canonical 64-char hex seed (`seedA` from `/shared/test-vectors.json`); validates the parser end-to-end through the UI. |
| `parity-check.yaml` | Enter demo, capture the displayed word, emit `PARITY_WORD=...` to stdout. CI runs this on Android and iOS for the same wall-clock minute and diffs the captured strings. |
| `screenshots.yaml` | Walks the high-value v1.3 surfaces and writes 6 PNGs to `/maestro/screenshots/`. Source for both Play and App Store listing assets. |

## 4. Test ID registry

The cross-platform contract for accessibility tags lives at
`/data/code/safewords-mobile/shared/maestro-test-ids.md`. Read that
file once before authoring a flow.

### Naming convention

`{screen-or-feature}.{element-purpose}` in lowercase kebab-case:

- `plain-home.word-display`
- `onboarding.create-cta`
- `challenge-sheet.match-button`

### Rules

1. **Don't rename an ID.** Renaming is a breaking change for every
   flow that references it. If a screen genuinely needs a new ID,
   add a new one and leave the old one in place until every flow has
   migrated.
2. **Document misses.** When a registry ID can't be implemented on
   one platform (e.g. the underlying UI element doesn't exist), add
   it to the "Implementation status" section of the registry with a
   one-line note. Flow authors check that section before targeting an
   ID; "missing" or "n/a" tells them this step won't run there.
3. **Tag at the leaf, not the wrapper.** See §10 — testTag on a
   `Column` or `VStack` wrapper is a frequent cause of "no element
   matches" errors.

## 5. Adding a new flow

The mechanical steps:

1. **Tag any new UI elements** per the registry. If the screen you're
   targeting needs an ID that isn't in the registry yet, add it to the
   registry first (see §6). Do not ship a flow that targets an
   undocumented ID.
2. **Author the YAML** under `/data/code/safewords-mobile/maestro/flows/`.
   Conventional pattern:
   ```yaml
   appId: app.thc.safewords
   ---
   - runFlow: _setup-plain-bypass.yaml
   - tapOn:
       id: 'onboarding.welcome-cta'
   # ... your steps ...
   - extendedWaitUntil:
       visible:
         id: 'home.word-display'
       timeout: 5000
   ```
   Use `id:` selectors, never raw `text:` (text changes when copy
   changes; IDs are stable).
3. **Validate locally** by running the flow against the Android app
   on `u5` (commands in §2). If the flow needs platform-divergent
   behaviour, gate it with `optional: true` and run on both platforms
   before merging.
4. **Wire into fastlane** if it should be a release gate. The Android
   `:maestro_test` lane in `repos/safewords-android/fastlane/Fastfile`
   runs every YAML in `/maestro/flows/` by default — adding a file is
   enough to add it to the gate. Same for iOS via
   `repos/safewords-ios/fastlane/Fastfile`. If the new flow shouldn't
   run on every release (e.g. it's an explicit screenshot capture),
   put it under a different directory or rename so it falls outside
   the suite glob.

## 6. Adding a new test ID

When a new screen or surface needs a Maestro-targetable element:

1. **Add the ID to the registry first.** Open
   `/data/code/safewords-mobile/shared/maestro-test-ids.md`, find the
   right section (or add a new one), and document the ID + purpose +
   per-platform file pointers. This is platform-engineer territory —
   if you're authoring a flow and the ID you need isn't there, ping
   the platform owner rather than coining your own.
2. **Implement on Android (Compose):**
   ```kotlin
   Text(
       text = currentWord,
       modifier = Modifier.testTag("plain-home.word-display"),
   )
   ```
   The nav root must have `semantics { testTagsAsResourceId = true }`
   so the testTag becomes a resource ID Maestro can find via `id:`.
   That bootstrap is already in place in `MainActivity.kt`; just
   ensure new screens render under it.
3. **Implement on iOS (SwiftUI):**
   ```swift
   Text(currentWord)
       .accessibilityIdentifier("plain-home.word-display")
   ```
   No special wiring needed — SwiftUI exposes
   `accessibilityIdentifier` directly to UIAutomation/Maestro.
4. **Confirm both sides** by booting the app on each platform and
   running a one-line `maestro test --command "assertVisible: id:..."`
   smoke. If one platform's tag doesn't surface, you've hit one of
   the gotchas in §10.

The ID string must be **byte-identical** across iOS and Android. The
whole point of the registry is that one YAML targets either platform.

## 7. The screenshot pipeline

`screenshots.yaml` is the answer to the production-submission gate
"these listing assets are stale". Pre-Maestro, the Android pipeline
captured screens via the bash harness in
`repos/safewords-android/fastlane/screenshot_walkthrough.sh`, which
drove the AVD with raw `adb shell input` calls. That worked but was
brittle and only produced Android assets. The Maestro flow replaces
both legs with a single cross-platform script.

### How it runs

1. `maestro test flows/screenshots.yaml` walks 6 v1.3 surfaces:
   onboarding welcome, demo Home (Advanced), Settings →
   Verification, Safety Cards browser, ChallengeSheet, Plain home
   with the demo banner.
2. Each `takeScreenshot` writes a PNG to
   `/data/code/safewords-mobile/maestro/screenshots/`.
3. The fastlane `:screenshots_maestro` lane (Android) /
   equivalent step on iOS copies those PNGs into the platform-specific
   listing-asset directory:
   - **Android**: `repos/safewords-android/fastlane/metadata/android/en-US/images/phoneScreenshots/`
   - **iOS**: `repos/safewords-ios/fastlane/screenshots/en-US/`
4. The next `fastlane internal` (Android) or `fastlane release`
   (iOS) bundles the freshly copied PNGs into the metadata upload —
   `skip_upload_screenshots: false` is already set on `:internal`,
   and the iOS `:release` lane uses `skip_screenshots:
   !screenshot_files?` so the presence of the files alone is enough
   to push them.

### Re-capturing

```bash
# Android
ssh u5 'cd /home/ultra/code/safewords-mobile/repos/safewords-android && \
    fastlane screenshots_maestro'

# iOS — runs on the macOS runner
gh workflow run ios-release.yml -f lane=screenshots_maestro
```

After re-capture, eyeball the PNGs before pushing. The Maestro flow
runs in the Plain-bypass setup (clean state, demo seed) so the words
shown are deterministic for that wall-clock minute — which is good
for parity but means you'll see different words each capture run.
That's fine; what matters is that the layout and copy are current.

## 8. The pre-release gate

Both platforms' release lanes call Maestro before pushing the
binary. **Fail closed** — if any flow regresses, the release lane
exits non-zero and nothing uploads.

### Android: `:internal`

In `repos/safewords-android/fastlane/Fastfile`, the `:internal` lane
calls `:maestro_test` first. The lane:

1. Builds the debug APK and installs it on the AVD.
2. Boots the AVD if it isn't already up (or asserts it's reachable).
3. Runs every YAML in `/maestro/flows/` against
   `appId: app.thc.safewords`.
4. Aborts the lane if any flow fails. The AAB never gets built;
   nothing reaches Play.

### iOS: `:beta`

In `repos/safewords-ios/fastlane/Fastfile`, the `:beta` lane runs the
same suite against an iOS simulator on the macOS runner before
calling `upload_to_testflight`. Same fail-closed semantics — a red
flow blocks the TestFlight upload.

The `:production` (Android) and `:release` (iOS) lanes don't re-run
Maestro — they trust `:internal` / `:beta`. If you bypass the
internal/beta path (e.g. promoting an artifact built off-pipeline),
Maestro doesn't run. Don't do that.

## 9. Cross-platform parity validation

`parity-check.yaml` is the integration tripwire for "do iOS and
Android still derive the same word from the same seed at the same
time, end-to-end through the UI". The unit tests already check this
at the byte level against frozen vectors; this flow checks it through
the actual rendered text.

### How it works

1. The flow enters demo mode (deterministic seed, deterministic
   group ID).
2. `copyTextFrom: id:home.word-display` puts the rendered word into
   `${maestro.copiedText}`.
3. `evalScript: ${console.log('PARITY_WORD=' + maestro.copiedText)}`
   emits the word to stdout in a scrapable format.
4. CI runs the flow on Android, then on iOS, within the same
   wall-clock minute. The harness greps `PARITY_WORD=...` from each
   log and diffs them. Mismatch fails the workflow.

### Comparison strategy

Demo mode uses a daily-rotation interval. As long as both runs land
within the same UTC day, the word should be identical. The CI
harness budgets a five-minute window (more than the rotation
interval would change the answer in the worst case, since `floor()`
boundaries are at midnight UTC and demo doesn't allow shorter
intervals).

If the Android run starts at 23:58 UTC and the iOS run finishes at
00:01 UTC the next day, the harness flags it as "boundary skip" and
re-runs both — not a parity failure.

### Manual fallback

When CI parity is red and you want to triage by hand:

1. Open the debug app on a real Android device, tap "Try without a
   group", note the word.
2. Within the same minute, open the iOS app on a TestFlight build,
   tap the same flow, note the word.
3. They should be identical. If they aren't, a primitive derivation
   has drifted — check the unit tests next:
   ```bash
   # Android
   ssh u5 'cd /home/ultra/code/safewords-mobile/repos/safewords-android && \
       ./gradlew :app:testDebugUnitTest'
   # iOS
   xcodebuild -scheme Safewords -destination 'platform=iOS Simulator,name=iPhone 15' test
   ```
   Both suites validate against `/shared/test-vectors.json`. A
   regression there points at the byte-level cause.

## 10. Common failures

For environment-level issues — PATH on `u5`, regex flags in Maestro
2.x, emulator boot timing, telemetry banners, the testTagsAsResourceId
bootstrap — see the **Maestro on u5** section at the bottom of
`/data/code/safewords-mobile/docs/release-pipeline-gotchas.md`. That
list is authoritative; don't duplicate it here.

A few additional flow-authoring failures we've hit that are specific
to writing the YAML itself:

### testTag on the wrong element

The `LabeledField` composable wraps a `BasicTextField` inside a
`Column`. Putting `Modifier.testTag(...)` on the outer `Column`
makes the tag visible to Maestro — but `tapOn: id:foo` then taps the
column wrapper, which doesn't focus the inner text field, and the
follow-up `inputText:` step writes nowhere. Symptom: flow looks like
it ran successfully but the text field is empty afterward.

**Fix:** put the testTag on the `BasicTextField` itself (the leaf
focusable element), not on its wrapper. Same lesson on iOS — tag the
`TextField`, not the enclosing `VStack`.

### Word display only renders after the periodic coroutine ticks

The home screen's word view subscribes to a periodic flow that emits
on a timer. The first emission lands ~0.5–1.5 seconds after the
screen mounts. A bare `assertVisible: id:home.word-display`
immediately after navigation flakes about 30% of the time — Maestro
checks before the first tick.

**Fix:** use `extendedWaitUntil` for any rotating-content element:

```yaml
- extendedWaitUntil:
    visible:
      id: 'home.word-display'
    timeout: 5000
```

Apply this rule to `plain-home.word-display`, `home.word-display`,
`plain-home.countdown`, `home.countdown-text`, and anything else that
depends on the periodic coroutine.

### Optional steps WARN but don't FAIL

`optional: true` is the right escape hatch for platform-divergent
UI — when the same flow walks both iOS and Android and a step only
applies to one. But a flow that's *supposed* to run a step every time
and accidentally has `optional: true` will **silently skip** when the
target ID is missing. The flow turns green when it should be red.

**Fix:** only mark steps optional when there's a documented platform
divergence in the registry's "Implementation status" table. For
single-platform flows, leave `optional` off so missing IDs surface
as failures.

## 11. Subagent-A spot-check

Maestro is fast, deterministic, and runs against an emulator or
simulator. None of those properties match a real Android phone in a
user's hand. Before any **production** rollout (not internal, not
TestFlight beta — production), we still need a human-driven smoke
on real Android hardware.

The minimum spot-check:

1. Install the release AAB on a real Pixel-class device.
2. Walk Plain onboarding to Advanced view.
3. Create a real group; confirm a word appears.
4. Force-quit, relaunch; confirm the same group is restored.
5. Toggle Plain mode in Settings; confirm the home screen visibly changes.
6. Try without a group → demo banner appears → exit demo lands on Onboarding.
7. Run the safety-card biometric flow (Maestro can't reach this).
8. Pin the widget; confirm it shows a word and updates.

Maestro covers steps 1–6 in the integration sense, but only on the
emulator. The widget (step 8) and biometric gate (step 7) aren't in
the accessibility tree at all. Those are why we still ship after a
human eyeball pass, not just a green CI run.

iOS equivalent: install via TestFlight on a personal device, same
checklist, plus AirDrop a card to confirm the print/share pipeline
works.

If the spot-check finds a regression Maestro didn't catch, file an
issue **and** add a flow that would have caught it (when feasible).
The flow set grows from real-world misses, not from upfront
coverage targets.
