'use strict';
const $ = (s) => document.querySelector(s);

// ---- live clock ----
setInterval(() => {
  $('#clock').textContent = new Date().toLocaleString('en-GB', { hour12: false });
}, 1000);

// ---- default date range: last 24h ----
(function initDates() {
  const now = new Date();
  const yest = new Date(now.getTime() - 24 * 3600 * 1000);
  $('#to').value = toLocalInput(now);
  $('#from').value = toLocalInput(yest);
})();
function toLocalInput(d) {
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
}

// ---- load devices ----
fetch('api/devices')
  .then(r => r.json())
  .then(list => {
    const sel = $('#device');
    if (!Array.isArray(list) || list.length === 0) {
      sel.innerHTML = '<option value="">No devices found</option>';
      return;
    }
    sel.innerHTML = list.map(d =>
      `<option value="${escapeAttr(d.deviceId)}">${escapeHtml(d.refName)}</option>`).join('');
  })
  .catch(() => { $('#device').innerHTML = '<option value="">Failed to load devices</option>'; });

// ---- tabs ----
let currentTab = 'data';
document.querySelectorAll('.tab').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    currentTab = btn.dataset.tab;
    $('#view-data').hidden = currentTab !== 'data';
    $('#view-map').hidden = currentTab !== 'map';
    if (currentTab === 'map' && map) setTimeout(() => map.invalidateSize(), 80);
  });
});

// ---- run report ----
let lastPoints = [];
$('#run').addEventListener('click', runReport);

function runReport() {
  const deviceId = $('#device').value;
  const from = $('#from').value;
  const to = $('#to').value;
  if (!deviceId) return setStatus('Select a reference first', true);
  if (!from || !to) return setStatus('Pick both dates', true);

  const btn = $('#run');
  btn.disabled = true;
  setStatus('Querying…');

  const qs = `deviceId=${encodeURIComponent(deviceId)}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`;
  fetch('api/locations?' + qs)
    .then(r => r.json().then(j => ({ ok: r.ok, j })))
    .then(({ ok, j }) => {
      if (!ok) throw new Error(j.error || 'Request failed');
      lastPoints = j.points || [];
      renderStats(j);
      renderTable(lastPoints);
      renderMap(lastPoints);
      setStatus(lastPoints.length ? `${lastPoints.length} points loaded` : 'No data in range');
    })
    .catch(err => setStatus(err.message, true))
    .finally(() => { btn.disabled = false; });
}

function setStatus(msg, isErr) {
  const el = $('#status');
  el.textContent = msg;
  el.classList.toggle('err', !!isErr);
}

// ---- stats ----
function renderStats(j) {
  $('#stats').hidden = false;
  $('#s-count').textContent = j.count;
  $('#s-expected').textContent = j.expectedCount;
  $('#s-dist').innerHTML = `${j.distanceKm} <small>km</small>`;
  $('#s-speed').innerHTML = `${j.maxSpeedKmh} <small>km/h</small>`;
  const pts = (j.points || []).slice().sort((a, b) => a.fixTime - b.fixTime);
  $('#s-span').textContent = pts.length
    ? `${pts[0].fixTimeIso} → ${pts[pts.length - 1].fixTimeIso}` : '—';
}

