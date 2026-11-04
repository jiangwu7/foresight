// Foresight dashboard (spec §5). Plain JS, no build step, works from file://.
// Phase 1: summary cards per event. Phase 3 adds the map, trends and CSV/PNG export.
(function () {
  "use strict";

  const SCHEMA = "foresight-deid-v1";
  // Mirrors DeidExport.DISALLOWED_KEYS in the app. Any of these anywhere in the file -> reject.
  const DISALLOWED = new Set([
    "name", "first_name", "last_name", "contact", "phone", "email", "address", "dob", "birth",
    "image", "images", "image_path", "photo", "signature", "notes", "free_text", "comment",
    "contactencrypted", "consentsignatureblob", "imagepathencrypted", "heatmappath",
  ]);
  // The event's own display name is allowed under event.name only.
  const ALLOWED_PATHS = new Set(["$.event.name", "$.model.name"]);

  const $ = (sel, root = document) => root.querySelector(sel);
  const drop = $("#drop"), input = $("#file"), events = $("#events"), error = $("#error"), pill = $("#mode-pill");
  const loaded = new Map(); // event id -> file

  function validate(obj, path = "$") {
    if (Array.isArray(obj)) { obj.forEach((v, i) => validate(v, `${path}[${i}]`)); return; }
    if (obj && typeof obj === "object") {
      for (const [k, v] of Object.entries(obj)) {
        const p = `${path}.${k}`;
        if (DISALLOWED.has(k.toLowerCase()) && !ALLOWED_PATHS.has(p)) {
          throw new Error(`Rejected: the file contains a personal field "${k}" at ${p}. De-identified exports must not include it.`);
        }
        validate(v, p);
      }
    }
  }

  function checkSchema(obj) {
    if (!obj || obj.schema !== SCHEMA) throw new Error(`Rejected: not a Foresight de-identified export (schema "${obj && obj.schema}").`);
    if (!obj.event || !obj.summary || !Array.isArray(obj.participants)) throw new Error("Rejected: missing event, summary or participants.");
  }

  function fmt(v, pct) {
    if (v === null || v === undefined) return "—";
    return pct ? `${v}%` : String(v);
  }

  function render() {
    events.innerHTML = "";
    const tpl = $("#event-card");
    [...loaded.values()].sort((a, b) => b.event.date - a.event.date).forEach(f => {
      const node = tpl.content.cloneNode(true);
      $(".name", node).textContent = f.event.name;
      $(".where", node).textContent = `${f.event.location_label} · ${new Date(f.event.date).toLocaleDateString()}`;
      $(".exported", node).textContent = `EXPORTED ${new Date(f.exported_at).toLocaleString()}`;
      node.querySelectorAll("[data-k]").forEach(el => { el.textContent = fmt(f.summary[el.dataset.k], el.hasAttribute("data-pct")); });
      $(".model", node).textContent = `Model ${f.model.name} ${f.model.version} · app ${f.app_version} · ${f.participants.length} participant rows`;
      events.appendChild(node);
    });
    events.hidden = loaded.size === 0;
    pill.textContent = loaded.size ? `${loaded.size} EVENT${loaded.size > 1 ? "S" : ""} LOADED` : "NO FILE LOADED";
    pill.classList.toggle("loaded", loaded.size > 0);
  }

  async function load(files) {
    error.hidden = true;
    for (const file of files) {
      try {
        const obj = JSON.parse(await file.text());
        checkSchema(obj);
        validate(obj);
        loaded.set(obj.event.id, obj);
      } catch (e) {
        error.textContent = `${file.name}: ${e.message}`;
        error.hidden = false;
      }
    }
    render();
  }

  input.addEventListener("change", () => load(input.files));
  ["dragenter", "dragover"].forEach(ev => drop.addEventListener(ev, e => { e.preventDefault(); drop.classList.add("over"); }));
  ["dragleave", "drop"].forEach(ev => drop.addEventListener(ev, e => { e.preventDefault(); drop.classList.remove("over"); }));
  drop.addEventListener("drop", e => load(e.dataTransfer.files));

  // Expose for tests in the browser console: window.foresightDashboard.validate(obj)
  window.foresightDashboard = { validate, checkSchema, SCHEMA };
})();
