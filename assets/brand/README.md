# assets/brand/

The Foresight identity. **Source of truth is one SVG** (`concepts/round2/ideogram/i304_soft_friendly.svg`) and
one script (`build_brand_kit.py`); everything else here is generated from them.

```bash
# regenerate the whole kit (mark, lockups, Android icons, favicon, palette.md)
.venv/Scripts/python assets/brand/build_brand_kit.py
```

| Path | Contents |
|---|---|
| `logo/` | the mark (SVG + PNG at 48–1024 px, amber / off-white / ink), `app_icon_store_512.png` |
| `logo/lockups/` | mark + "Foresight" wordmark, horizontal and stacked, dark and light |
| `palette.md` | brand colors and the WCAG contrast table |
| `concepts/round1/`, `concepts/round2/` | every generated candidate, kept for the record (see `../ASSETS.md`) |

Rules:
- Use the SVGs wherever you can; use PNGs only where SVG is not supported (Android launcher icons, favicons).
- The wordmark is **type set in Be Vietnam Pro**, not a generated image. Change the text in the script, not in a
  paint program.
- Colors come from `palette.md` / `Color.kt`. Do not introduce new ones.
- Minimum size: the mark stays legible at 48 px; below 32 px use the mark alone without the wordmark.