// ---- data table ----
// device-state flags (0/1) render as Yes/No (— when null)
function yn(v){ return v == null ? '—' : (Number(v) === 1 ? 'Yes' : 'No'); }
function ynCell(v){ const c = Number(v) === 1 ? 'yes' : (Number(v) === 0 ? 'no' : ''); return `<td class="${c}">${yn(v)}</td>`; }
function onoff(v){ return v == null ? '—' : (Number(v) === 1 ? 'ON' : 'Off'); }
function onoffCell(v){ const c = Number(v) === 1 ? 'yes' : (Number(v) === 0 ? 'no' : ''); return `<td class="${c}">${onoff(v)}</td>`; }
function typeLabel(v){ return Number(v) === 1 ? 'Login' : (Number(v) === 2 ? 'Normal' : '—'); }
function typeCell(v){ const c = Number(v) === 1 ? 'yes' : ''; return `<td class="${c}">${typeLabel(v)}</td>`; }
function esc(t){ return String(t).replace(/[&<>]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;'}[c])); }
function healthCell(v){
  const t = (v == null || v === '') ? '\u2014' : v;
  const c = (t === 'All Ok') ? 'yes' : (t === '\u2014' ? '' : 'no');
  return `<td class="${c}" style="text-align:left;white-space:normal;min-width:160px">${esc(t)}</td>`;
}

function renderTable(pts) {
  const tb = $('#grid tbody');
  if (!pts.length) {
    tb.innerHTML = '<tr><td colspan="14" class="empty">No records in the selected range.</td></tr>';
    return;
  }
  const GAP_SEC = 90; // > ~1.5x the 60s cadence is treated as a gap
  let html = '';
  for (let i = 0; i < pts.length; i++) {
    const p = pts[i];
    // Table is newest-first, so the older neighbour is the next row (i + 1).
    // A large jump between them means fixes are missing in that interval.
    let rowClass = '';
    let badge = '';
    if (i + 1 < pts.length) {
      const gapSec = (p.fixTime - pts[i + 1].fixTime) / 1000;
      if (gapSec > GAP_SEC) {
        const missing = Math.max(1, Math.round(gapSec / 60) - 1);
        rowClass = ' class="gap-after"';
        badge = ` <span class="gap-badge">▲ ${(gapSec / 60).toFixed(1)} min gap · ≈${missing} missing</span>`;
      }
    }
    html += `
    <tr${rowClass}>
      <td>${i + 1}</td>
      <td>${p.fixTimeIso}${badge}</td>
      <td>${p.latitude.toFixed(6)}</td>
      <td>${p.longitude.toFixed(6)}</td>
      <td>${p.speed.toFixed(1)}</td>
      <td>${p.accuracy != null && p.accuracy > 0 ? p.accuracy.toFixed(1) : '—'}</td>
      <td class="batt">${p.battery == null ? '—' : p.battery + '%'}</td>
      ${onoffCell(p.gpsState)}${onoffCell(p.internetState)}${onoffCell(p.flightState)}${ynCell(p.isNetThere)}${ynCell(p.isNwThere)}${typeCell(p.datatype)}${healthCell(p.observations)}
    </tr>`;
  }
  tb.innerHTML = html;
}

// ---- map (Leaflet + OpenStreetMap, no API key) ----
let map, layer;
function renderMap(pts) {
  if (!map) {
    map = L.map('map', { zoomControl: true }).setView([20.5937, 78.9629], 5);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19, attribution: '© OpenStreetMap',
      crossOrigin: 'anonymous'   // fetch tiles via CORS so they pass COEP (require-corp)
    }).addTo(map);
  }
  if (layer) { map.removeLayer(layer); layer = null; }
  if (!pts.length) return;

  // The data table is newest-first, but the map track must be chronological.
  const ordered = pts.slice().sort((a, b) => a.fixTime - b.fixTime);

  layer = L.layerGroup().addTo(map);
  const latlngs = ordered.map(p => [p.latitude, p.longitude]);

  // history track: a white casing under a thicker RED line for a clean, readable route
  L.polyline(latlngs, { color: '#ffffff', weight: 7, opacity: .9 }).addTo(layer);
  L.polyline(latlngs, { color: '#dc2626', weight: 4, opacity: .95 }).addTo(layer);

  // start (green) and end (red) pin markers
  flag(ordered[0], '#16a34a', 'S', 'Start').addTo(layer);
  flag(ordered[ordered.length - 1], '#dc2626', 'E', 'End').addTo(layer);

  // intermediate points: amber dot with a white outline so each is distinct
  ordered.forEach((p, i) => {
    if (i === 0 || i === ordered.length - 1) return;
    L.circleMarker([p.latitude, p.longitude], {
      radius: 5, color: '#ffffff', weight: 2,
      fillColor: '#dc2626', fillOpacity: 1
    }).bindPopup(popupHtml(p, '#' + (i + 1))).addTo(layer);
  });

  map.fitBounds(L.latLngBounds(latlngs).pad(0.15));
  setTimeout(() => map.invalidateSize(), 80);
}

function flag(p, fill, glyph, label) {
  return L.marker([p.latitude, p.longitude], { icon: pinIcon(fill, glyph) })
    .bindPopup(popupHtml(p, label));
}

// A teardrop pin (SVG) with a centered letter — a nicer marker than a plain dot.
function pinIcon(fill, glyph) {
  const svg =
    '<svg width="30" height="42" viewBox="0 0 30 42" xmlns="http://www.w3.org/2000/svg">' +
    '<path d="M15 0C6.7 0 0 6.7 0 15c0 10.9 13.2 25.1 14.3 26.3a1 1 0 0 0 1.4 0C16.8 40.1 30 25.9 30 15 30 6.7 23.3 0 15 0z" fill="' + fill + '" stroke="#ffffff" stroke-width="2"/>' +
    '<circle cx="15" cy="15" r="6.5" fill="#ffffff"/>' +
    '<text x="15" y="18.5" text-anchor="middle" font-size="9" font-weight="700" fill="' + fill + '" font-family="Arial, sans-serif">' + glyph + '</text>' +
    '</svg>';
  return L.divIcon({ className: 'pin', html: svg, iconSize: [30, 42], iconAnchor: [15, 42], popupAnchor: [0, -38] });
}

function popupHtml(p, label) {
  return `<div>
    <b>${label}</b><br/>
    ${p.fixTimeIso}<br/>
    <b>Lat:</b> ${p.latitude.toFixed(6)}<br/>
    <b>Lon:</b> ${p.longitude.toFixed(6)}<br/>
    <b>Speed:</b> ${p.speed.toFixed(1)} km/h
    ${p.accuracy != null && p.accuracy > 0 ? `<br/><b>Acc:</b> ${p.accuracy.toFixed(1)} m` : ''}
    ${p.battery == null ? '' : `<br/><b>Batt:</b> ${p.battery}%`}
    <br/><b>GPS:</b> ${onoff(p.gpsState)}
    <br/><b>Internet:</b> ${onoff(p.internetState)}
    <br/><b>Flight:</b> ${onoff(p.flightState)}
    <br/><b>Net There:</b> ${yn(p.isNetThere)}
    <br/><b>Nw There:</b> ${yn(p.isNwThere)}
    <br/><b>Type:</b> ${typeLabel(p.datatype)}
    ${p.observations ? `<br/><b>Health:</b> ${esc(p.observations)}` : ''}
  </div>`;
}

// ---- escaping ----
function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function escapeAttr(s) { return escapeHtml(s); }