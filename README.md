# Sow7Rix Studio — Clean Build

A clean-history copy of the Sow7Rix Studio Android project, including video processing with FFmpeg, TikTok Studio hand-off through a real browser, TikTok oEmbed analysis, Telegram membership gate, update check, and a Claude AI chat screen.

## Build an APK

Every push to `main` runs GitHub Actions. Open **Actions → Build APK**. After a successful build, the downloadable APK is attached to the `latest` release and as a workflow artifact.

The first build can use a temporary debug signing certificate if no private release key is configured. That APK is installable, but it cannot update an app signed with a different certificate. For a stable release, configure the protected signing secrets described below. Never commit a keystore or its password.

## Claude AI setup

The mobile app calls the Cloudflare Worker route `/ai/chat`. The Anthropic key is intentionally absent from the APK and this Git repository.

1. Create an API key in the Anthropic Console. API access and billing are separate from a Claude.ai chat subscription.
2. In the Cloudflare Worker account used by `sowz-tracket-gate`, set the secret `ANTHROPIC_API_KEY` using Wrangler's secret prompt, then deploy the Worker source in `server/telegram-gate/worker.js`.
3. The existing Worker must also have `BOT_TOKEN` configured. Do not paste either key into code, a commit, an issue, or this conversation.

The Worker uses the currently listed `claude-haiku-5-5` API model and Anthropic Messages API. See the [official API overview](https://platform.claude.com/docs/en/api/overview) and [model list](https://platform.claude.com/docs/en/about-claude/models/overview).

**Important security note:** the current login form accepts a numeric Telegram ID; that value is not cryptographic proof of who is holding the phone. The Worker rechecks channel membership, but ID-only login can be spoofed by someone who knows a member's ID. Before allowing widespread paid AI usage, add a verified Telegram sign-in/token flow and enforce rate limits at Cloudflare.

## Protected release signing

For a stable release, add these repository **Actions secrets**:

- `ANDROID_KEYSTORE_BASE64` — Base64-encoded release keystore
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `SIGNING_CERT_SHA256` — SHA-256 certificate fingerprint without colons

The signing key must be private and backed up by the project owner. The old source repository's keystore was deliberately not copied into this repository. If the previous key was exposed, do not reuse it as a trusted release key; a newly signed APK may require removing the older app first.

## Main features

- Telegram channel membership gate and settings/logout
- Video selection and FFmpeg processing presets
- TikTok Studio opened through a real external browser/Custom Tab
- Public TikTok oEmbed analysis
- Processing history and update check
- Claude AI chat via a server-side API proxy

## Build status

GitHub Actions is the source of truth for the Android compile. A successful repository commit is not itself proof of a successful APK build; use the latest workflow result and its attached APK.
