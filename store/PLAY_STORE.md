# SpiMp3 — Google Play submission guide

Everything needed to put SpiMp3 on Google Play, in the order the Play Console asks for it.
Copy-paste text lives in `listing-en.md`; the privacy policy page is `privacy-policy.html`.

---

## 0. Before you start: the two things that will block you

**a) The 25 USD one-time registration fee.** A Google Play developer account costs $25,
paid once, forever. You need a Google account, and for a *personal* account Google also
asks you to verify a **physical phone** with Play Protect before the account is fully
activated.

**b) Closed testing is mandatory — 12 testers for 14 days.** If your developer account was
created **after 13 November 2023** (almost certainly yours), Google requires you to run a
**closed test with at least 12 opted-in testers, continuously opted in for 14 days**, before
you can request production access. This is not optional and there is no way to skip it.

Budget ~2–3 weeks: 1 day setting up, 14 days testing, then review.

> Practically: send the closed-test link to friends, family, classmates and any Discord/Telegram
> groups you're in. The testers just install the AAB from a link — they don't need a Google
> Play review, and they can't rate or review the app during closed testing.

---

## 1. Store presence

| Field | Value |
|---|---|
| App name | `SpiMp3: Offline Music Player` |
| Package name | `com.spimp3.app` |
| Default language | English (United States) |
| App or game | App |
| Category | Music & Audio |
| Tags | Music player · Offline · Audio |
| Contact email | `workspikestudio@gmail.com` |
| Contact website | your site, or the GitHub Pages privacy-policy URL |
| Privacy policy URL | see §4 |
| Contains ads | **No** |
| In-app purchases | **No** |
| App availability | Start with your own country + a few, then expand |

---

## 2. Data safety  ·  the easiest section in the whole form

SpiMp3 has no `INTERNET` permission, so nothing can leave the device. Answer:

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Data collected | *none* |
| Data shared | *none* |
| Used for tracking | **No** |
| Is all user data encrypted in transit? | N/A — nothing is transmitted |
| Do you provide a way to request data deletion? | N/A — no data is collected |
| Independent security review | No |

This is the strongest possible Data Safety result: a **"No data collected, no data shared"**
badge appears on the store listing, and it is literally verifiable.

> If you ever add a crash reporter or an "anonymous analytics" library, this section becomes
> mandatory to redo — and the badge disappears. That's the trade-off of adding them.

---

## 3. Content rating questionnaire (IARC)

Category: **Music**

| Question | Answer |
|---|---|
| Violence, blood, or scary content | No |
| Sexual or suggestive content | No |
| Profanity or crude humour | No |
| Controlled substances | No |
| Gambling / simulated gambling | No |
| Horror or fear themes | No |
| Does the app allow users to interact or share content with others? | No |
| Does the app let users access the unrestricted open internet? | **No** |
| User-generated content | No |
| Sharing a user's current location | No |
| Digital purchases | No |

Result: **Everyone**.

---

## 4. Privacy policy

Play requires a public URL that is reachable, non-`noindex`, and matches what the app does.

**Free hosting in 2 minutes — GitHub Pages:**

1. `git init` in any folder, add `privacy-policy.html` at the root, commit, push to a new
   (public) GitHub repo.
2. Repo → **Settings → Pages** → Source: *Deploy from a branch*, branch `main`, folder
   `/ (root)` → Save.
3. The URL is `https://<user>.github.io/<repo>/privacy-policy.html`.

Paste that URL into Play Console → **App content → Privacy policy**, and into
**Store settings → Contact website**.

The same text is readable **inside the app** at
*Settings → Security & privacy → Privacy policy*, which is what Google recommends and which
reviewers like to see.

---

## 5. App content  ·  remaining declarations

| Section | Answer |
|---|---|
| Ads | No |
| App access | **All functionality is available without special access** |
| Content guidelines | Nothing objectionable |
| Privacy policy | §4 |
| News app | No |
| COVID-19 contact tracing | No |
| Government app | No |
| Financial features | No |
| Health | No |
| Target audience & content | Not directed at children; content rating *Everyone* |
| News/magazine | No |

