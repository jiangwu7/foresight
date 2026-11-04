// Foresight smartphone fundus adapter — parametric OpenSCAD model (spec §7.1)
// STATUS: Phase 0 placeholder with the parameter block only. Geometry is built in Phase 2.
// Students regenerate STLs for any phone by editing the parameters and running:  openscad -o adapter.stl adapter.scad

// ---- Phone ----
phone_width      = 70.6;   // mm, e.g. Galaxy S23 70.9, A54 76.7
phone_thickness  = 7.6;    // mm
camera_offset_x  = 12.0;   // mm from the phone's left edge to the main camera center
camera_offset_y  = 10.0;   // mm from the top edge

// ---- Lens (decision: 20D first; 28D variant via these two numbers) ----
lens_diameter    = 50.0;   // mm (20D Volk-style ≈ 50 mm; 28D ≈ 41 mm)
lens_power_D     = 20;     // diopters, for documentation only
aerial_image_mm  = 50.0;   // the aerial image forms ≈ 50 mm in front of the 20D lens

// ---- Rail ----
rail_travel      = 60.0;   // mm of lens-to-camera adjustment (60–120 mm total per spec)
rail_min_gap     = 60.0;   // mm, closest lens-to-camera distance

// ---- Illumination module ----
beamsplitter_w   = 36.0;   // mm
beamsplitter_h   = 25.0;   // mm
beamsplitter_angle = 45;   // degrees
led_red_d        = 5.0;    // mm, alignment LED package
led_white_d      = 5.0;    // mm, capture LED package
polarizer_t      = 0.5;    // mm film thickness

// ---- Comfort ----
eyecup_d         = 34.0;   // mm
forehead_rest    = true;

wall             = 2.4;    // mm, printed wall thickness
$fn              = 96;

// Phase 2: modules phone_clamp(), rail(), lens_holder(), illumination_module(), eyecup(); assembly below.
echo("Foresight adapter: Phase 0 placeholder — geometry arrives in Phase 2.");
