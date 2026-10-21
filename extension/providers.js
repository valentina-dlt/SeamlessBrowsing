/* Selector data adapted from duckduckgo/autoconsent, MPL-2.0.
 * Source: https://github.com/duckduckgo/autoconsent
 * License: licenses/autoconsent-MPL-2.0.txt. See THIRD_PARTY.md. */
/* Provider control selectors reviewed against upstream source; see THIRD_PARTY.md. */
(() => {
  const rules = [
    {id:'OneTrust', box:'#onetrust-banner-sdk,#onetrust-pc-sdk', reject:'#onetrust-reject-all-handler,.ot-pc-refuse-all-handler', manage:'#onetrust-pc-btn-handler', panel:'#onetrust-pc-sdk', auxiliary:'#ot-pc-lst', optional:'input.category-switch-handler:not(:disabled)', save:'.save-preference-btn-handler'},
    {id:'Cookiebot', box:'#CybotCookiebotDialog', reject:'#CybotCookiebotDialogBodyButtonDecline,#CybotCookiebotDialogBodyLevelButtonLevelOptinDeclineAll', manage:'#CybotCookiebotDialogBodyLevelButtonCustomize,#CybotCookiebotDialogBodyButtonDetails', panel:'#CybotCookiebotDialog', optional:'#CybotCookiebotDialogBodyLevelButtonPreferences,#CybotCookiebotDialogBodyLevelButtonStatistics,#CybotCookiebotDialogBodyLevelButtonMarketing', save:'#CybotCookiebotDialogBodyLevelButtonLevelOptinAllowallSelection,#CybotCookiebotDialogBodyButtonAcceptSelected'},
    {id:'Shopify', box:'#shopify-pc__banner', reject:'#shopify-pc__banner__btn-decline'},
    {id:'Didomi', box:'#didomi-notice,#didomi-popup,.didomi-popup-notice', reject:'#didomi-notice-disagree-button,.didomi-continue-without-agreeing'},
    {id:'CookieYes', box:'.cky-consent-container,.cky-modal', reject:'[data-cky-tag="reject-button"]', manage:'[data-cky-tag="settings-button"]', panel:'.cky-modal', optional:'.cky-modal-open input[type=checkbox]:not([disabled])', save:'[data-cky-tag="detail-save-button"]'},
    {id:'Complianz', box:'.cmplz-cookiebanner', reject:'.cmplz-deny'},
    {id:'Iubenda', box:'#iubenda-cs-banner', reject:'.iubenda-cs-reject-btn'},
    {id:'Osano', box:'.osano-cm-dialog', reject:'.osano-cm-denyAll,.osano-cm-deny'},
    {id:'CookieScript', box:'#cookiescript_injected,.cookiescript_fsd_main', reject:'#cookiescript_reject', manage:'#cookiescript_manage'},
    {id:'Usercentrics', box:'#usercentrics-button,[data-testid="uc-container"],#uc-main-dialog', reject:'#uc-btn-deny-banner,[data-testid="uc-deny-all-button"],[data-action-type="deny"]'},
    {id:'Tarteaucitron', box:'#tarteaucitronRoot', reject:'#tarteaucitronAllDenied2,#tarteaucitronAllDenied'},
    {id:'Termly', box:'#termly-code-snippet-support,.t-preference-modal', reject:'[data-tid="banner-decline"],.t-declineAllButton', manage:'.t-preference-button,[data-testid="preferences-link"]'},
    {id:'CookieHub', box:'.ch2-dialog,.ch2-settings', reject:'.ch2-deny-all-btn', manage:'.ch2-open-settings-btn'}
  ];
  const api = {rules, find(root) {
    const found = [];
    for (const rule of rules) for (const box of root.querySelectorAll(rule.box)) found.push({box,rule});
    return found;
  }};
  if (typeof module !== 'undefined') module.exports = api;
  else Object.defineProperty(globalThis,'CookieCalmProviders',{value:api});
})();
