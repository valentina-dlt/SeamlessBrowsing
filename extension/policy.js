/* Cookie Calm — original code, MIT. Never treat a generic Close/OK as rejection. */
(() => {
  const normalize = text => String(text || '').normalize('NFKC').toLowerCase()
    .replace(/[’']/g, "'").replace(/[‐‑–—]/g, '-').replace(/\s+/g, ' ').trim().replace(/[.!]$/, '');
  const rejectLabels = new Set([
    'reject all', 'reject all cookies', 'reject optional cookies', 'reject non-essential cookies',
    'decline all', 'decline optional cookies', 'deny all', 'refuse all',
    'only necessary', 'necessary cookies only', 'essential cookies only',
    'use necessary cookies only', 'use essential cookies only', 'accept necessary cookies only',
    'accept only necessary cookies', 'accept essential cookies only', 'accept only essential cookies',
    'only accept essential cookies', 'only accept necessary cookies', 'continue without accepting', 'continue without agreeing',
    'tout refuser', 'refuser tout', 'alle ablehnen', 'nur notwendige cookies',
    'rechazar todo', 'rechazar todas', 'solo cookies necesarias'
  ]);
  const preferences = new Set(['manage cookies', 'manage cookie preferences', 'cookie settings',
    'manage preferences', 'customise cookies', 'customize cookies', 'manage options']);
  const save = new Set(['save preferences', 'save my preferences', 'save settings',
    'confirm my choices', 'confirm choices', 'save and close', 'save cookie settings']);
  const cookieContext = text => /\bcookies?\b|\bconsent preferences\b|\bprivacy choices\b/i.test(text || '');
  const paidChoice = text => /(?:subscribe|pay|purchase).{0,45}(?:reject|refuse|without|tracking)|(?:reject|refuse).{0,45}(?:subscribe|pay|purchase)/i.test(text || '');
  function classify(text) {
    const label = normalize(text);
    const essentialOnly = /^(?:(?:accept|allow|use|keep) (?:only (?:strictly )?(?:necessary|essential)(?: cookies)?|(?:strictly )?(?:necessary|essential)(?: cookies)? only)|only (?:accept|allow|use|keep) (?:strictly )?(?:necessary|essential)(?: cookies)?|(?:strictly )?(?:necessary|essential)(?: cookies)? only)$/;
    const declineOptional = /^(?:reject|decline|deny|refuse) (?:all(?: (?:optional|non[ -]essential))?(?: cookies)?|(?:optional|non[ -]essential) cookies)$/;
    return (rejectLabels.has(label) || essentialOnly.test(label) || declineOptional.test(label)) ? 'reject' : preferences.has(label) ? 'preferences' : save.has(label) ? 'save' : null;
  }
  function category(text) {
    const label = normalize(text);
    if (/necessary|essential|required|security|strictly|always active/.test(label)) return 'essential';
    if (/analytics|analytical|statistics|advertis|marketing|personaliz|personalis|performance|functional|preferences|legitimate interest|sale of|sharing of|targeting/.test(label)) return 'optional';
    return 'unknown';
  }
  const api = {normalize, classify, cookieContext, paidChoice, category};
  if (typeof module !== 'undefined') module.exports = api;
  else Object.defineProperty(globalThis, 'CookieCalmPolicy', {value: api});
})();
