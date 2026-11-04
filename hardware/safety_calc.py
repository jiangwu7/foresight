"""Light-hazard estimate for the Foresight adapter against ISO 15004-2 Group 1 (spec §7.2).

    python hardware/safety_calc.py            # prints the table and writes hardware/safety_calc.xlsx

This is an ESTIMATE from datasheet-style assumptions, written so a clinician or physics teacher can check
every step. Every input marked ASSUMED must be replaced by the real datasheet value or a measurement
before SAFETY.md is signed. The structure (Maxwellian-view illumination through a condensing lens) is the
same one used for hand-held fundus cameras.

Model
-----
The LED is imaged by the beamsplitter + 20D lens into the pupil plane (coaxial, Maxwellian view). All the
light that gets through the pupil is spread over the illuminated retinal patch, so

    retinal irradiance  E_retina = P_pupil / A_retina

    P_pupil  = P_led * T_polarizer * R_beamsplitter * T_lens * f_collect * f_pupil
    A_retina = pi * (d_patch / 2)^2,   d_patch = 2 * f_eye * tan(field / 2)      (f_eye = 17 mm)

The photochemical (aphakic-weighted) irradiance applies the ISO 15004-2 A(lambda) weighting to the LED
spectrum. For a monochromatic deep-red LED A(660 nm) ~ 0.001; for a warm-white LED we use a conservative
spectrum-averaged weight (ASSUMED 0.20, the blue peak dominates; replace with the integral over the
measured spectrum).

Limits (ISO 15004-2:2007, Group 1 = no light hazard; verify against the purchased standard text):
    E_A-R   weighted photochemical retinal irradiance   <= 2.2 mW/cm^2   (also dose: H_A-R <= 10 J/cm^2 is Group 2)
    E_VIR-R unweighted visible+IR retinal irradiance     <= 0.7 W/cm^2    (continuous)
    corneal UV and IR: no emission from these LEDs (negligible)
"""
from __future__ import annotations

import json
import math
from pathlib import Path

HERE = Path(__file__).resolve().parent

# ----------------------------------------------------------------------------- inputs (ASSUMED unless noted)
INPUTS = {
    "f_eye_mm": 17.0,                # standard reduced eye
    "field_deg": 45.0,               # illuminated retinal field through a 20D lens
    "pupil_mm_worst": 8.0,           # dark-adapted worst case (spec §7.2)
    "T_polarizer": 0.38,             # ASSUMED: linear polarizer film transmission (unpolarized in)
    "R_beamsplitter": 0.50,          # ASSUMED: 50:50 plate, reflection toward the eye
    "T_lens": 0.92,                  # ASSUMED: AR-coated condensing lens
    "f_collect": 0.10,               # ASSUMED: fraction of the LED's emitted power the port + lens collect into the pupil image
    "f_pupil": 1.0,                  # fraction of the collected beam that fits through the pupil (Maxwellian spot smaller than pupil)
    "sources": {
        "alignment_red": {
            "part": "ASSUMED: 5 mm 660 nm AlGaInP LED, e.g. Kingbright WP7113SRC/J-type",
            "I_mA": 5.0,             # driven dim (series resistor), ASSUMED
            "radiant_power_mW_at_20mA": 4.0,   # ASSUMED typical for a 5 mm 660 nm LED at 20 mA; replace from datasheet
            "A_weight": 0.001,       # aphakic photochemical weight at 660 nm (ISO 15004-2 A(lambda) table)
            "exposure_s": 120.0,     # continuous alignment, worst case per eye
        },
        "capture_white": {
            "part": "ASSUMED: 5 mm warm-white (2700-3000 K, CRI>=90) LED",
            "I_mA": 20.0,
            "radiant_power_mW_at_20mA": 15.0,  # ASSUMED (~5 lm warm white -> ~15 mW radiant); replace from datasheet
            "A_weight": 0.20,        # ASSUMED spectrum-averaged aphakic weight for warm white; replace with the integral
            "exposure_s": 0.050,     # one flash <= 50 ms (hard-limited by the 555 one-shot / firmware)
            "flashes_per_participant": 6,
        },
    },
    "limits": {"E_AR_mW_cm2": 2.2, "E_VIRR_W_cm2": 0.7, "H_AR_J_cm2_group2": 10.0},
}


