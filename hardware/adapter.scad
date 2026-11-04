// Foresight smartphone fundus adapter — parametric OpenSCAD model (spec §7.1), Phase 2 v1.
//
// Optical axis = +Z, starting at the phone's main camera (z = 0) and pointing toward the participant.
// Order along the axis:  phone camera → illumination module (beamsplitter) → rail → 20D lens → aerial image
// plane → eyecup / participant's eye.  The aerial image of the retina forms ≈ 50 mm in front of the lens
// (on the camera side); the phone focuses on that plane, 60–120 mm from the camera depending on the phone.
//
// Students regenerate STLs for any phone by editing the parameter block and running, e.g.:
//   openscad -o stl/clamp.stl    -D 'part="clamp"'    adapter.scad
//   openscad -o stl/assembly.stl -D 'part="assembly"' adapter.scad     (or just:  make -C hardware)
// Print every part in matte black PLA/PETG (stray light!), 0.2 mm layers, 3 walls, 20 % infill.

// ======================= PARAMETERS =======================
part = "assembly";            // "assembly" | "clamp" | "illumination" | "rail" | "carriage" | "eyecup" | "exploded"

// ---- Phone ----
phone_width      = 70.9;      // mm  (Galaxy S23 70.9, S23 Ultra 78.1, A54 76.7)
phone_thickness  = 7.6;       // mm
camera_offset_x  = 12.0;      // mm from the phone's left edge (screen up, camera at the top) to the main camera center
camera_offset_y  = 10.0;      // mm from the top edge to the main camera center
clamp_depth      = 28.0;      // mm of phone edge the clamp grips, measured from the top edge
clamp_lip        = 3.0;       // mm the jaws wrap onto the screen side

// ---- Lens (decision: 20D first; 28D via these two numbers) ----
lens_diameter    = 50.0;      // mm (20D Volk-style ≈ 50 mm; 28D ≈ 41 mm)
lens_edge_t      = 8.0;       // mm edge thickness of the lens
lens_power_D     = 20;        // diopters, documentation only
aerial_image_mm  = 50.0;      // mm in front of the lens (camera side) where the aerial image forms (≈ 1000/20D)

// ---- Rail ----
// Camera-to-lens distance = (camera-to-aerial-image distance) + aerial_image_mm. The phone must be able to focus
// on the aerial image: the Galaxy S23 Ultra main camera focuses no closer than 100 mm (Settings → Device check
// shows your phone's value), so the lens sits 150 mm or more from the camera. Change these two for other phones.
rail_min_gap     = 140.0;     // mm, closest camera-to-lens distance (aerial image 90 mm from the camera)
rail_travel      = 80.0;      // mm of adjustment → lens at 140–220 mm, aerial image at 90–170 mm from the camera
rail_w           = 24.0;      // mm outer width of the square rail
rail_slot        = 4.0;       // mm slot width for the locking screw

// ---- Illumination module (sits between camera and rail) ----
beamsplitter_w   = 36.0;      // mm plate width
beamsplitter_h   = 25.0;      // mm plate height
beamsplitter_t   = 1.1;       // mm plate thickness (glass 50:50 plate or half-mirror slide)
beamsplitter_angle = 45;      // degrees to the optical axis
led_red_d        = 5.0;       // mm, alignment LED package (≈ 660 nm)
led_white_d      = 5.0;       // mm, capture LED package (warm white)
polarizer_t      = 0.5;       // mm film thickness; one in the LED path, one crossed in the camera path
illum_bore       = 14.0;      // mm clear aperture through the module on the camera axis

// ---- Comfort ----
eyecup_d         = 34.0;      // mm inner diameter at the face
eye_relief       = 15.0;      // mm from the eyecup rim to the cornea (the 20D lens works at 50 mm from the eye)
forehead_rest    = true;

wall             = 2.4;       // mm printed wall thickness
clearance        = 0.4;       // mm print clearance for sliding/inserted parts
m3               = 3.4;       // mm clearance hole for M3 screws
$fn              = 96;
eps              = 0.01;