**App access** is a common stumble: if you write "requires a login" or leave it blank when
you shouldn't, a human reviewer has to install and click through the app. SpiMp3 is
fully functional immediately after the audio permission, so answer *all available*.

---

## 6. Graphics assets  ·  already generated ✅

| Asset | Requirement | File |
|---|---|---|
| App icon | 512 × 512 PNG, 32-bit | `store/play-icon.png` |
| Feature graphic | 1024 × 500 PNG/JPEG | `store/feature-graphic.png` |
| Phone screenshots | min 2, 9:16, 320–3840 px on the long edge | **you take these** |
| 7" / 10" tablet screenshots | optional, but recommended | same source images |

Play checks the icon and feature graphic for text/banners and will reject blurry ones.
Both generated files are exactly at spec.

**Screenshot requirements:** JPG or 24-bit PNG (no alpha), each side 320–3840 px,
9:16 aspect. The frame must contain the whole device screen with no status-bar or
navigation-bar cut-outs. Add a short caption inside each shot if you like — plenty of
top music players do.

Suggested order: **Home · Now Playing · Library (albums) · Search · Settings.**

---

## 7. Build  ·  already prepared ✅

| | |
|---|---|
| Upload format | **Android App Bundle (`.aab`)** — required since Aug 2021 |
| File | `out/app/outputs/bundle/release/app-release.aab` (~8.4 MB) |
| versionCode | `1` |
| versionName | `1.0.0` |
| minSdk | 26 (Android 8.0) — covers ~99% of active devices |
| targetSdk | 36 |
| Signing | real release key (`spimp3-release.p12`), validity until 2054 |

**targetSdk 36 matters.** From **31 August 2026** Google requires new apps and updates to
target **Android 16 / API 36** or higher. SpiMp3 already targets 36, so it can be published
today with no further migration. Next bump is API 37, due around August 2027.

**Back up your keystore now.** `spimp3-release.p12` and `keystore.properties` (in
`spimp3-out/`) are the only proof of app identity. If they are lost you must enrol in
Play App Signing, and Play will reject your next update until you do. Put both somewhere
durable — and *never* in a public git repo (`.gitignore` already excludes them).

---

## 8. Release

1. **Production → Create new release** → upload the `.aab`.
2. Release name: `1.0.0`.
3. **Start with a staged rollout at 20%** for the first week. It costs you nothing and it
   means a crash on some odd OEM skin is caught by a minority of users rather than everyone.
   Promote to 100% when the Android vitals dashboard stays green.
4. Save as a **draft**, fill in the release notes from `listing-en.md`, and submit for
   review.

**Review timeline:** typically a few hours to a few days for a first submission from a brand
new account. A rejection is common and usually trivial (a screenshot that is 8:19 instead of
9:16, a privacy policy URL that 404s). Read the rejection reason, fix that one thing, resubmit.

---

## 9. After it's live

- Watch **Play Console → vitals**: crash rate, ANR rate, and "users affected".
- Reply to reviews. A developer who answers gets measurably better ratings.
- Version bumps: raise `versionCode` in `app/build.gradle.kts` by 1, rebuild, upload.
- Keep the rollout staged for any risky change.

---

## 10. Common reasons for rejection (and how this app already avoids them)

| Rejection reason | Status for SpiMp3 |
|---|---|
| Privacy policy URL missing, broken or generic | ✅ real page, matches the app |
| Data safety form contradicts the manifest | ✅ manifest has no network permission |
| Misleading claims (e.g. "impossible to ever copy your files") | ✅ every claim is verifiable |
| Broken or blurry store graphics | ✅ generated at exact spec |
| Target API level too low | ✅ targets API 36 |
| App doesn't function / crashes on review | ✅ needs only the audio permission |
| Permissions not justified in the listing | ✅ each one is explained on the listing |

> The one thing to keep in mind: **don't overclaim security.** Google penalises misleading
> claims, and technically-minded users catch them. "No internet permission" is provable and
> is the strongest honest claim you can make. "Nobody can ever copy your files" is false —
> your MP3s sit in shared storage — and this app deliberately does not say it.
