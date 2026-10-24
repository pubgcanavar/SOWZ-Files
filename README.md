# Sow7Rix Studio

An original dark/neon video workspace built around the SOWZ fox mark. It includes a rebuilt dashboard, animated five-slot navigation, TikTok browser-session screen, FFmpeg presets, render history, video-link analysis and a Claude assistant.

## Build
Pushes to `main` run GitHub Actions and publish an APK plus a source-and-APK ZIP. APKs use a temporary debug signature unless protected release-signing secrets are configured. Never commit signing keys, passwords or API keys.

## TikTok browser session
TikTok login opens through AndroidX Custom Tabs: a browser sheet appears above the app and uses browser session cookies. SOWZ never reads or stores a TikTok password. After signing in, reopen Studio; TikTok may reuse that session. Do not collect identity-provider credentials inside a WebView.

This browser-session flow does **not** expose TikTok follower/likes/profile data automatically. Real metrics require TikTok Login Kit, a registered client key and redirect URI, user consent, approved scopes such as `user.info.stats` and `video.list`, plus secure server-side OAuth token handling. Until configured, the app shows actual local render counts rather than fabricated TikTok metrics.

## Render modes
- Original: copies source without encoding.
- Fast Render: faster x264 preset; output may be larger depending on source footage.
- High Quality: CRF-based encoding; source dimensions and FPS stay unchanged unless a target preset is selected.
- 720p/60, 1080p/60 and 1080p/60 Max profiles.
- Outputs save to `Movies/Sow7Rix`; temporary files are cleaned after completion or failure.

Render speed depends on source resolution, duration, codec and device hardware.
