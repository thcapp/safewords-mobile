# Safewords Best-in-Class Expansion Proposal

Date: 2026-04-30
Status: Draft for review

## Review Ask

Decide which of these directions should become the next funded product pass after the current iOS/TestFlight and Android/Play Store shipping work:

- Make Safewords the fastest stressed-call verification app.
- Add printable Safety Cards to mobile.
- Extend the same verification primitive to agentic teams.
- Preserve the strongest product position: offline-first, no account, no tracking, no contact upload.

This proposal intentionally separates consumer safety features from agentic-team features. They can share cryptographic primitives and schemas, but they should not be forced into one UI.

## Executive Summary

The competitive screenshots point to three existing approaches:

- `safe_word`: contact-network verification with practice calls and server-side matching.
- `Supercode`: polished AI-scam positioning, invite links/QR, configurable code styles, and education.
- `Codeword`: stronger security model with directional/asymmetric code exchange, QR/link setup, and explicit "do not share" framing.

Safewords should not copy all of them. The best wedge is narrower:

> Safewords should be the clearest, fastest, most private way for a normal person to verify a trusted caller under stress.

The major differentiators should be:

- A first-class "Someone is calling me" mode.
- Senior/panic-friendly Plain Mode.
- Family-circle setup that does not require contacts upload or accounts.
- Strong recovery and backup.
- Printable cards that move the protocol into the physical household.
- Optional agentic-team verification surfaces that reuse the same rotating proof concept for high-impact automation.

## Product Principles

1. Fast under stress beats feature richness.
2. The user should never need to understand cryptography to use the product correctly.
3. Privacy claims must stay simple and true: no account, no tracking, no contact upload, no cloud matching.
4. Physical-world behavior matters. A card on a fridge can be more useful than a hidden settings screen.
5. Agents can verify authority, but should not be treated as identity-proof subjects.
6. Sensitive exports must be visibly dangerous and biometric-gated where possible.

## Competitive Takeaways

### safe_word

Strengths:

- Practice verification is valuable.
- Simple contact list makes the product feel familiar.
- Permission and Face ID flows imply seriousness.

Weaknesses:

- Contacts upload is a major privacy/trust cost.
- Server-side matching undermines an offline-first story.
- The user is asked for permissions before enough value is visible.
- The flow feels contact-centric rather than crisis-centric.

Safewords response:

- Build drills and practice, but keep them local.
- Avoid contacts upload entirely.
- Lead with "what to do during a suspicious call," not with a network graph.

### Supercode

Strengths:

- Clear "Stop AI scams" positioning.
- Good invite mechanics: link, QR, share.
- Educates users about when to use the code.
- Code-style choice creates ownership and delight.

Weaknesses:

- Cloud/link framing can feel less private.
- Code style choice may distract from emergency use.
- It is more "setup a code" than "survive this call right now."

Safewords response:

- Keep education concise and embedded in the action flow.
- Consider visual/personality options later, but do not make them core.
- Make the emergency verification flow the home-screen default.

### Codeword

Strengths:

- Strong security mental model: "they say X, you say Y."
- Explicit "do not share this" framing.
- QR/link setup is clear.
- Expiring links reduce long-lived exposure.

Weaknesses:

- More cognitive load.
- Pairwise directional codes are harder for families and seniors.
- Better for high-trust professional contacts than simple family use.

Safewords response:

- Keep family circles simple.
- Consider directional/asymmetric verification later for agentic teams or advanced groups.
- Borrow the explicit secret-handling language.

## Concept 1: Someone Is Calling Me Mode

### Problem

The moment of highest value is not group setup. It is the 20 seconds after a user receives a suspicious call, text, or video chat.

Current mobile UX still behaves like a general app. The next best-in-class pass should make Safewords feel like an emergency decision tool.

### Proposed Flow

Entry points:

- Home primary CTA: "Someone is calling me"
- Plain Mode primary CTA: same label, larger
- Lock-screen/widget later: direct launch into this flow

Flow:

