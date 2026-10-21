/* Runs in Chrome's isolated extension world. No network, cookies API, or page eval. */
(() => {
  const P = CookieCalmPolicy;
  const KNOWN = '#shopify-pc__banner,#onetrust-banner-sdk,#onetrust-pc-sdk,#CybotCookiebotDialog,#didomi-notice,#didomi-popup,.qc-cmp2-container,#usercentrics-root,#cookie-notice,#cookie-banner,.cookie-banner,.cookie-consent,[data-cookieconsent]';
  const BUTTONS = 'button,[role="button"],input[type="button"],input[type="submit"]';
  let settings = {enabled: true, pausedSites: []}, stopped = false, timer, observer;
  let actions = 0, lastAction = 0, opening = false;
  const clicked = new WeakSet();
  const host = location.ancestorOrigins?.length ? new URL(location.ancestorOrigins[location.ancestorOrigins.length - 1]).hostname : location.hostname;
  const visible = el => {
    const style = getComputedStyle(el), r = el.getBoundingClientRect();
    return el.isConnected && style.display !== 'none' && style.visibility !== 'hidden' &&
      style.opacity !== '0' && r.width > 0 && r.height > 0 && r.bottom > 0 && r.right > 0 &&
      r.top < innerHeight && r.left < innerWidth && !el.closest('[hidden],[inert],[aria-hidden="true"]');
  };
  const label = el => el.getAttribute('aria-label') || el.innerText || el.value || '';
  function roots() {
    const found = [document];
    // Open shadow trees only; closed shadow trees remain untouched.
    for (let i = 0; i < found.length && i < 30; i++) {
      for (const el of found[i].querySelectorAll('*')) if (el.shadowRoot) found.push(el.shadowRoot);
    }
    return found;
  }
  function containers(root) {
    return [...root.querySelectorAll(`${KNOWN},[role="dialog"],[role="alertdialog"],[aria-modal="true"]`)]
      .filter(el => visible(el) && P.cookieContext(el.innerText) && el.innerText.length < 35000);
  }
  function click(el) {
    if (stopped || !settings.enabled || settings.pausedSites.includes(host) || clicked.has(el) ||
        !visible(el) || el.disabled || el.getAttribute('aria-disabled') === 'true' || el.closest('form') ||
        actions >= 12 || Date.now() - lastAction < 600) return false;
    clicked.add(el); actions++; lastAction = Date.now();
    // No focus(), scrollIntoView(), CSS removal, or synthetic acceptance.
    el.click();
    return true;
  }
  function scan() {
    if (stopped || !settings.enabled || settings.pausedSites.includes(host) || document.hidden) return;
    if (Date.now() - lastAction < 650) return;
    // Leave pages alone while the user is entering information.
    const active = document.activeElement;
    if (active && (active.matches('input,textarea,select') || active.isContentEditable)) return;
    for (const root of roots()) for (const box of containers(root)) {
      if (P.paidChoice(box.innerText)) continue;
      const buttons = [...box.querySelectorAll(BUTTONS)].filter(visible);
      // Shopify's native banner uses a plain Decline label and alertdialog role.
      // Scope that otherwise ambiguous action to its verified provider markup.
      const shopifyDecline = box.matches('#shopify-pc__banner') && buttons.find(el =>
        el.id === 'shopify-pc__banner__btn-decline' && P.normalize(label(el)) === 'decline');
      const reject = shopifyDecline || buttons.find(el => P.classify(label(el)) === 'reject');
      if (reject && click(reject)) { opening = false; return; }
      // Preference editing is restricted to two known CMP panels with explicit switches.
      const panel = box.matches('#onetrust-pc-sdk,#CybotCookiebotDialog') &&
        box.querySelector('input[type="checkbox"],[role="switch"]');
      if (panel) {
        const toggles = [...box.querySelectorAll('input[type="checkbox"],[role="switch"]')].filter(visible);
        const states = toggles.map(el => {
          const text = [el.getAttribute('aria-label'), ...(el.labels || [])].map(x => typeof x === 'string' ? x : x?.innerText).join(' ') || el.closest('li,section,.ot-cat-item')?.innerText || '';
          return {el, kind: P.category(text), on: el.matches('input') ? el.checked : el.getAttribute('aria-checked') === 'true'};
        });
        // Unknown controls prevent saving potentially permissive defaults.
        if (states.length && states.every(s => s.kind !== 'unknown')) {
          const enabled = states.find(s => s.kind === 'optional' && s.on);
          if (enabled) { if (click(enabled.el)) return; else continue; }
          const save = buttons.find(el => P.classify(label(el)) === 'save');
          if (save && click(save)) { opening = false; return; }
        }
      }
      if (!opening && box.matches('#onetrust-banner-sdk,#CybotCookiebotDialog')) {
        const manage = buttons.find(el => P.classify(label(el)) === 'preferences');
        if (manage && click(manage)) { opening = true; return; }
      }
    }
  }
  const schedule = () => { if (!stopped && !timer) timer = setTimeout(() => {timer = null; scan();}, 400); };
  function stop() { stopped = true; observer?.disconnect(); clearTimeout(timer); clearInterval(poll); }
  let poll;
  chrome.storage.sync.get({enabled: true, pausedSites: []}).then(value => {
    settings = value;
    observer = new MutationObserver(schedule);
    observer.observe(document.documentElement, {childList: true, subtree: true, attributes: true,
      attributeFilter: ['class', 'style', 'hidden', 'aria-hidden', 'aria-checked']});
    poll = setInterval(schedule, 1500);
    // Observe briefly; permanently running full-page scans are unnecessary.
    setTimeout(stop, 120000);
    scan();
  });
  chrome.storage.onChanged.addListener((changes, area) => {
    if (area !== 'sync') return;
    for (const key of ['enabled', 'pausedSites']) if (changes[key]) settings[key] = changes[key].newValue;
    schedule();
  });
  addEventListener('pagehide', stop, {once: true});
})();
