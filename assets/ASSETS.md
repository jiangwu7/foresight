# ASSETS.md — provenance and prompt log for every generated file

Rules: spec §13.1. Every file under `assets/` has an entry here: tool, model, prompt, job id, date, license
note, and where it is used. Credits are logged per asset group. Real-vs-generated is recorded for every
shot that reaches the pitch video. Nothing in `assets/` is ever used as training data, a fixture, or
anything a participant or clinician could mistake for a real retinal photo or result.

## Session log

| Date | Session purpose | Balance before | Balance after | Credits spent |
|---|---|---|---|---|
| 2026-11-03 | Phase 0: mood boards (3 × 6) + logo concept directions (3 × 4) | 1585.5 | 1519.5 | 66.0 |

Per group on 2026-11-03: mood boards 18 images × 2.0 = 36.0 credits (`nano_banana_pro`, served by the
backend as `nano_banana_2`, 1k); logo concepts 12 × 2.5 = 30.0 credits (`recraft_v4_1`, `model_type: vector`,
1k, SVG output). One batch item failed submission on a mistyped model id and was resubmitted; no charge.

Licensing of generated media: Higgsfield Ultra subscription terms (commercial use permitted for the
subscriber). All generated files below are **Illustration / concept** material. None depicts a real person,
a real retina, a real result, a real partner, or a brand logo.

---

## 1. Mood boards — `assets/ui/mockups/moodboards/` (spec §13.3a step 1)

Status: **awaiting team direction choice**. Three directions, six images each, generated 2026-11-03 with
`generate_image_batch`, model `nano_banana_pro` (served as `nano_banana_2`), resolution 1k, no reference images.
Contact sheets: `contact_A-instrument.jpg`, `contact_B-horizon.jpg`, `contact_C-aperture.jpg` (made locally with PIL).
Common negative prompt in every image: *no text, no letters, no logo, no eye, no face, no retina photo*.

