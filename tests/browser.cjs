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
    const shopify = await fs.readFile(path.join(__dirname,'fixtures/shopify-croftmill.html'),'utf8');
    await page.route('https://shopify-fixture.test/**',route=>route.fulfill({contentType:'text/html',body:shopify}));
    await page.goto('https://shopify-fixture.test/');
    await page.waitForFunction(()=>localStorage.getItem('consent')==='necessary');
    assert.equal(await page.locator('#shopify-pc__banner').count(),0);
    assert.equal(await page.evaluate(()=>localStorage.getItem('unrelated')),null);
    assert.equal(await page.locator('#login').count(),1);
    console.log('Shopify regression: Decline pressed, unrelated Decline untouched');
    console.log('Browser fixture: rejected optional cookies, closed banner, preserved input and scrolling');
  } finally { await context.close(); await fs.rm(profile,{recursive:true,force:true}); }
})().catch(error=>{console.error(error);process.exitCode=1});
