# Cookie Calm — development preview

A Chrome Manifest V3 extension for macOS plus an experimental Android Accessibility companion for Samsung. Rejects explicit optional-cookie choices while preserving necessary cookies. No telemetry, no external rule downloads, no cookie deletion, no CSS-only banner hiding.

## Installation

Desktop: download this branch as ZIP, extract it, open chrome://extensions, enable Developer mode, choose Load unpacked and select extension. Repeat in each Chrome profile and Mac. Unpacked extensions do not install automatically through Chrome Sync. The manifest includes a fixed public key to keep the extension ID stable across Macs; enabled/pause settings use Chrome storage.sync when extension sync is enabled. Website consent cookies remain separate per browser profile.

Click the extension toolbar icon to pause globally or on the current site. Reload to restart detection after its two-minute observation window. It leaves input fields alone while you type, and never explicitly focuses or scrolls controls.

Android: the GitHub workflow builds an experimental debug APK. Install on your Samsung, read the disclosure, then enable Cookie Calm under Settings → Accessibility → Installed apps. Android may require App info → Allow restricted settings for a sideloaded app. This permission reads browser UI and presses buttons; enable it only after reviewing the app. The APK requests no Internet permission. Settings are local to the phone. This is not a Chrome Android extension.

## Coverage and limits

Direct opt-out buttons in recognized cookie containers and cookie dialogs; open shadow roots and HTTP(S) frames. Limited preference editing for OneTrust and Cookiebot when every visible switch category is recognized. Unknown banners, paid choices, form controls, disabled controls and ambiguous buttons are skipped. Android only attempts explicit rejection buttons with nearby cookie context and a readable website address; private-mode indicators and focused editable fields stop processing. Some Chrome accessibility trees do not expose suitable buttons, so phone coverage must be validated on-device. The program cannot guarantee sites honour a click or prevent tracking before consent.

## Verification

Run npm install, npm test, and npx playwright install chromium; npm run test:browser loads the actual extension in an isolated fixture browser. Android: Gradle 8.11.1 and JDK 17, gradle -p android :app:assembleDebug :app:lintDebug. Java policy tests also run in CI. GitHub Actions packages the desktop extension and APK separately.

Verified 21 October 2026: six JavaScript tests, Java policy tests, the real Chromium extension fixture, Android APK assembly, and Android lint passed in [GitHub Actions run 39636563575](https://github.com/valentina-dlt/SeamlessBrowsing/actions/runs/39636563575). Download the Chrome ZIP and Samsung experimental APK from that run’s Artifacts section. Installation, real-site coverage, Chrome settings sync, and Samsung behaviour have not yet been verified on the user’s devices. This is a development preview; unknown banners may remain visible.

See RESEARCH.md for sources and design decisions.
