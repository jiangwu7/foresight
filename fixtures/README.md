# fixtures/ — real fundus images for tests

**Only real, licensed images live here. Nothing generated, ever** (spec §8, §13.1).

The images themselves are **not committed**. They come from APTOS 2019 (Kaggle), whose competition
terms do not permit redistribution, so each machine builds its own identical copy:

```bash
# after following ml/data/download.md for APTOS
.venv/Scripts/python ml/make_fixtures.py --data-dir ml/data/aptos
```

What you get:

| Folder | Contents | Count |
|---|---|---|
| `gradable/` | `grade<g>_<id>.png`, 5 real images per DR grade, longest side 1024 px | 25 |
| `ungradable/` | real images degraded to be `dark`, `blur`, `offcenter`, `reflection` | 16 |
| `expected/` | `preprocess.py` output tensors (`.bin`, float32 CHW) for the first 20 gradable images + `index.json` | 20 |
| `fixtures_manifest.json` | the exact APTOS ids used (tracked in git so everyone builds the same set) | 1 |

Selection is deterministic (first 5 ids per grade in sorted order), so `fixtures_manifest.json` in
git should always match what the script writes. If it doesn't, the APTOS download is different or
incomplete.

Tests that need the fixtures **skip** when this folder is empty (`ml/tests/test_fixture_equivalence.py`,
the Kotlin `PreprocessEquivalenceTest`). They do not fail, so CI stays green on machines without data,
but a real Phase 0 acceptance requires running them with fixtures present.
