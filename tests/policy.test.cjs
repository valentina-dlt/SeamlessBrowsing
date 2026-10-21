const test = require('node:test');
const assert = require('node:assert/strict');
const P = require('../extension/policy.js');
test('only explicit opt-out phrases are eligible', () => {
  for (const label of ['Reject all', '  REJECT   OPTIONAL COOKIES ', 'Continue without accepting', 'Essential cookies only', 'Tout refuser', 'Accept essential cookies only', 'Accept only essential cookies', 'Only accept necessary cookies']) assert.equal(P.classify(label), 'reject', label);
});
test('acceptance, dismissals, checkout and ambiguous controls are never opt-out', () => {
  for (const label of ['Accept all', 'Accept cookies', 'Accept', 'Reject', 'Close', 'OK', 'Continue', 'Buy now', 'Reject all and subscribe', 'Do not sell my data', 'Reject all orders', 'Accept essential and optional cookies', 'Accept all essential cookies', 'Accept essential cookies only and subscribe', 'Do not accept essential cookies only']) assert.equal(P.classify(label), null, label);
});
test('cookie context is required and pay-or-consent is excluded', () => {
  assert.equal(P.cookieContext('Choose cookie preferences'), true);
  assert.equal(P.cookieContext('Reject all invitations'), false);
  assert.equal(P.paidChoice('Subscribe to continue without tracking'), true);
  assert.equal(P.paidChoice('Reject all cookies or pay monthly'), true);
  assert.equal(P.paidChoice('Cookies help our site work'), false);
});
test('essential categories are preserved and unknown categories are blocked', () => {
  assert.equal(P.category('Strictly necessary cookies'), 'essential');
  for (const label of ['Analytics', 'Advertising cookies', 'Legitimate interests', 'Functional cookies']) assert.equal(P.category(label), 'optional');
  assert.equal(P.category('Partners'), 'unknown');
});