### Direction A — "Instrument" (precision optical instrument; charcoal + pale amber hairlines)
| File | Job id | Prompt (abridged) |
|---|---|---|
| `A-instrument/A1_key_visual.png` | b3600633-32c0-432e-ba12-7258cf38e610 | Abstract precision optical instrument aesthetic; near-black matte background; concentric hairline rings in pale amber light; tick marks like a lens focus scale; calm, quiet, minimal. 4:3 |
| `A-instrument/A2_phone_capture.png` | efce139f-4f44-4ae9-af11-f9f09733a1a6 | Smartphone in hand in a dark room; near-black UI with one thin glowing amber alignment ring and one large rounded button; greeked text only. 3:4 |
| `A-instrument/A3_materials_palette.png` | 3397d663-6383-4ee8-9862-1e5c2b957029 | Matte black anodized aluminum, amber hairline inlay, glass lens edge, brushed steel; five swatches: deep charcoal, graphite, pale amber, warm off-white, sage green. 4:3 |
| `A-instrument/A4_reticle_motif.png` | 86230a2c-c052-4fa7-91c8-d6940a5a46ff | Alignment reticle: thin luminous ring, four tick marks, inner ring, faint dot grid, dash scale; pale amber hairlines on near-black. 4:3 |
| `A-instrument/A5_result_card.png` | a89e9b4c-cd15-4b2f-b4e7-e7eb716747a5 | Dark charcoal result card, small ring icon glowing desaturated sage green, greeked text, hairline dividers; calm, humane. 3:4 (resubmitted after a typo'd model id) |
| `A-instrument/A6_adapter.png` | 771f34ac-4d36-441a-8dcd-98f7b4785911 | Matte black 3D-printed phone adapter with round condensing lens, sliding rail, rubber eyecup, single amber accent line; dark slate surface. 4:3 |

### Direction B — "Horizon" (retina-palette: oxblood to near-black, amber-gold sunrise arc)
| File | Job id | Prompt (abridged) |
|---|---|---|
| `B-horizon/B1_key_visual.png` | b8ad9fef-264a-4aef-83d1-1336aeafdf78 | Abstract luminous gradient in the warm palette of the inner eye; deep oxblood to near-black; glowing amber-gold horizon arc; abstract only. 4:3 |
| `B-horizon/B2_phone_capture.png` | 7862bfef-9408-477f-9668-c2b3eb0984f9 | Smartphone in hand, dark-red dimmed capture mode, warm-amber ring brighter at the bottom like a rising sun; greeked text. 3:4 |
| `B-horizon/B3_materials_palette.png` | fd556d6f-7777-4744-b469-2173b47ec987 | Matte black with copper-amber hairline, glass lens with warm reflection, oxblood leather; swatches: near-black, oxblood, amber gold, warm cream, sage. 4:3 |
| `B-horizon/B4_ring_motif.png` | ee3a8276-36a0-48b3-861a-9aeda578dcf0 | Single thin ring, dim at top, brightening into an amber-gold arc at the bottom like a sunrise over a horizon line. 4:3 |
| `B-horizon/B5_result_card.png` | 032303d8-d60e-47bc-ba45-a0d859f56c2a | Dark result card on oxblood-black gradient, warm glow rising from the bottom edge, small green ring icon, greeked text. 3:4 |
| `B-horizon/B6_adapter.png` | 23789fac-9ae8-4682-8d18-2e92ec93504f | Matte black adapter with copper-amber accent ring around the lens holder, lit by a single warm low light. 4:3 |

### Direction C — "Aperture" (graphite, frosted glass, warm-white light path, amber indicator)
| File | Job id | Prompt (abridged) |
|---|---|---|
| `C-aperture/C1_key_visual.png` | f1ccde50-7f03-4adf-9472-9684974b7153 | Lens aperture of thin blades forming a ring; cone of warm-white light converging to a focal point; frosted-glass haze; schematic lines. 4:3 |
| `C-aperture/C2_phone_capture.png` | a7dd12ed-da71-4f6a-a5a4-2842bc83008b | Smartphone in hand; graphite UI with aperture-blade alignment ring glowing warm white; frosted-glass panel with one large button. 3:4 |
| `C-aperture/C3_materials_palette.png` | 0f43d329-a962-4c88-95df-49700598dc8e | Frosted glass over graphite, lens edge with warm-white light leak, amber indicator; swatches: graphite, slate, warm white, pale amber, sage. 4:3 |
| `C-aperture/C4_lightpath_motif.png` | c76c3ded-af29-4f26-bf6d-4a312de5010b | Schematic of light through a round lens converging to a focus, glowing hairlines and dashed construction lines, amber dot at focus. 4:3 |
| `C-aperture/C5_result_card.png` | 17a17fd3-451f-4f04-8a4a-05899c27d82b | Frosted translucent glass result card, aperture-ring icon in calm green, greeked text, warm-white glow behind. 3:4 |
| `C-aperture/C6_adapter.png` | 531137ad-7028-4c33-b10d-99decafab2ae | Graphite adapter with frosted translucent lens shroud glowing faintly from within; dark glass surface. 4:3 |

**Review notes (Claude, 2026-11-03):** see "QA" section below.

### Winning direction: A "Instrument" (chosen by the team lead, 2026-11-03)
Every later asset reuses this style core, prepended to its own subject:

> *Precision optical instrument aesthetic. Near-black matte charcoal background (not pure black), thin
> hairline rings and tick marks in pale amber light with a soft glow, warm off-white secondary light,
> desaturated sage green for "good" states, matte black anodized surfaces with a single amber hairline
> accent. Calm, quiet, luxurious, minimal. No text, no letters, no logos, no faces, no retina photographs.*

Reference images: `A1_key_visual.png` (overall feel), `A4_reticle_motif.png` (alignment ring), `A6_adapter.png`
(hardware finish), `A3_materials_palette.png` (palette). Tokens in `app/.../ui/theme/Color.kt` are now final
for this direction. Directions B and C stay in the folder as rejected alternatives; C4's light-path schematic
may still be reused for SAFETY.md.

---

## 2. Logo concept directions — `assets/brand/concepts/` (spec §13.2)

Status: **awaiting team choice**; nothing finalized, no icon set or palette extraction yet. Generated 2026-11-03
with `generate_image_batch`, model `recraft_v4_1`, `model_type: "vector"`, 1:1, 1k, palette
`["#F2B55B", "#F5F1E8"]` on background `#0B0D10`. Output is native **SVG** (editable). Contact sheet
`contact_logo_concepts.jpg` was built from Higgsfield's WebP previews.

| File | Job id | Concept / variant |
|---|---|---|
| `1-sunrise-iris/1a_mark_geometric.svg` | a68d2a19-4bf7-4b25-9669-43ffd9cd5eeb | Eye outline whose iris is a rising half-sun over a horizon; geometric; mark only |
| `1-sunrise-iris/1b_mark_soft.svg` | 6d35ef00-f1b4-4b80-ba29-ef81f28142b7 | Lens ring, lower interior a sunrise horizon; softer; mark only |
| `1-sunrise-iris/1c_lockup_horizontal.svg` | 8a8a526c-5f50-4dc0-8b56-08344b8d4d2b | Sunrise-iris mark + "Foresight" wordmark, horizontal |
| `1-sunrise-iris/1d_lockup_stacked.svg` | 62322dd9-a3c3-43b8-a436-f0955ee24a3e | Sunrise-iris mark + wordmark, stacked |
| `2-aperture-cone/2a_mark_blades.svg` | 6222f80a-21b0-4e47-ab80-e1058750aac4 | Pupil as six-blade lens aperture + forward light cone; mark only |
| `2-aperture-cone/2b_mark_ring_beam.svg` | 36157c1d-4c21-4216-9399-b36dff3d31c0 | Bold ring, center dot, forward beam wedge; mark only |
| `2-aperture-cone/2c_lockup_horizontal.svg` | 5d5100e7-6555-49c2-826d-480bc9383557 | Aperture mark + wordmark, horizontal |
| `2-aperture-cone/2d_lockup_stacked.svg` | ce5f5d59-7db0-44d5-914c-4f937ff5e2f9 | Aperture mark + wordmark, stacked |
| `3-vessel-F/3a_mark_tree.svg` | e42bdc72-0cb2-4bff-8883-0798204a75d6 | Letter F built from a branching vessel tree; mark only |
| `3-vessel-F/3b_mark_in_ring.svg` | cb352dc9-0e34-46fa-ba6c-9f93761a727b | Vessel-tree F inside a thin ring; mark only |
| `3-vessel-F/3c_lockup_horizontal.svg` | c0920460-cd58-46a7-93a4-2ade6e67be3c | Vessel-F mark + wordmark, horizontal |
| `3-vessel-F/3d_lockup_stacked.svg` | b4adad7a-65b9-4af1-9091-54a5bc762a8b | Vessel-F mark in ring + wordmark, stacked |

**Outcome:** all 12 rejected by the team lead on 2026-11-03 ("don't like any of them"); kept in `round1/` for the
record. New brief: a mark that fuses an eye with a camera shutter, explored across styles.

## 2b. Logo round 2 — eye × camera shutter — `assets/brand/concepts/round2/` (2026-11-03)

Session: balance 1519.5 → 1441.5, **78.0 credits**: 12 × `recraft_v4_1` vector (2.5) = 30.0; 6 × `ideogram_vector`
generate, quality high (6.5) = 39.0; 6 × `gpt_image_2_5` 1k/high (1.5) = 9.0. Three models on purpose, to vary
the drawing style, not just the prompt. Recraft and Ideogram return native SVG; GPT returns PNG (raster, would
need `ideogram_vector` vectorize or a redraw if chosen). Palette `#F2B55B` amber / `#F5F1E8` off-white on `#0B0D10`.
Contact sheets: `contact_round2_recraft_gpt.jpg`, `contact_round2_ideogram.jpg`. Common negative prompt: no text,
no gradients, no shading.

| File | Model | Job id | Style brief |
|---|---|---|---|
| `recraft/r200_monoline_shutter_iris.svg` | recraft_v4_1 vector | d094a590-2671-44f6-acc9-b6d5880cc46a | monoline eye, six-blade shutter iris |
| `recraft/r201_monoline_blade_lids.svg` | recraft_v4_1 vector | 0de42abb-a20a-4b12-9e66-888ebdfd9e89 | eyelids are two shutter blades, off-white |
| `recraft/r202_solid_negative_space.svg` | recraft_v4_1 vector | 2b44e76d-14dd-4065-9709-e993b1f20b14 | solid amber eye, shutter as negative space |
| `recraft/r203_solid_blades_form_eye.svg` | recraft_v4_1 vector | 871ffff1-f595-4f5e-956d-937f04c80339 | blades themselves form the eye silhouette |
| `recraft/r204_emblem_ring_ticks.svg` | recraft_v4_1 vector | f59ac19f-c7a7-4e47-8c3d-6f3f95556e62 | lens-barrel ring with tick marks, eye inside |
| `recraft/r205_emblem_lid_over_circle.svg` | recraft_v4_1 vector | 7c18a550-0a87-45b2-9295-2feaae4ba4d8 | bold circle + eyelid arc, shutter inside, off-white |
| `recraft/r206_abstract_spiral_open.svg` | recraft_v4_1 vector | a2035538-42d2-434d-9b51-f67f32a516b7 | blades spiraling open under a lid sweep |
| `recraft/r207_abstract_five_blades.svg` | recraft_v4_1 vector | 29a62d90-f05d-4abe-846a-aaabeee4f78f | five blades, gaps form the eye, off-white |
| `recraft/r208_soft_friendly.svg` | recraft_v4_1 vector | 9a6edc2e-e219-4c60-bc51-07b367867efb | rounded chunky friendly eye |
| `recraft/r209_geometric_hex.svg` | recraft_v4_1 vector | 82b1e1b9-fbaa-4f97-a10d-2eb1c8671a12 | angular lids, hexagonal shutter |
| `recraft/r210_negative_space_circle.svg` | recraft_v4_1 vector | dc07703c-7143-4f92-93c3-783f524a6d61 | eye cut out of a solid circle, off-white |
| `recraft/r211_minimal_three_blades.svg` | recraft_v4_1 vector | 8732149e-2219-444c-83bc-f2b830256972 | three blades only |
| `ideogram/i300_monoline.svg` | ideogram_vector | 24a11a77-9427-4b60-a820-d9aee270d5bd | monoline |
| `ideogram/i301_solid_negative.svg` | ideogram_vector | 49ffb00c-1b4b-48da-a79e-ccf42ec1950b | solid negative space |
| `ideogram/i302_emblem.svg` | ideogram_vector | e77af316-50ea-41aa-a6d1-9b2512b9286e | emblem ring with ticks |
| `ideogram/i303_abstract_spiral.svg` | ideogram_vector | 139778ee-ab01-4361-8bbc-e68d1b36ff0d | abstract spiral |
| `ideogram/i304_soft_friendly.svg` | ideogram_vector | 08eb9dea-e57a-44b2-9386-5fbb8ca86a56 | soft friendly |
| `ideogram/i305_geometric.svg` | ideogram_vector | ac88ed79-5397-4329-9184-86c5e90e313f | geometric hex |
| `gpt/g400_monoline.png` | gpt_image_2_5 | b2f23a40-01a7-4806-8c1f-206d0759668f | monoline |
| `gpt/g401_solid_negative.png` | gpt_image_2_5 | ee65fb42-7adb-420e-a069-b322bae51a91 | solid negative space |
| `gpt/g402_duotone.png` | gpt_image_2_5 | 53d26695-fc49-4f1b-b85d-9a35cf60ea8d | duotone off-white lids + amber shutter |
| `gpt/g403_petals_open.png` | gpt_image_2_5 | 0bdeb136-6a59-4d16-9bbc-37ac025390fe | blades open like petals |
| `gpt/g404_app_icon.png` | gpt_image_2_5 | 306bd06f-6458-4ba6-9d4a-af9dcd907da3 | app-icon framing |
| `gpt/g405_single_line.png` | gpt_image_2_5 | 36a17c90-8832-48ad-baeb-89462dd657ca | single continuous line |

All 24 pass §13.1 (no text, no photoreal eye, no people, no brand marks).

**Review notes (Claude, 2026-11-03).** Strongest, in order: `r200` (monoline; cleanest fusion, instrument feel,
survives 48 px), `r204` (same eye inside the lens-barrel ring with ticks — matches the GlowRing/reticle motif
of direction A exactly; the ring could even become the alignment overlay), `g402` (duotone off-white lids +
amber shutter; most refined, but raster and the lids' two-tone adds a second color), `r205` (bold off-white
circle with lid arc; best app-icon silhouette), `r202`/`g401` (solid amber with negative-space blades; punchy,
reads at tiny sizes), `i303`/`r206` (blades under a single lid sweep; most "opening eye", a little decorative).
Weak: `r203` (reads as a flower/pinwheel), `r211` (ninja star), `i300` (just a shutter, no eye), `i301` (the S-shape
inside reads as a lightning bolt), `i305` (busy), `r209` (too small/angular), `r207` (fan blade).
**Team pick:** to be recorded here.

Next (after the team picks): refine the chosen concept (3–5 tighter variants), test at 48 px, run
`remove_background` for the raster versions, derive the palette, build the adaptive icon set and favicon.

---

## 3. Not generated (bundled third-party, for the record)
- Fonts in `app/app/src/main/res/font/`: Be Vietnam Pro (Light/Regular/Medium/SemiBold) and IBM Plex Mono
  (Regular/Medium), SIL Open Font License 1.1, from github.com/google/fonts. License files sit beside them.

---

## QA — spec §13.1 checklist for this session
- [x] No fundus/retinal image generated for any pipeline use (none generated at all).
- [x] No generated image placed where a real photo or result belongs (mood boards only; greeked text).
- [x] No participant data uploaded (text-only prompts, no reference media).
- [x] No fabricated evidence, statistics, partnerships or testimonials.
- [x] No real people's likenesses (hands only in phone shots), no brand logos, no minors.
- [x] `balance` called before and after; credits logged above.
- [x] Every file listed with tool, model, prompt, job id, date, license and use.
- [x] Visual review of each image for anti-patterns (§3.10) and accidental readable text — notes below.

### Review notes (Claude, 2026-11-03, from the contact sheets)
**Mood boards.** All 18 images pass §13.1: no readable text, no faces (hands only), no retina imagery, no
brand marks. Observations for the team's choice:
- *A Instrument*: the most literal reading of "precision optical instrument". A4 reticle is close to a usable
  alignment overlay as-is; A6 adapter with the single amber line is the strongest hardware look of the three.
  Risk: can tip into "luxury watch" rather than "medical screening".
- *B Horizon*: warmest and most emotional; the sunrise ring (B4) is a memorable motif and ties to the logo
  concept 1. Matches the dark-red capture mode naturally (B2). Risk: oxblood surfaces may feel heavy over a
  whole app; keep it to capture/result accents if chosen.
- *C Aperture*: cleanest and most "futuristic"; C4 light-path schematic is the single most useful figure for
  SAFETY.md and judges regardless of direction (reuse it). Risk: C1 construction lines edge toward HUD clutter;
  frosted-glass cards need care for contrast at 18sp.
- B2 and C2 show a few decorative status-bar icons on the phone screen; mockups only, not a concern.

**Logo concepts.** All 12 pass §13.1. Quality:
- Concept 1 (sunrise iris): `1a` is a clean, legible mark; `1c`/`1d` lockups work. `1b` reads as a generic
  "user profile" avatar — reject.
- Concept 2 (aperture + cone): `2a` has an awkward cut-out wedge; `2b` reads as a key/magnifier — reject.
  `2c`/`2d` are acceptable but the aperture blades will not survive 48 px.
- Concept 3 (vessel F): `3b` (cursive F in a ring) is elegant and reads as an F; `3a` and `3d` read as a bare
  tree, not an F; `3c` is a tree glyph beside the name.
- Wordmark typography in the lockups is model-generated and is **reference only** (§13.2 says do not generate
  typography as an image); the real wordmark will be set in Be Vietnam Pro.
Shortlist for the team: **1a**, **3b**, and the lockup layout of **1c** (horizontal) — pending the team's pick.
