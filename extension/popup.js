(async () => {
  const toggle = document.querySelector('#enabled'), pause = document.querySelector('#pause'), status = document.querySelector('#status');
  const [tab] = await chrome.tabs.query({active: true, currentWindow: true});
  let host;
  try { const u = new URL(tab.url); if (/^https?:$/.test(u.protocol)) host = u.hostname; } catch {}
  const settings = await chrome.storage.sync.get({enabled: true, pausedSites: []});
  toggle.checked = settings.enabled;
  const paint = () => { pause.disabled = !host; pause.textContent = !host ? 'Open a website first' : settings.pausedSites.includes(host) ? 'Resume on ' + host : 'Pause on ' + host; };
  paint();
  toggle.onchange = async () => { await chrome.storage.sync.set({enabled: toggle.checked}); status.textContent = toggle.checked ? 'Enabled. Reload to restart detection.' : 'Paused on all sites.'; };
  pause.onclick = async () => {
    // Fetch again to avoid overwriting another window's recent exceptions.
    const latest = await chrome.storage.sync.get({pausedSites: []});
    settings.pausedSites = latest.pausedSites.includes(host) ? latest.pausedSites.filter(x => x !== host) : [...latest.pausedSites, host];
    await chrome.storage.sync.set({pausedSites: settings.pausedSites});
    paint(); status.textContent = 'Saved. Reload this page if needed.';
  };
})();
