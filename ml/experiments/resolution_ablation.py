"""
Resolution ablation (spec §4.6) — PHASE 2. Not implemented in Phase 0.

Plan: load the Messidor-2 test set, degrade progressively (downsample to 1024/512/384/256/128 px,
Gaussian blur sigma 1-4, synthetic corneal reflection and vignetting that mimic the phone adapter),
run M2 at each level, and plot referable-DR sensitivity/specificity vs. degradation. The degradation
functions will be shared with `ml/make_fixtures.py::degrade` and the Phase 2 training augmentations
so the three stay consistent.
"""
import sys

if __name__ == "__main__":
    print(__doc__)
    sys.exit(2)
