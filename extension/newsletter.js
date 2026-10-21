/* Find dismiss controls only in email-marketing overlays, never inline forms. */
(() => {
  function find(root, visible) {
    const candidates = new Set(root.querySelectorAll('[role="dialog"],[role="alertdialog"],[aria-modal="true"],dialog[open]'));
    for (const field of root.querySelectorAll('input[type="email"],input[autocomplete="email"]')) {
      let node = field.parentElement;
      for (let depth = 0; node && depth < 7; depth++, node = node.parentElement) {
        if (['fixed', 'sticky'].includes(getComputedStyle(node).position)) { candidates.add(node); break; }
      }
    }
    const result = [];
    for (const box of candidates) {
      if (!visible(box)) continue;
      const text = (box.innerText || '').replace(/\s+/g, ' ').trim();
      if (text.length > 10000 || /\bcookies?\b|\bcheckout\b|\bsign in\b|\blog in\b|\bcontact (?:us|support)\b/i.test(text)) continue;
      if (box.querySelector('input[type="password"],input[autocomplete="current-password"],input[autocomplete="new-password"],input[autocomplete="cc-number"]')) continue;
      const email = box.querySelector('input[type="email"],input[autocomplete="email"]');
      const marketing = /\bnewsletter\b|\bmailing list\b|\bemail (?:updates|offers|marketing|news)\b|\bsubscribe\b|\bsign up\b|\bjoin\b.{0,40}\b(?:community|makers|list)\b/i.test(text);
      if (!marketing || (!email && !/\bnewsletter\b|\bmailing list\b|\bemail (?:updates|offers|marketing|news)\b/i.test(text))) continue;
      // Preserve any form the user has begun completing, including autofilled fields.
      if ([...box.querySelectorAll('input:not([type="hidden"]),textarea')].some(el => (el.value || '').trim())) continue;
      const close = [...box.querySelectorAll('button,[role="button"],a')].find(el => {
        if (!visible(el) || el.disabled || el.getAttribute('aria-disabled') === 'true') return false;
        if (el.tagName === 'A' && el.getAttribute('href') && el.getAttribute('href') !== '#') return false;
        if (el.closest('form') && (el.tagName !== 'BUTTON' || el.type !== 'button')) return false;
        const label = (el.getAttribute('aria-label') || el.getAttribute('title') || el.innerText || '').trim().toLowerCase();
        return /^(?:close(?: (?:dialog|form|popup|pop-up|modal|newsletter))?|dismiss|no[, ]+thanks|no thank you|×|✕|✖|x)$/.test(label) ||
          (el.matches('button.klaviyo-close-form') && !!box.closest('[class*="klaviyo"]'));
      });
      if (close) result.push(close);
    }
    return result;
  }
  Object.defineProperty(globalThis, 'CookieCalmNewsletter', {value: {find}});
})();
