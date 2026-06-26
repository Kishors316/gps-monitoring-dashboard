<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>GPS Tracking Reports</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Sora:wght@500;600;700&family=IBM+Plex+Mono:wght@400;500&family=IBM+Plex+Sans:wght@400;500;600&display=swap" rel="stylesheet">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
      integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin=""/>
<link rel="stylesheet" href="assets/app.css"/>
</head>
<body>
<div class="bg-grid"></div>

<header class="topbar">
  <div class="brand">
    <span class="dot"></span>
    <div>
      <h1>FLEET&nbsp;TELEMETRY</h1>
      <p>GPS history &amp; analysis console</p>
    </div>
  </div>
  <div class="clock" id="clock">—</div>
</header>

<main class="wrap">
  <!-- Filter panel -->
  <section class="panel filters">
    <div class="field">
      <label for="device">Reference</label>
      <select id="device"><option value="">Loading devices…</option></select>
    </div>
    <div class="field">
      <label for="from">From</label>
      <input type="datetime-local" id="from"/>
    </div>
    <div class="field">
      <label for="to">To</label>
      <input type="datetime-local" id="to"/>
    </div>
    <button id="run" class="run">Generate</button>
  </section>

  <!-- Tabs -->
  <nav class="tabs">
    <button class="tab active" data-tab="data">Data Analysis</button>
    <button class="tab" data-tab="map">Map / History</button>
    <div class="tab-status" id="status"></div>
  </nav>

  <!-- Summary stats -->
  <section class="stats" id="stats" hidden>
    <div class="stat"><span class="k">Points · recv / exp</span><span class="v"><span id="s-count">0</span> <small>/ <span id="s-expected">0</span></small></span></div>
    <div class="stat"><span class="k">Distance</span><span class="v" id="s-dist">0 km</span></div>
    <div class="stat"><span class="k">Max speed</span><span class="v" id="s-speed">0 km/h</span></div>
    <div class="stat"><span class="k">Span</span><span class="v" id="s-span">—</span></div>
  </section>

  <!-- Data report -->
  <section class="panel view" id="view-data">
    <div class="table-scroll">
      <table id="grid">
        <thead>
          <tr>
            <th>#</th><th>Timestamp</th><th>Latitude</th><th>Longitude</th>
            <th>Speed (km/h)</th><th>Accuracy (m)</th><th>Battery</th><th>GPS</th><th>Internet</th><th>Flight</th><th>Net There</th><th>Nw There</th><th>Type</th><th>Health</th>
          </tr>
        </thead>
        <tbody><tr><td colspan="14" class="empty">Run a report to see data.</td></tr></tbody>
      </table>
    </div>
  </section>

  <!-- Map report -->
  <section class="panel view" id="view-map" hidden>
    <div id="map"></div>
  </section>
</main>

<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
        integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>
<script src="assets/app.js"></script>
</body>
</html>
