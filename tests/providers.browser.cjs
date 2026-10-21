const assert = require('node:assert/strict');
const cases = [
 ['OneTrust','<div id="onetrust-banner-sdk">','<button id="onetrust-reject-all-handler">'],
 ['Cookiebot','<div id="CybotCookiebotDialog">','<button id="CybotCookiebotDialogBodyLevelButtonLevelOptinDeclineAll">'],
 ['Shopify','<div id="shopify-pc__banner">','<button id="shopify-pc__banner__btn-decline">'],
 ['Didomi','<div id="didomi-notice">','<button id="didomi-notice-disagree-button">'],
 ['CookieYes','<div class="cky-consent-container">','<button data-cky-tag="reject-button">'],
 ['Complianz','<div class="cmplz-cookiebanner">','<button class="cmplz-deny">'],
 ['Iubenda','<div id="iubenda-cs-banner">','<button class="iubenda-cs-reject-btn">'],
 ['Osano','<div class="osano-cm-dialog">','<button class="osano-cm-denyAll">'],
 ['CookieScript','<div id="cookiescript_injected">','<button id="cookiescript_reject">'],
 ['Usercentrics','<div data-testid="uc-container">','<button data-testid="uc-deny-all-button">'],
 ['Tarteaucitron','<div id="tarteaucitronRoot">','<button id="tarteaucitronAllDenied2">'],
 ['Termly','<div id="termly-code-snippet-support">','<button data-tid="banner-decline">'],
 ['CookieHub','<div class="ch2-dialog">','<button class="ch2-deny-all-btn">']
];
const wrap = body => `<!doctype html><style>body{margin:20px} .popup{position:fixed;bottom:0;background:white;padding:20px}</style><main>Shop content</main>${body}`;
module.exports = async function(context) {
 const page = await context.newPage();
 const check = async (name,body,condition) => {
   const url='https://provider-fixture.test/'+name;
   await page.route(url,r=>r.fulfill({contentType:'text/html',body:wrap(body)}));
   await page.goto(url); await page.waitForFunction(condition);
   assert.equal(await page.locator('main').innerText(),'Shop content');
 };
 for(const [name,container,button] of cases) {
   // Deliberately use a label outside the English phrase matcher.
   await check(name,`${container}<p>Confidentialité</p>${button}Non merci</button><button id="accept">Oui</button></div><script>document.querySelector('button').onclick=()=>{document.body.dataset.outcome='rejected'};document.querySelector('#accept').onclick=()=>document.body.dataset.outcome='accepted'</script>`,()=>document.body.dataset.outcome==='rejected');
   console.log('Provider control passed:',name);
 }
 await check('shadow',`<div id="usercentrics-root"></div><script>const shadow=document.querySelector('#usercentrics-root').attachShadow({mode:'open'});setTimeout(()=>{shadow.innerHTML='<div data-testid="uc-container"><p>Datenschutz</p><button data-testid="uc-deny-all-button">Nein</button></div>';shadow.querySelector('button').onclick=()=>document.body.dataset.outcome='rejected'},1000)</script>`,()=>document.body.dataset.outcome==='rejected');
 await check('cookiebot-preferences',`<div id="CybotCookiebotDialog"><p>Cookies</p><label><input type="checkbox" checked disabled>Strictly necessary</label><label><input type="checkbox" id="CybotCookiebotDialogBodyLevelButtonStatistics" checked>Statistics</label><button id="CybotCookiebotDialogBodyButtonAcceptSelected">Save choices</button></div><script>document.querySelector('button').onclick=()=>{document.body.dataset.outcome=document.querySelector('#CybotCookiebotDialogBodyLevelButtonStatistics').checked?'bad':'essential';document.body.dataset.necessary=document.querySelector('input[disabled]').checked}</script>`,()=>document.body.dataset.outcome==='essential');
 assert.equal(await page.locator('body').getAttribute('data-necessary'),'true');
 await check('onetrust-two-stage',`<div id="onetrust-banner-sdk"><p>Cookies</p><button id="onetrust-pc-btn-handler">Personnaliser</button></div><div id="onetrust-pc-sdk" hidden><p>Cookies</p><label><input checked disabled type="checkbox">Strictly necessary</label><label><input class="category-switch-handler" checked type="checkbox">Analytics</label><button class="save-preference-btn-handler">Enregistrer</button></div><script>document.querySelector('#onetrust-pc-btn-handler').onclick=()=>{document.querySelector('#onetrust-banner-sdk').hidden=true;document.querySelector('#onetrust-pc-sdk').hidden=false};document.querySelector('.save-preference-btn-handler').onclick=()=>document.body.dataset.outcome=document.querySelector('.category-switch-handler').checked?'bad':'essential'</script>`,()=>document.body.dataset.outcome==='essential');
 // A known reject ID outside its provider container must not be used.
 await page.route('https://provider-fixture.test/unrelated',r=>r.fulfill({contentType:'text/html',body:wrap('<section role="dialog"><p>Account settings</p><button id="onetrust-reject-all-handler">Remove account</button></section><script>document.querySelector("button").onclick=()=>document.body.dataset.clicked="yes"</script>')}));
 await page.goto('https://provider-fixture.test/unrelated'); await page.waitForTimeout(1800);
 assert.equal(await page.locator('body').getAttribute('data-clicked'),null);
 // Restored pages must restart detection after pagehide, with no reload.
 await check('restore',`<script>setTimeout(()=>{dispatchEvent(new PageTransitionEvent('pagehide',{persisted:true}));dispatchEvent(new PageTransitionEvent('pageshow',{persisted:true}));document.body.insertAdjacentHTML('beforeend','<div role="dialog"><p>Cookies</p><button id="reject">Reject all</button></div>');document.querySelector('button').onclick=()=>document.body.dataset.outcome='rejected'},800)</script>`,()=>document.body.dataset.outcome==='rejected');
 await page.close();
 console.log('Provider matrix: 13 controls plus shadow DOM, preference flows, context safety and restored pages passed');
};
