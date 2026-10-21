# Cookie Calm: research and coverage audit

Reviewed 21 October 2026. This supersedes the initial narrow research notes.

## Main finding

A short phrase allowlist is inadequate as the main consent engine. Sites use provider IDs, localized labels, settings panels, checkboxes, shadow roots, iframes, timed banners, and page restoration. Croft Mill and O2 exposed independent detection and wording defects. Passing one artificial banner did not establish useful website coverage.

## Established implementations examined

Consent-O-Matic documents more than 200 CMP/site rules and separates detection, opening options, category changes and saving: https://github.com/cavi-au/Consent-O-Matic . Its source includes preference-state handling that a word matcher lacks. A focus-interference report informed our typing guard: https://github.com/cavi-au/Consent-O-Matic/issues/557 . Reports do not establish the current state of every release.

DuckDuckGo Autoconsent is used in DuckDuckGo browser apps and has declarative and dynamic CMP rules, frame context, optOut flows, optional self-tests and separate cosmetic handling: https://github.com/duckduckgo/autoconsent/blob/main/readme.md ; https://github.com/duckduckgo/autoconsent/blob/main/docs/api.md ; https://github.com/duckduckgo/autoconsent/blob/main/lib/rules.ts . We examined its configuration, message API, DOM actions, and major provider rules. Some rules fall back to hiding or page API calls; importing every action would change the behaviour promised by Cookie Calm. We reuse reviewed provider selector data, with attribution, rather than claim the entire upstream engine or its coverage.

## Provider evidence and implemented scope

| Provider | Upstream evidence | Cookie Calm 0.3.0 implementation |
|---|---|---|
| OneTrust | lib/cmps/onetrust.ts | Reject controls; settings → optional switches → save |
| Cookiebot | lib/cmps/cookiebot.ts; Consent-O-Matic rules/cookiebot.json | Decline controls; known category switches and save |
| Shopify | rules/autoconsent/shopify.json | Native decline button inside native banner |
| Didomi | rules/autoconsent/didomi.json | Explicit disagree/continue-without-agreeing control |
| CookieYes | rules/autoconsent/cookieyes.json | Reject control; settings and classified category switches |
| Complianz | rules/autoconsent/complianz-banner.json | Deny control inside cookie banner |
| Iubenda | rules/autoconsent/iubenda.json | Explicit reject control |
| Osano | rules/autoconsent/osano.json | Deny/deny-all control |
| CookieScript | rules/autoconsent/cookie-script.json | Reject control; manage to rejection panel |
| Usercentrics | rules/autoconsent/usercentrics-api.json; usercentrics-button.json | DOM deny controls, including open shadow roots |
| Tarteaucitron | rules/autoconsent/tarteaucitron-deny.json | Deny-all control |
| Termly | rules/autoconsent/termly.json | Decline control; preference panel decline-all |
| CookieHub | rules/autoconsent/cookiehub.json | Settings → deny-all control |

Paths above refer to https://github.com/duckduckgo/autoconsent/blob/main/ unless otherwise specified. These are provider adapter fixtures, not proof that all configurations or websites using a provider work. Missing controls, unsupported panels and unknown category states remain visible.

Official corroboration: OneTrust reject API https://developer.onetrust.com/onetrust/docs/javascript-api and control styling https://developer.onetrust.com/onetrust/docs/common-custom-css ; Cookiebot decline callback https://www.cookiebot.com/us/developer/ and banner options https://support.cookiebot.com/hc/en-us/articles/4401873267090 ; Didomi notice configuration https://developers.didomi.io/cmp/web-sdk/consent-notice/notice/look-and-feel ; Shopify privacy API https://shopify.dev/docs/api/customer-privacy . Didomi documents that legitimate-interest handling can depend on site configuration. Pressing a rejection button does not prove every site honours every requested purpose.

## Newsletter research

Klaviyo supports delayed, scroll, exit-intent and custom-trigger forms; its targeting and reappearance depend on cookies and Shopify privacy settings: https://help.klaviyo.com/hc/en-us/articles/4413544555547 ; https://help.klaviyo.com/hc/en-us/articles/115005249348 ; https://developers.klaviyo.com/en/docs/how_to_custom_trigger_a_popup_or_flyout_form . Forms can be inline, modal or flyout; dismissal should use the actual close control: https://help.klaviyo.com/hc/en-us/articles/4405381628827 . Mailchimp similarly distinguishes embedded and popup signup forms: https://mailchimp.com/help/about-signup-form-options/ . Cookie Calm checks overlay context plus marketing content and an explicit close control; it does not submit signup forms. Native dialog, fixed flyout, SVG-labelled close and ordinary checkbox variants belong in the fixture matrix. Croft Mill loads Klaviyo, but its exact live email popup has not reappeared in our inspection.

## Browser engineering

Chrome content scripts run in an isolated world; HTTP frames and inherited-origin frames have distinct matching rules: https://developer.chrome.com/docs/extensions/develop/concepts/content-scripts . Current matching remains HTTP(S); closed shadow trees and nonmatching frames are not claimed supported. Open shadow mutations now receive observers, including after initial page load.

Back/forward cache preserves the page, so stopping on pagehide requires restarting on persisted pageshow: https://developer.mozilla.org/en-US/docs/Web/API/Window/pagehide_event . Added that lifecycle handling. Settings remain synced per extension ID using https://developer.chrome.com/docs/extensions/reference/api/storage ; unpacked installations still require a manual reload after code changes.

## Validation and limits

The expanded browser matrix covers 13 provider control patterns using nonmatching French labels, provider context boundaries, shadow DOM, multistage settings, essential preservation, restored pages, Croft Mill, O2, and newsletter safeguards. Fixture pass results establish behaviour on those fixtures only. They are not a measured web-wide success rate. Android remains a separate Accessibility implementation with narrower coverage; desktop provider selectors do not transfer to Android. No claim of universal popup suppression or tracker blocking is justified.