// ======================= DERIVED =======================
illum_len   = beamsplitter_w * cos(beamsplitter_angle) + 2 * wall + 4;   // axial length of the illumination box
illum_w     = max(beamsplitter_h + 2 * wall + 4, rail_w + 2 * wall);
z_illum0    = wall;                              // module starts just above the clamp plate
z_rail0     = z_illum0 + illum_len;              // rail starts after the module
rail_len    = rail_min_gap + rail_travel - z_rail0 + lens_edge_t;
z_lens_min  = rail_min_gap;                      // lens front face at closest position
lens_holder_od = lens_diameter + 2 * (wall + clearance) + 8;
z_eye       = rail_min_gap + rail_travel / 2 + aerial_image_mm + 2;   // nominal cornea plane (mid-travel) — eyecup is attached to the carriage
cam = [camera_offset_x - phone_width / 2, 0, 0];   // camera position in clamp coordinates (clamp centered on phone width)

// ======================= MODULES =======================

// Phone clamp: a plate over the top edge of the phone with two side jaws and a bore around the camera.
module phone_clamp() {
    pw = phone_width + 2 * (wall + clearance);
    difference() {
        union() {
            // back plate (sits on the camera side of the phone) — the optical axis passes through it
            translate([-pw / 2, -clamp_depth, -phone_thickness - wall - clearance])
                cube([pw, clamp_depth + wall, wall]);
            // side jaws
            for (s = [-1, 1])
                translate([s * (pw / 2 - wall / 2) - wall / 2, -clamp_depth, -phone_thickness - wall - clearance])
                    cube([wall, clamp_depth + wall, phone_thickness + 2 * wall + clearance]);
            // screen-side lips so the clamp cannot slide off
            for (s = [-1, 1])
                translate([s * (pw / 2 - wall / 2) - (s > 0 ? clamp_lip + wall / 2 : wall / 2), -clamp_depth, wall / 2 - clearance])
                    cube([clamp_lip + wall, clamp_depth + wall, wall]);
            // top bar over the phone's top edge
            translate([-pw / 2, 0, -phone_thickness - wall - clearance]) cube([pw, wall, phone_thickness + 2 * wall + clearance]);
            // boss around the camera that the illumination module registers on
            translate([cam[0], -camera_offset_y, -phone_thickness - wall - clearance]) cylinder(d = illum_bore + 2 * wall + 6, h = wall + z_illum0);
        }
        // camera bore
        translate([cam[0], -camera_offset_y, -phone_thickness - wall - clearance - eps]) cylinder(d = illum_bore, h = 3 * wall + 1);
        // M3 holes to bolt the illumination module on (3, 120° apart)
        for (a = [30, 150, 270])
            translate([cam[0] + (illum_bore / 2 + wall + 2) * cos(a), -camera_offset_y + (illum_bore / 2 + wall + 2) * sin(a), -phone_thickness - wall - clearance - eps])
                cylinder(d = m3 - 0.6, h = 3 * wall + 1);   // tap-size hole for self-threading M3
    }
}