1. Show active group and current word in large text.
2. Instruction: "Ask them: What is our word?"
3. Warning: "Do not read the word to them."
4. Two huge buttons: "They said it" and "They did not."
5. Match path: "Safe to continue, but still be careful."
6. No-match path: "Hang up. Call them back on a known number."
7. Optional: "Run a quick drill later" prompt after the event.

### UX Requirements

- Time from app launch to readable word: under 3 seconds.
- One-handed use.
- Works in Plain Mode.
- No keyboard required.
- No account, network, contacts, or permissions required.

### Implementation Notes

- Reuse existing TOTP derivation and selected group.
- Make this a dedicated screen, not just a variant of `VerifyView`.
- Use the same copy on Android and iOS.
- Add analytics only if privacy posture changes; default should be no telemetry.

## Concept 2: Printable Safety Cards on Mobile

### Problem

The web app already supports printable protocol cards, but mobile does not. Physical cards are important because Safewords is a household safety behavior, not only an app behavior.

Printed cards also create a defensible differentiator against competitor apps: Safewords bridges digital verification and physical family preparedness.

### Web Baseline

The web implementation has four templates:

- Full Page: complete protocol reference.
- Wallet Cards: 8 compact cards per page.
- Fridge Poster: large household reminder.
- Emergency Cards: handouts for neighbors, caregivers, or extended family.

Mobile should stay compatible with these templates conceptually, but the mobile product must distinguish safe instruction cards from sensitive recovery cards.

### Proposed Mobile Feature: Safety Cards

Entry points:

- Settings -> Safety Cards
- Group detail -> Safety Cards
- Post-onboarding CTA: "Print backup cards"

Templates:

- Wallet Card
- Fridge Poster
- Caregiver Card
- Full Protocol
- Recovery Card, sensitive
- Agent Action Card, future

Output actions:

- Print
- Save PDF
- Share PDF
- AirDrop, iOS
- Android share sheet

### Safe To Print Often

These cards should not contain the rotating seed or anything that can recreate group membership.

Allowed content:

- Group name
- Member names
- When to ask for the word
- What to do if they fail
- Emergency guidance
- Optional static emergency override word, if the user explicitly enabled one
- safewords.io/app install QR

Example copy:

```text
ASK FOR THE SAFEWORD

If someone asks for money, account access, travel help, gift cards, or a secret:

1. Ask: "What is our Safewords word?"
2. Do not tell them the word first.
3. If they cannot answer, hang up.
4. Call back on a number you already know.
```

### Sensitive Print

These cards can recreate group access and should be treated like backup keys.

Sensitive content:

- Recovery phrase
- Import QR containing the group seed
- Any static fallback word if it is used as a real authenticator

Required gates:

- Biometric or device-passcode unlock.
- Confirmation screen explaining risk.
- Plain-language warning: "Anyone with this card can join or restore this group."
- Optional "print only once" reminder, but do not rely on it for security.

### Mobile Technical Approach

iOS:

- Generate PDF with `UIGraphicsPDFRenderer`.
- Present via `UIPrintInteractionController` and share sheet.
- Keep rendering local; no webview or upload.

Android:

- Generate PDF with `PrintedPdfDocument` or Compose-to-bitmap/PDF pipeline.
- Use `PrintManager` and share sheet.
- Keep rendering local.

Shared:

- Add `/shared/safety-card-schema.md`.
- Add static template copy constants or shared JSON fixtures.
- Keep template names and dimensions compatible with web.

### Non-Goals

- Do not add cloud print services.
- Do not upload PDFs for rendering.
- Do not auto-include recovery phrases in default cards.
- Do not encourage users to photograph sensitive cards.

## Concept 3: Agentic Team Safewords

### Problem

Agentic systems introduce a different version of the same trust problem:

- Was this instruction actually authorized?
- Is this request fresh or copied from old context?
- Should this agent be allowed to deploy, spend, email, delete, or publish?
- Did prompt injection trick the agent into bypassing normal process?

Safewords can become a lightweight "shared intent verification" layer for high-impact actions.

### Product Thesis

For humans:

> Verify identity during suspicious communication.

For agentic teams:

