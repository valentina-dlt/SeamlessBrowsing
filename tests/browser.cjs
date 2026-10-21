const {chromium} = require('playwright');
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs/promises');
const os = require('node:os');
(async()=>{
  const profile = await fs.mkdtemp(path.join(os.tmpdir(),'cookie-calm-'));
  const ext = path.resolve('extension');
  const context = await chromium.launchPersistentContext(profile,{headless:false,args:[`--disable-extensions-except=${ext}`,`--load-extension=${ext}`]});
  try {
    const page = await context.newPage();
    await page.route('https://fixture.test/**',route=>route.fulfill({contentType:'text/html',body:`<!doctype html><title>Cookie Calm fixture</title><input id="login" placeholder="Email"><main style="height:1800px">Main content</main><section role="dialog" aria-label="Cookie preferences" style="position:fixed;bottom:0;background:white;padding:20px"><p>We use cookies</p><button id="accept">Accept all</button><button id="reject">Reject all</button></section><script>document.querySelector('#reject').onclick=()=>{localStorage.setItem('consent','necessary');document.querySelector('section').remove()};document.querySelector('#accept').onclick=()=>localStorage.setItem('consent','all')</script>`}));
    await page.goto('https://fixture.test/');
    await page.waitForFunction(()=>localStorage.getItem('consent')==='necessary');
    assert.equal(await page.locator('[role=dialog]').count(),0);
    assert.equal(await page.locator('#login').count(),1);
    assert.equal(await page.evaluate(()=>getComputedStyle(document.body).overflow),'visible');
    const o2 = await fs.readFile(path.join(__dirname,'fixtures/o2-cookies.html'),'utf8');
    await page.route('https://o2-fixture.test/**',route=>route.fulfill({contentType:'text/html',body:o2}));
    await page.goto('https://o2-fixture.test/');
    await page.waitForFunction(()=>localStorage.getItem('consent')==='essential');
    assert.equal(await page.locator('[role=dialog]').count(),0);
    assert.equal(await page.locator('main').innerText(),'Shop content');
    console.log('O2 regression: essential-only selected, accept-all untouched');
    const shopify = await fs.readFile(path.join(__dirname,'fixtures/shopify-croftmill.html'),'utf8');
    await page.route('https://shopify-fixture.test/**',route=>route.fulfill({contentType:'text/html',body:shopify}));
    await page.goto('https://shopify-fixture.test/');
    await page.waitForFunction(()=>localStorage.getItem('consent')==='necessary');
    assert.equal(await page.locator('#shopify-pc__banner').count(),0);
    assert.equal(await page.evaluate(()=>localStorage.getItem('unrelated')),null);
    assert.equal(await page.locator('#login').count(),1);
    console.log('Shopify regression: Decline pressed, unrelated Decline untouched');
    for (const scenario of ['close', 'inline', 'password', 'entered', 'submit', 'unrelated', 'delayed', 'native', 'flyout', 'svg', 'checkbox']) {
      const tag = scenario === 'native' ? 'dialog' : 'section';
      const body = `<!doctype html><title>Newsletter fixture</title>
      <main>Shop content</main><form id="inline"><p>Subscribe to our newsletter</p><input type="email"><button type="button" aria-label="Close">×</button></form>
      <${tag} id="overlay" ${scenario === 'native' ? 'open' : ''} ${['inline','flyout','native'].includes(scenario) ? '' : 'role="dialog"'} style="${scenario === 'inline' ? '' : 'position:fixed;top:20px;left:20px;background:white;z-index:99'}${scenario === 'delayed' ? ';display:none' : ''}">
      <form><h2>${scenario === 'unrelated' ? 'Contact support' : 'Join our newsletter'}</h2><input type="email" value="${scenario === 'entered' ? 'example@example.test' : ''}">
      ${scenario === 'password' ? '<input type="password">' : scenario === 'checkbox' ? '<label><input type="checkbox">Email updates</label>' : ''}
      <button type="${scenario === 'submit' ? 'submit' : 'button'}" ${scenario === 'svg' ? '' : 'aria-label="Close dialog"'}>${scenario === 'svg' ? '<svg><title>Close</title></svg>' : '×'}</button><button type="submit">Subscribe</button></form></${tag}>
      <script>localStorage.clear();document.querySelector('#overlay button').onclick=e=>{e.preventDefault();localStorage.setItem('closed','yes');document.querySelector('#overlay').remove()};document.querySelector('#inline button').onclick=()=>localStorage.setItem('inline','clicked');document.querySelector('#overlay form').onsubmit=e=>{e.preventDefault();localStorage.setItem('submitted','yes')};${scenario === 'delayed' ? "setTimeout(()=>document.querySelector('#overlay').style.display='block',1000);" : ''}</script>`;
      await page.route('https://newsletter-fixture.test/'+scenario,route=>route.fulfill({contentType:'text/html',body}));
      await page.goto('https://newsletter-fixture.test/'+scenario);
      if (['close','delayed','native','flyout','svg','checkbox'].includes(scenario)) await page.waitForFunction(()=>localStorage.getItem('closed')==='yes');
      else { await page.waitForTimeout(2200); assert.equal(await page.evaluate(()=>localStorage.getItem('closed')),null,scenario); }
      assert.equal(await page.evaluate(()=>localStorage.getItem('inline')),null,scenario);
      assert.equal(await page.evaluate(()=>localStorage.getItem('submitted')),null,scenario);
      assert.equal(await page.locator('#inline').count(),1,scenario);
    }
    console.log('Newsletter regression: closes empty marketing overlays including delayed forms; preserves inline, login, entered, submit-only and support forms');

    await require('./providers.browser.cjs')(context);
    console.log('Browser fixture: rejected optional cookies, closed banner, preserved input and scrolling');
  } finally { await context.close(); await fs.rm(profile,{recursive:true,force:true}); }
})().catch(error=>{console.error(error);process.exitCode=1});