// Illumination module: box on the optical axis holding the 45° beamsplitter, the two LEDs on a side port,
// the LED-path polarizer and the camera-path polarizer. Light path: LED → polarizer → beamsplitter (reflects
// toward the eye) → lens → pupil. The retina's return light passes straight through the beamsplitter, then the
// crossed polarizer, into the camera.
module illumination_module() {
    L = illum_len; W = illum_w;
    difference() {
        union() {
            translate([-W / 2, -W / 2, 0]) cube([W, W, L]);
            // side port for the LEDs (on -X), long enough for the polarizer slot and two LED seats
            translate([-W / 2 - 14, -W / 2 + 4, L / 2 - 9]) cube([14 + eps, W - 8, 18]);
        }
        // clear bore along the axis
        translate([0, 0, -eps]) cylinder(d = illum_bore, h = L + 2 * eps);
        // beamsplitter slot at 45°, entered from +Y (a lid closes it)
        translate([0, 0, L / 2]) rotate([0, beamsplitter_angle, 0])
            translate([-beamsplitter_t / 2 - clearance / 2, -W / 2 - 1, -beamsplitter_h / 2 - clearance])
                cube([beamsplitter_t + clearance, W / 2 + 1 + beamsplitter_h / 2, beamsplitter_h + 2 * clearance]);
        // side-port bore from the LEDs to the beamsplitter (perpendicular to the axis, along X)
        translate([-W / 2 - 15, 0, L / 2]) rotate([0, 90, 0]) cylinder(d = illum_bore, h = W / 2 + 15);
        // LED seats: two 5 mm packages side by side at the end of the port
        for (s = [-1, 1])
            translate([-W / 2 - 14 - eps, s * 4.2, L / 2]) rotate([0, 90, 0]) cylinder(d = (s < 0 ? led_red_d : led_white_d) + clearance, h = 6);
        // LED-path polarizer slot (vertical film, between LEDs and beamsplitter)
        translate([-W / 2 - 6, -W / 2 + 2, L / 2 - 8]) cube([polarizer_t + clearance, W - 4 + 1, 16]);
        // camera-path polarizer slot (film across the bore, just above the camera), entered from +Y only
        translate([-W / 2 + 2, -W / 2 + 2, 2]) cube([W - 4, W - 2 + 1, polarizer_t + clearance]);
        // mounting holes to the clamp boss (match phone_clamp)
        for (a = [30, 150, 270])
            translate([(illum_bore / 2 + wall + 2) * cos(a), (illum_bore / 2 + wall + 2) * sin(a), -eps]) cylinder(d = m3, h = wall + 4);
        // rail socket at the top: the rail's foot slides in and is pinned
        translate([-rail_w / 2 - clearance, -rail_w / 2 - clearance, L - 6]) cube([rail_w + 2 * clearance, rail_w + 2 * clearance, 6 + eps]);
    }
}

// Rail: square tube along the axis with a slot on +X for the carriage's locking screw. Foot plugs into the module.
module rail() {
    difference() {
        union() {
            translate([-rail_w / 2, -rail_w / 2, 0]) cube([rail_w, rail_w, rail_len]);
            translate([-rail_w / 2, -rail_w / 2, -6]) cube([rail_w, rail_w, 6]);       // foot into the module socket
        }
        translate([0, 0, -6 - eps]) cylinder(d = rail_w - 2 * wall, h = rail_len + 6 + 2 * eps);   // light bore
        translate([rail_w / 2 - wall - eps, -rail_slot / 2, 8]) cube([wall + 2 * eps, rail_slot, rail_len - 16]);   // slot
        // mm scale ticks every 10 mm on -X face (students read the focus setting off these)
        for (z = [10 : 10 : rail_len - 10]) translate([-rail_w / 2 - eps, -5, z]) cube([0.6, 10, 0.6]);
    }
}

// Carriage: slides over the rail, carries the lens holder, and the eyecup bolts to its front.
module carriage() {
    cw = rail_w + 2 * (wall + clearance);
    ch = 22;
    difference() {
        union() {
            translate([-cw / 2, -cw / 2, 0]) cube([cw, cw, ch]);
            // lens holder ring on top
            translate([0, 0, ch]) cylinder(d = lens_holder_od, h = lens_edge_t + 2 * wall);
            // locking-screw boss on +X
            translate([cw / 2 - eps, -5, ch / 2 - 5]) cube([6, 10, 10]);
        }
        translate([0, 0, -eps]) translate([-rail_w / 2 - clearance, -rail_w / 2 - clearance, 0]) cube([rail_w + 2 * clearance, rail_w + 2 * clearance, ch + 2 * eps]);  // rail passage
        translate([0, 0, ch - eps]) cylinder(d = rail_w - 2 * wall, h = 3 * wall);        // light bore through the ring floor
        // lens seat: lens drops in from the front, rests on a lip; three M3 set screws hold it
        translate([0, 0, ch + wall]) cylinder(d = lens_diameter + 2 * clearance, h = lens_edge_t + wall + eps);
        translate([0, 0, ch - eps]) cylinder(d = lens_diameter - 6, h = wall + 2 * eps);  // lip aperture
        for (a = [0, 120, 240])
            translate([0, 0, ch + wall + lens_edge_t / 2]) rotate([0, 0, a]) rotate([0, 90, 0]) cylinder(d = m3 - 0.6, h = lens_holder_od);
        // locking screw through the boss into the rail slot
        translate([cw / 2 - 1, 0, ch / 2]) rotate([0, 90, 0]) cylinder(d = m3 - 0.6, h = 8);
        // eyecup mounting holes on the ring front
        for (a = [45, 135, 225, 315])
            translate([(lens_holder_od / 2 - 3) * cos(a), (lens_holder_od / 2 - 3) * sin(a), ch + lens_edge_t]) cylinder(d = m3 - 0.6, h = 3 * wall);
    }
}

