# Safewords — Google Play Store Listing

**App ID:** `app.thc.safewords`
**Version:** 1.1.5 (versionCode 8)
**Category:** Tools (primary) · Communication (alternate)
**Pricing:** Free, no in-app purchases, no ads
**Contact email:** mail@theholding.company
**Privacy policy URL:** https://safewords.io/privacy

---

## Title (max 30 chars)

> **Safewords**

*(10 chars — recommended. Let the description do the framing. If you want a tagline, alternate: `Safewords — Verify the channel` (30, exact limit) or `Safewords: A word for trust` (27).)*

---

## Short description (max 80 chars)

> **Pre-agreed proof of identity. Survives stolen accounts, cloned voices, anything.**

*(78 chars. Threat-agnostic, audience-agnostic — leads with the abstract value, names three threat categories without picking one.)*

---

## Full description (max 4000 chars)

```
A pre-agreed word, shared between people who trust each other, that proves identity when nothing else can.

Voices can be cloned in three seconds. Email accounts get hijacked. Phone numbers get swapped to strangers. Texts arrive from "you" that you didn't send. The attacker who has all of that — the channel, the voice, the writing style, the relationship context — is one shared secret away from being unstoppable. Safewords is that shared secret.

Set one up with anyone you'd take an urgent message from. Family. A team. A partner. A friend. A source. A client. Anyone where "are you really who you say you are?" is a question that could one day matter.

WHEN IT SAVES YOU
- A voice that sounds exactly like your daughter calls in tears, asking you to wire money.
- An email from your CEO's real address asks the finance team to send a payment to a new account.
- A vendor emails new wire instructions for a real, expected invoice.
- "Your spouse" texts from their real number, but the account was just SIM-swapped.
- IT calls and asks you to confirm your password.
- A first date's friend gets a text from her saying "I'm fine" — but you agreed she'd send a code word if she actually was.
- Anyone you trust, contacting you through any channel, asking for something that matters.

The pattern is the same. The channel can be compromised. The voice can be cloned. The face on the video call can be fake. The context can be researched. But the word — agreed in person, never written down, never sent over any wire — can't be.

HOW IT WORKS
- Create a group. Name it whatever fits — "Family", "Engineering", "Mike & I".
- Show the QR code. The other person scans it.
- Now both of you see the same word, at the same time, that rotates every hour, day, week, or month — your choice.
- When something feels off, ask. If they can't say today's word, you've caught them.

ZERO SERVER, ZERO ACCOUNT
- No sign-up. No email. No password.
- No analytics. No advertising. No tracking.
- The app makes zero network requests. Period.
- Seeds are stored encrypted on each device, never uploaded.
- If we shut down tomorrow, your app keeps working forever.

DESIGNED FOR EVERYONE
- High Visibility mode: enormous text, plain language, AAA contrast — for older users and stressful moments.
- Single-use word generator: a one-off random word with no group, for any conversation.
- Emergency override word: a permanent fallback that always works.
- Lock-screen widget: today's word at a glance.
- Biometric unlock: optional.
- Drill mode: practice catching a fake call before it's real.

YOUR DATA, YOUR DEVICE
- Seeds live in Android's encrypted storage (hardware-backed where available).
- We don't see them. We can't see them. There is no server.
- Uninstall = total wipe.

RECOMMENDED BY
The FBI Internet Crime Complaint Center, the FTC Consumer Alerts, and AARP Fraud Watch all recommend that families and organizations agree on a private verification word with the people they trust.

OPEN BY DESIGN
- Same algorithm runs on iOS and Android: HMAC-SHA256 of seed × time bucket.
- Words chosen for clarity over phone audio: "Breezy Rocket 75" travels better than "Quixotic Cardamom 03".
- Anyone can verify a phrase deterministically. No magic. No back doors.

WHEN SOMETHING FEELS OFF
1. Don't react. Don't decide. Don't move money.
2. Ask: "What's our word?"
3. If they can't say it, end the conversation. Reach them through a channel you trust.
4. Real emergencies survive a 60-second pause. Scams don't.

Account compromise plus impersonation is the attack profile every smart adversary will eventually run. Safewords is the only defense that survives both — because the proof you're asking for was never on a wire they could intercept, never in an account they could hijack, never on a recording they could clone.
```

*(≈ 3,200 chars — well under 4,000.)*

---

## What's new (release notes — max 500 chars)

```
First public release. Pre-agreed proof of identity for any channel that can be impersonated, hijacked, or cloned. Set one up with the people you trust — family, team, friends, anyone.

- Rotating word every hour / day / week / month
- QR-based group invites
- Single-use word generator
- Emergency override word
- High Visibility (accessibility) mode
- Biometric unlock, lock-screen widget, drill mode
- Zero network, zero account, zero analytics
```

---

## Categorization & content rating answers

- **Primary category:** Tools
- **Tags:** privacy, family, security, accessibility, communication
- **Target age group:** All ages (no PEGI/ESRB-restricted content)
- **Contains ads:** No
- **In-app purchases:** No
- **Data safety form** *(IARC questions)*:
  - Does the app collect or share user data? **No**
  - Is all user data encrypted in transit? **N/A — no data leaves the device**
  - Do you provide a way for users to request data deletion? **Yes — uninstall the app**
  - Personal info collected: **none**
  - Financial info: **none**
  - Health/fitness: **none**
  - Messages: **none** *(SMS invite uses the user's own SMS app)*
  - Photos/videos: **none** *(camera is QR-only, never stored)*
  - Audio files: **none**
  - Files/docs: **none**
  - Calendar/contacts: **none**
  - App activity, web history, device IDs: **none**
- **Permissions declared:**
  - `android.permission.CAMERA` — QR scanning only, foreground only
  - `android.permission.USE_BIOMETRIC` — optional app-open lock
  - *(no INTERNET permission requested — app is offline-only)*

---

## Content rating questionnaire (IARC)

All answers: **No.**
- Violence: No
- Sexual content: No
- Profanity: No
- Drugs/alcohol: No
- Gambling: No
- User-generated content: No (group names are local-only, never shared with us)
- Location sharing: No
- Personal info sharing: No
- Digital purchases: No

Expected rating: **Everyone**.

---

## Store assets needed (you upload these)

- App icon: 512×512 PNG (regenerate from `output/icon/safewords-icon-foreground.svg` + composite background — see `output/icon/`)
- Feature graphic: 1024×500 PNG (TODO — not yet generated)
- Phone screenshots: minimum 2, max 8, 16:9 or 9:16, ≥320 px short side (see `output/play-store/v2/screenshots/`)
- *(Optional)* tablet screenshots: 7" + 10"
- *(Optional)* short promo video on YouTube

---

## Pre-launch checklist

- [ ] Privacy policy hosted at a stable public URL (currently `http://10.10.10.85:9876/privacy.html` — internal IP, swap before submitting)
- [ ] App icon 512×512 PNG generated
- [ ] Feature graphic 1024×500 PNG generated
- [ ] At least 2 phone screenshots ≥ 320 px short side
- [ ] Signed AAB at `output/play-store/v2/safewords-1.1.0.aab` *(rebuild with versionCode 7 / 1.1.4 before upload)*
- [ ] Content rating questionnaire submitted
- [ ] Data safety form submitted
- [ ] Internal testing track tester emails added (your account is fine for solo)
- [ ] App Content section: target age, ads declaration, news app status, COVID-19, government app — all answered
- [ ] Listing language: English (United States)