def compute(inp: dict) -> dict:
    d_patch_mm = 2 * inp["f_eye_mm"] * math.tan(math.radians(inp["field_deg"] / 2))
    a_retina_cm2 = math.pi * (d_patch_mm / 20) ** 2
    out = {"d_patch_mm": d_patch_mm, "A_retina_cm2": a_retina_cm2, "sources": {}}
    chain = inp["T_polarizer"] * inp["R_beamsplitter"] * inp["T_lens"] * inp["f_collect"] * inp["f_pupil"]
    for name, s in inp["sources"].items():
        p_led_mW = s["radiant_power_mW_at_20mA"] * s["I_mA"] / 20.0        # linear with current (conservative for small currents)
        p_pupil_mW = p_led_mW * chain
        e_vir_W_cm2 = p_pupil_mW / 1000 / a_retina_cm2
        e_ar_mW_cm2 = p_pupil_mW * s["A_weight"] / a_retina_cm2
        t_total = s["exposure_s"] * s.get("flashes_per_participant", 1)
        h_ar_J_cm2 = e_ar_mW_cm2 / 1000 * t_total
        out["sources"][name] = {
            "P_led_mW": p_led_mW, "P_pupil_mW": p_pupil_mW,
            "E_VIR-R_W_cm2": e_vir_W_cm2, "margin_VIR": inp["limits"]["E_VIRR_W_cm2"] / e_vir_W_cm2,
            "E_A-R_mW_cm2": e_ar_mW_cm2, "margin_AR": inp["limits"]["E_AR_mW_cm2"] / e_ar_mW_cm2,
            "exposure_total_s": t_total, "H_A-R_J_cm2": h_ar_J_cm2,
            "group1": e_vir_W_cm2 <= inp["limits"]["E_VIRR_W_cm2"] and e_ar_mW_cm2 <= inp["limits"]["E_AR_mW_cm2"],
        }
    out["chain_transmission"] = chain
    return out


def write_xlsx(inp: dict, res: dict, path: Path) -> bool:
    try:
        from openpyxl import Workbook
    except Exception:
        return False
    wb = Workbook(); ws = wb.active; ws.title = "inputs"
    ws.append(["Input", "Value", "Status"])
    for k, v in inp.items():
        if k in ("sources", "limits"):
            continue
        ws.append([k, v, "ASSUMED" if k.startswith(("T_", "R_", "f_c")) else "standard"])
    for name, s in inp["sources"].items():
        for k, v in s.items():
            ws.append([f"{name}.{k}", v, "ASSUMED" if k != "A_weight" or name == "capture_white" else "ISO table"])
    for k, v in inp["limits"].items():
        ws.append([f"limit.{k}", v, "ISO 15004-2 Group 1 (verify)"])
    ws2 = wb.create_sheet("results")
    ws2.append(["Source", "P_led (mW)", "P_pupil (mW)", "E_VIR-R (W/cm2)", "margin", "E_A-R (mW/cm2)", "margin", "exposure (s)", "H_A-R (J/cm2)", "Group 1?"])
    for name, r in res["sources"].items():
        ws2.append([name, r["P_led_mW"], r["P_pupil_mW"], r["E_VIR-R_W_cm2"], r["margin_VIR"], r["E_A-R_mW_cm2"], r["margin_AR"], r["exposure_total_s"], r["H_A-R_J_cm2"], "yes" if r["group1"] else "NO"])
    ws2.append([]); ws2.append(["retinal patch diameter (mm)", res["d_patch_mm"]]); ws2.append(["retinal area (cm2)", res["A_retina_cm2"]]); ws2.append(["optical chain transmission", res["chain_transmission"]])
    wb.save(path)
    return True


def main() -> None:
    res = compute(INPUTS)
    print(f"retinal patch {res['d_patch_mm']:.1f} mm -> {res['A_retina_cm2']:.2f} cm^2; chain transmission {res['chain_transmission']:.4f}")
    print(f"{'source':16s} {'P_led mW':>9s} {'P_pupil mW':>11s} {'E_VIR-R W/cm2':>14s} {'margin':>8s} {'E_A-R mW/cm2':>13s} {'margin':>8s} {'H_A-R J/cm2':>12s} Group1")
    for name, r in res["sources"].items():
        print(f"{name:16s} {r['P_led_mW']:9.3f} {r['P_pupil_mW']:11.4f} {r['E_VIR-R_W_cm2']:14.5f} {r['margin_VIR']:8.0f}x {r['E_A-R_mW_cm2']:13.5f} {r['margin_AR']:8.0f}x {r['H_A-R_J_cm2']:12.5f} {'yes' if r['group1'] else 'NO'}")
    ok = write_xlsx(INPUTS, res, HERE / "safety_calc.xlsx")
    (HERE / "safety_calc.json").write_text(json.dumps({"inputs": INPUTS, "results": res}, indent=2))
    print("wrote safety_calc.json" + (" and safety_calc.xlsx" if ok else " (openpyxl missing: no xlsx)"))


if __name__ == "__main__":
    main()