// Eyecup: a flared cone from the lens holder to the face, length set by the lens working distance
// (≈ 50 mm from lens to cornea for a 20D lens) minus the eye relief; optional forehead rest arm.
module eyecup() {
    len = aerial_image_mm - eye_relief;      // 20D: ~35 mm cone
    base_d = lens_holder_od;
    difference() {
        union() {
            cylinder(d1 = base_d, d2 = eyecup_d + 2 * wall, h = len);
            if (forehead_rest)
                translate([-8, eyecup_d / 2, len - 6]) cube([16, 22, 6]);   // pad arm above the eye
        }
        translate([0, 0, -eps]) cylinder(d1 = base_d - 2 * wall, d2 = eyecup_d, h = len + 2 * eps);
        for (a = [45, 135, 225, 315])
            translate([(base_d / 2 - 3) * cos(a), (base_d / 2 - 3) * sin(a), -eps]) cylinder(d = m3, h = 2 * wall);
        if (forehead_rest) translate([-6, eyecup_d / 2 + 14, len - 6 - eps]) cube([12, 6, 6 + 2 * eps]);  // foam pad slot
    }
}

// ======================= ASSEMBLY =======================
module assembly(explode = 0) {
    color("DimGray") translate([-cam[0], camera_offset_y, phone_thickness + wall + clearance]) phone_clamp();
    e = explode;
    color("DarkSlateGray") translate([0, 0, z_illum0 + e]) illumination_module();
    color("Gray")          translate([0, 0, z_rail0 + 2 * e]) rail();
    z_car = z_lens_min + rail_travel / 2 - 22 - wall;     // mid-travel
    color("SlateGray")     translate([0, 0, z_car + 3 * e]) carriage();
    color("Black")         translate([0, 0, z_car + 22 + lens_edge_t + 2 * wall + 4 * e]) eyecup();
    // reference objects (not printed): lens, beamsplitter plate, aerial image plane
    %color("LightSkyBlue", 0.5) translate([0, 0, z_car + 22 + wall + 3 * e]) cylinder(d = lens_diameter, h = lens_edge_t);
    %color("Silver", 0.6) translate([0, 0, z_illum0 + illum_len / 2 + e]) rotate([0, beamsplitter_angle, 0]) cube([beamsplitter_t, beamsplitter_h, beamsplitter_w], center = true);
    %color("Gold", 0.3) translate([0, 0, z_car + 22 + wall - aerial_image_mm + 3 * e]) cylinder(d = 10, h = 0.5);   // aerial image plane
}

if (part == "assembly")      assembly(0);
else if (part == "exploded") assembly(25);
else if (part == "clamp")    phone_clamp();
else if (part == "illumination") illumination_module();
else if (part == "rail")     rail();
else if (part == "carriage") carriage();
else if (part == "eyecup")   eyecup();

echo(str("Foresight adapter: lens ", lens_power_D, "D ", lens_diameter, " mm; camera-to-lens ", rail_min_gap, "–", rail_min_gap + rail_travel,
         " mm; aerial image ", aerial_image_mm, " mm in front of the lens; phone ", phone_width, " mm."));
