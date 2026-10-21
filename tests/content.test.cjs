const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const P = require('../extension/policy.js');
const source = fs.readFileSync(require('node:path').join(__dirname, '../extension/content.js'), 'utf8');

// Exercise the actual content script with controlled DOM boundaries and Chrome storage.
async function fixture({text = 'We use cookies', label = 'Reject all', paused = false, hidden = false, form = false, typing = false, disabled = false, known = false, shopify = false, buttonId = ""} = {}) {
  let clicks = 0;
  const button = {id: buttonId, innerText: label, isConnected: true, disabled,
    getAttribute: () => null, closest: selector => selector === 'form' && form ? {} : null,
    getBoundingClientRect: () => ({width:100,height:30,top:20,left:20,bottom:50,right:120}),
    click: () => { clicks++; }};
  const box = {...button, innerText: text, querySelectorAll: () => [button], querySelector: () => null,
    matches: selector => (known && selector.includes('#onetrust-banner-sdk')) || (shopify && selector === '#shopify-pc__banner')};
  let listener;
  const context = {
    CookieCalmPolicy: P, CookieCalmNewsletter: {find:()=>[]}, location: {hostname:'example.com'}, innerHeight:1000, innerWidth:1000,
    getComputedStyle: () => ({display:hidden ? 'none':'block',visibility:'visible',opacity:'1'}),
    document: {hidden:false, activeElement:typing ? {matches:()=>true}:null, documentElement:{}, querySelectorAll: selector => selector === '*' ? []:[box]},
    MutationObserver: class {observe(){} disconnect(){}},
    chrome: {storage:{sync:{get:async()=>({enabled:true,pausedSites:paused ? ['example.com']:[]})},onChanged:{addListener:fn=>listener=fn}}},
    setTimeout:()=>1, clearTimeout(){}, setInterval:()=>1, clearInterval(){}, addEventListener(){}
  };
  vm.runInNewContext(source, context);
  await Promise.resolve(); await Promise.resolve();
  return {clicks, listener};
}
test('actual script presses a visible explicit rejection in a cookie dialog', async()=>assert.equal((await fixture()).clicks,1));
test('actual script leaves unrelated and risky controls untouched', async()=>{
  for (const options of [{text:'Invitations'}, {label:'Accept all'}, {label:'Close'}, {label:'Reject'}, {text:'Subscribe to continue without tracking cookies'}, {form:true}, {typing:true}, {disabled:true}, {hidden:true}, {paused:true}]) assert.equal((await fixture(options)).clicks,0,JSON.stringify(options));
});

test('Shopify Decline is eligible only inside its verified banner', async()=>{
  assert.equal((await fixture({shopify:true,buttonId:'shopify-pc__banner__btn-decline',label:'Decline'})).clicks,1);
  for (const options of [
    {label:'Decline'},
    {shopify:true,label:'Decline'},
    {buttonId:'shopify-pc__banner__btn-decline',label:'Decline'},
    {shopify:true,buttonId:'shopify-pc__banner__btn-decline',label:'Accept'},
    {shopify:true,buttonId:'shopify-pc__banner__btn-decline',label:'Decline',paused:true}
  ]) assert.equal((await fixture(options)).clicks,0,JSON.stringify(options));
});

test('O2 essential-only wording is handled by the actual content script', async()=>{
  assert.equal((await fixture({label:'Accept essential cookies only'})).clicks,1);
  assert.equal((await fixture({label:'Accept essential cookies only',text:'Subscription checkout'})).clicks,0);
});
