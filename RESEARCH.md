# Research and architecture — 21 October 2026

- Google Chrome installation guidance: https://support.google.com/chrome/answer/2664769 . The phone flow queues an extension for desktop; standard phone Chrome does not offer desktop extension loading. Desktop installation is per profile.
- Android Accessibility service guide: https://developer.android.com/guide/topics/ui/accessibility/views/service . Services can retrieve accessible controls and perform actions. This supports a companion app without replacing Chrome, but exposed nodes and button semantics vary by site/device. A phone trial is necessary.
- Consent-O-Matic source and MIT licence: https://github.com/cavi-au/Consent-O-Matic . Existing CMP-specific rules provide broader coverage than a small new implementation. No upstream source or rules are incorporated into this preview.
- Focus interference report: https://github.com/cavi-au/Consent-O-Matic/issues/557 . User reports motivate suppressing automation while typing and avoiding focus/scroll APIs. This is not proof every current upstream version has the defect.
- Chrome storage API: https://developer.chrome.com/docs/extensions/reference/api/storage . Sync settings are extension-specific; unpacked builds need stable IDs. Sync does not install unpacked extensions or copy website cookie decisions.
- Stable extension ID discussion: https://groups.google.com/a/chromium.org/g/chromium-extensions/c/qp_087h_vrU . A fixed public manifest key is included; no private signing key is stored.
- Cookie AutoDelete source: https://github.com/Cookie-AutoDelete/Cookie-AutoDelete . Deletion serves a different purpose and may remove consent persistence, so it is not used.

Decisions: perform real explicit rejection actions; do not accept all as a fallback; do not hide banners without recording preferences; skip unknown paid choices; provide per-site pause; retain local consent cookies so decisions persist. Android receives no network permission. Avoid claiming universal banner suppression or tracker blocking. Add site-specific rules after verified reports rather than broad speculative click heuristics.