> Verify authority before an agent takes consequential action.

This is not full identity, secrets management, or policy orchestration. It is a small, auditable freshness check before dangerous automation.

### Core Objects

- Circle: trust domain, such as `family`, `prod-deploy`, `billing`, `customer-data`.
- Seed: shared secret for that circle.
- Safeword: short rotating proof derived from seed and time window.
- Action: gated operation, such as deploy, delete, publish, email, purchase.
- Policy: maps actions to required circles and freshness windows.
- Audit event: records action, actor, verifier, time window, and result without storing the safeword.

### Example CLI

```bash
safewords init circle prod-deploy
safewords current prod-deploy
safewords verify prod-deploy amber-river
safewords gate prod-deploy --action deploy-production -- ./deploy.sh
safewords audit list
```

### Example GitHub Actions Gate

```yaml
- uses: safewords/verify-action@v1
  with:
    circle: prod-deploy
    action: app-store-release
    word: ${{ inputs.safeword }}
```

### Dogfood Target

Use agentic Safewords internally before exposing it publicly:

- Require an Agent Circle safeword before Play production promotion.
- Require an Agent Circle safeword before TestFlight/App Store release lanes.
- Require an Agent Circle safeword before destructive Based MCP deploy actions.

### Security Model

Best architecture:

- Humans/mobile devices can reveal current words.
- Agents and CI can verify words for specific circles.
- High-risk agents should not be able to reveal the word if avoidable.
- Audit logs record verification outcome, not secrets.

Important limitation:

If an agent has the seed, it can generate the word. This feature is strongest when the requester needs to obtain the current word from a separate trusted human/device/channel.

### Future Advanced Mode

Directional/asymmetric code exchange from the `Codeword` competitor may fit agentic workflows better than family workflows:

- Human says one word.
- Agent expects a different response.
- The pair changes per time window and per action.

This is more complex and should be deferred until the basic gate is dogfooded.

## Concept 4: Recovery and Backup as a Trust Pillar

The recovery schema already recommends BIP39 24-word encoding for the 32-byte group seed. This should remain a top priority because a family safety tool fails badly if users lose access to the shared seed.

Next steps:

- Implement shared recovery vectors on iOS and Android.
- Replace placeholder recovery entry flows.
- Add biometric-gated "Back up recovery phrase."
- Add "Confirm you saved it" during group creation.
- Consider a printable Recovery Card as part of Safety Cards, but make it explicitly sensitive.

## Concept 5: Practice Drills as Behavior Training

Competitors show that practice is valuable, but Safewords can do it with less account/network surface.

Recommended drills:

- "Grandparent emergency money call"
- "Boss asks for payroll change"
- "Child says they lost their phone"
- "Bank calls asking for a code"
- "Agent asks to deploy production"

Drill principles:

- Local only.
- Short, realistic scenarios.
- End with one memorable rule.
- Offer replay, not gamification-heavy scoring.

## Proposed Release Sequencing

### v1.2: Recovery Contract Implementation

Goal:

Make backup/restore reliable and identical across platforms.

Scope:

- Implement BIP39 recovery services on iOS and Android.
- Consume `/shared/recovery-vectors.json` in tests.
- Add backup/export flow behind biometrics.
- Add recovery import flow during onboarding and settings.

Why first:

Recovery is foundational. Safety Cards and agentic teams both depend on clear handling of sensitive exports.

### v1.3: Best-in-Class Call Flow and Safety Cards

Goal:

Make Safewords clearly better for real household use.

Scope:

- "Someone is calling me" mode.
- Plain Mode parity for the call flow.
- Safety Cards screen.
- Safe-to-print cards on both platforms.
- Sensitive Recovery Card behind biometric gate.
- Shared safety-card schema and copy.

Why second:

This is the product differentiation pass. It addresses competitor gaps and makes the app useful outside the phone.

### v1.4: Agent Circle Prototype

Goal:

Dogfood Safewords for agentic release approvals.

Scope:

- `/shared/agent-circle-schema.md`
- Minimal CLI prototype.
- GitHub Actions manual gate.
- Local audit log format.
- Mobile "Agent Circle" developer-mode display.

