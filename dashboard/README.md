# dashboard/

A static page: open `index.html` in any browser (double-click works; no server, no internet). Drop one or
more `foresight_export_<event>_deid.json` files from the app's Settings → Export onto the page.

- Reads files in the browser only; nothing is uploaded.
- Rejects any file whose schema is not `foresight-deid-v1` or that contains a personal field anywhere
  (`name`, `phone`, `contact`, `image`, `signature`, `notes`, …). The same list lives in the app's
  `DeidExport.kt`; keep the two in sync.
- Phase 1: summary cards per event. Phase 3: map (per-event lat/lng), trends, CSV and PNG export.

`sample_deid_FAKE.json` is made-up data for trying the page. It is not a real event.
