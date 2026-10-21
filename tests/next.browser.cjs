const assert = require('node:assert/strict');
module.exports = async function(context) {
 const page = await context.newPage();
 for (const mode of ['on','off','unknown','active-vendor-list']) {
  const url='https://next-fixture.test/'+mode;
  const html=`<!doctype html><style>
    #onetrust-pc-sdk{position:fixed;top:20px;left:20px;width:500px;background:white;padding:20px}
    #ot-pc-content{height:200px;overflow:auto}.category-switch-handler{position:absolute;width:0;height:0;opacity:0}
    .ot-switch{display:block;width:100px;height:30px}.later{margin-top:1000px}
   </style><main>Shop content</main><div id="onetrust-pc-sdk" role="region"><h2>Cookie Preference Center</h2>
   <div id="ot-pc-content"><label><input type="checkbox" id="necessary" checked disabled>Strictly Necessary</label>
   <div class="ot-cat-item"><input class="category-switch-handler" type="checkbox" id="ot-group-id-Targt" ${mode==='on'?'checked':''}><label class="ot-switch" for="ot-group-id-Targt">Offsite Targeting</label></div>
   <div class="ot-cat-item later"><input class="category-switch-handler" type="checkbox" id="ot-group-id-Perf" ${mode==='on'?'checked':''}><label class="ot-switch" for="ot-group-id-Perf">Performance and Analytics</label></div>
   ${mode==='unknown'?'<label><input type="checkbox" checked>Unclassified choice</label>':''}
   </div>
   <section id="ot-pc-lst" style="display:${mode==='active-vendor-list'?'block':'none'}"><input id="chkbox-id" type="checkbox" class="category-filter-handler"><label for="chkbox-id">checkbox label</label><input id="select-all-hosts-groups-handler" type="checkbox"></section>
   <button id="accept-recommended-btn-handler">ALLOW ALL COOKIES</button><button class="save-preference-btn-handler">CONFIRM MY CHOICES</button></div>
   <script>
    document.querySelector('.save-preference-btn-handler').onclick=()=>{document.body.dataset.saved=[...document.querySelectorAll('.category-switch-handler')].every(el=>!el.checked)?'essential':'bad';};
    document.querySelector('#accept-recommended-btn-handler').onclick=()=>document.body.dataset.accepted='yes';
    document.querySelector('#chkbox-id').onclick=()=>document.body.dataset.auxiliary='clicked';
   </script>`;
  await page.route(url,r=>r.fulfill({contentType:'text/html',body:html}));await page.goto(url);
  if(['on','off'].includes(mode)) await page.waitForFunction(()=>document.body.dataset.saved==='essential');
  else {await page.waitForTimeout(2500);assert.equal(await page.locator('body').getAttribute('data-saved'),null,mode);}
  assert.equal(await page.locator('#necessary').isChecked(),true,mode);
  assert.equal(await page.locator('body').getAttribute('data-accepted'),null,mode);
  assert.equal(await page.locator('body').getAttribute('data-auxiliary'),null,mode);
  assert.deepEqual(await page.evaluate(()=>({scroll:scrollY,panelScroll:document.querySelector('#ot-pc-content').scrollTop,focus:document.activeElement.tagName})),{scroll:0,panelScroll:0,focus:'BODY'},mode);
 }
 await page.close(); console.log('Next regression: hidden checkbox inputs and offscreen categories handled; hidden filters untouched; unknown and active vendor controls prevent saving; focus and scroll preserved');
};