Why third:

This is promising, but it is a new market surface. It should prove itself internally before it complicates the consumer app.

### v1.5: Agentic Team Productization

Goal:

Decide whether Agent Circle becomes a public feature, a separate product, or an internal tool.

Scope:

- Better policies.
- Signed audit logs.
- Multi-circle action mapping.
- Integrations for Based MCP, GitHub Actions, fastlane, deploy scripts.
- Possible directional/asymmetric verification.

## Shared Schema Work

Recommended new shared specs:

- `/shared/safety-card-schema.md`
- `/shared/safety-card-copy.json`
- `/shared/agent-circle-schema.md`
- `/shared/agent-action-vectors.json`

Safety card JSON sketch:

```json
{
  "version": 1,
  "template": "wallet",
  "sensitivity": "instructional",
  "groupName": "Family",
  "members": ["Mom", "Dad", "Nana"],
  "rules": [
    "Ask for the safeword",
    "Do not say the word first",
    "Hang up and call back on a known number"
  ],
  "qr": {
    "kind": "install",
    "value": "https://safewords.io/app"
  }
}
```

Agent policy JSON sketch:

```json
{
  "version": 1,
  "circle": "prod-deploy",
  "actions": [
    {
      "name": "deploy-production",
      "windowSeconds": 300,
      "requiresHumanReveal": true,
      "audit": true
    }
  ]
}
```

Audit event JSON sketch:

```json
{
  "version": 1,
  "timestamp": "2026-04-30T14:00:00Z",
  "circle": "prod-deploy",
  "action": "app-store-release",
  "actor": "github-actions",
  "verifier": "safewords-cli",
  "timeStep": 29625840,
  "result": "accepted"
}
```

## Success Metrics

Consumer:

- New user can create a group and see the current word in under 60 seconds.
- Returning user can reach call mode in under 3 seconds.
- Plain Mode user can verify a call without reading dense text.
- Recovery restore succeeds against shared vectors on both platforms.
- Safety Card PDF can be printed and scanned without network access.

Agentic:

- CI gate can verify a current word without storing the submitted word.
- Release workflow can be blocked by missing/expired safeword.
- Audit log captures action outcome without leaking secrets.
- Internal team finds it useful enough to keep in the release flow for two weeks.

## Risks and Mitigations

Risk: Printed cards leak secrets.

Mitigation: Split instructional cards from sensitive recovery/import cards. Default to instructional. Gate sensitive cards.

Risk: Agentic positioning confuses the family app.

Mitigation: Keep Agent Circle behind developer mode or a separate CLI/product surface.

Risk: Too many features before store launch.

Mitigation: Ship current mobile release first. Treat this as post-launch roadmap.

Risk: Users think Safewords proves identity absolutely.

Mitigation: Copy should always say "verify before you trust," not "guaranteed identity." No-match guidance should be conservative.

Risk: Agents with seed access can self-authorize.

Mitigation: Prefer human reveal / verifier-only patterns and document the limitation plainly.

## Open Questions

- Should mobile Safety Cards be implemented before or after BIP39 recovery is live?
- Should the emergency override word exist as a separate printed-card concept, or does that create too much static-secret risk?
- Should Agent Circle be a public Safewords feature or a separate developer tool?
- Should agentic verification use the same word format as family groups, or a more machine-friendly code?
- Should printed import QR codes be time-limited, or is that incompatible with offline-first recovery?
- Should web and mobile share a single card-rendering schema, or should each platform render natively from shared copy?

## Recommendation

Approve the following sequence:

1. Finish shipping current Android/iOS pipelines.
2. Implement v1.2 recovery contract across both platforms.
3. Build v1.3 "Someone is calling me" mode plus non-sensitive Safety Cards.
4. Add sensitive Recovery Cards only after backup/restore is well tested.
5. Prototype Agent Circle internally as a CLI/GitHub Actions gate before exposing it in the mobile UI.

This keeps Safewords focused while still expanding into the two most promising differentiators: physical household readiness and agentic action verification.
