#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
plot_env1_analyse.py
--------------------
Reads the CSV produced by E6EtudeCahierDesChargesEnv1 and creates publication-ready
figures + a short textual summary explaining how the offer is met.

Usage:
    python plot_env1_analyse.py out/teb_snr_env1.csv

Outputs (in the same folder as the CSV):
    - fig_teb_vs_snr.png
    - fig_energy_vs_snr.png
    - fig_battery_vs_snr.png
    - fig_teb_zoom.png  (zoom near 1e-3)
    - summary.txt       (thresholds and key numbers)
"""

import sys
import os
import math
import csv
from typing import Dict, List, Tuple, Optional

import numpy as np
import matplotlib.pyplot as plt

TARGET_BER = 1e-3

def read_csv(path: str) -> Dict[str, np.ndarray]:
    cols = {
        "SNRdB": [], "RZ": [], "NRZ": [], "NRZT": [],
        "Eb_tx_Jbit": [], "EnergyDay_J": [], "BatteryDays": []
    }
    with open(path, "r", encoding="utf-8") as f:
        reader = csv.reader(f)
        for row in reader:
            if not row or row[0].startswith("#") or row[0] == "SNRdB":
                continue
            # Expected header: SNRdB,E,RZ,NRZ,NRZT,Eb_tx_Jbit,EnergyDay_J,BatteryDays
            try:
                snrdb = float(row[0])
                rz = float(row[2])
                nrz = float(row[3])
                nrzt = float(row[4])
                eb = float(row[5])
                eday = float(row[6])
                bdays = float(row[7])
            except (ValueError, IndexError):
                continue
            cols["SNRdB"].append(snrdb)
            cols["RZ"].append(rz)
            cols["NRZ"].append(nrz)
            cols["NRZT"].append(nrzt)
            cols["Eb_tx_Jbit"].append(eb)
            cols["EnergyDay_J"].append(eday)
            cols["BatteryDays"].append(bdays)
    for k in cols:
        cols[k] = np.array(cols[k], dtype=float)
    return cols

def find_threshold_x(x: np.ndarray, y: np.ndarray, target: float) -> Optional[float]:
    """
    Find x where y(x) crosses down through 'target'. Returns None if no crossing.
    Uses a simple linear interpolation between the bracketing points.
    """
    if len(x) != len(y) or len(x) == 0:
        return None
    # Ensure x is increasing
    order = np.argsort(x)
    x = x[order]; y = y[order]
    for i in range(1, len(x)):
        y0, y1 = y[i-1], y[i]
        if (y0 >= target and y1 <= target) or (y0 <= target and y1 >= target):
            # Linear interpolation in log10-domain if y is BER-like and monotonic
            if y0 > 0 and y1 > 0:
                t0, t1 = math.log10(y0), math.log10(y1)
                T = math.log10(target)
                if t0 == t1:
                    # flat segment: return midpoint on x
                    return float((x[i-1] + x[i]) / 2.0)
                w = (T - t0) / (t1 - t0)
                return float(x[i-1] + w * (x[i] - x[i-1]))
            else:
                # Fallback to linear y
                if y1 == y0:
                    return float((x[i-1] + x[i]) / 2.0)
                w = (target - y0) / (y1 - y0)
                return float(x[i-1] + w * (x[i] - x[i-1]))
    return None

def format_eng(x: float, unit: str = "") -> str:
    if x == 0 or not math.isfinite(x):
        return f"{x} {unit}".strip()
    exp = int(math.floor(math.log10(abs(x)) / 3) * 3)
    exp = max(-12, min(12, exp))
    scaled = x / (10 ** exp)
    prefix = { -12:"p", -9:"n", -6:"µ", -3:"m", 0:"", 3:"k", 6:"M", 9:"G", 12:"T"}[exp]
    return f"{scaled:.3g} {prefix}{unit}".strip()

def plot_teb_vs_snr(out_dir: str, snrdb: np.ndarray, rz: np.ndarray, nrz: np.ndarray, nrzt: np.ndarray) -> None:
    plt.figure()
    plt.semilogy(snrdb, rz, marker="o", label="RZ")
    plt.semilogy(snrdb, nrz, marker="o", label="NRZ")
    plt.semilogy(snrdb, nrzt, marker="o", label="NRZT")
    plt.axhline(TARGET_BER, linestyle="--")
    plt.xlabel("SNRpb = Eb/N0 (dB)")
    plt.ylabel("TEB (log)")
    plt.title("TEB vs SNRpb (AWGN)")
    plt.grid(True, which="both")
    plt.legend()
    path = os.path.join(out_dir, "fig_teb_vs_snr.png")
    plt.savefig(path, bbox_inches="tight", dpi=160)
    plt.close()

    # Zoom near target if possible (±2 decades around target)
    ymin = TARGET_BER/100; ymax = TARGET_BER*100
    plt.figure()
    plt.semilogy(snrdb, rz, marker="o", label="RZ")
    plt.semilogy(snrdb, nrz, marker="o", label="NRZ")
    plt.semilogy(snrdb, nrzt, marker="o", label="NRZT")
    plt.axhline(TARGET_BER, linestyle="--")
    plt.ylim([ymin, ymax])
    plt.xlabel("SNRpb = Eb/N0 (dB)")
    plt.ylabel("TEB (log)")
    plt.title("TEB vs SNRpb (zoom around 10^-3)")
    plt.grid(True, which="both")
    plt.legend()
    path = os.path.join(out_dir, "fig_teb_zoom.png")
    plt.savefig(path, bbox_inches="tight", dpi=160)
    plt.close()

def plot_energy_vs_snr(out_dir: str, snrdb: np.ndarray, ebtx: np.ndarray) -> None:
    plt.figure()
    plt.plot(snrdb, ebtx, marker="o")
    plt.xlabel("SNRpb = Eb/N0 (dB)")
    plt.ylabel("TX energy per bit (J)")
    plt.title("Transmit energy per bit vs SNRpb")
    plt.grid(True)
    path = os.path.join(out_dir, "fig_energy_vs_snr.png")
    plt.savefig(path, bbox_inches="tight", dpi=160)
    plt.close()

def plot_battery_vs_snr(out_dir: str, snrdb: np.ndarray, bdays: np.ndarray) -> None:
    plt.figure()
    plt.plot(snrdb, bdays, marker="o")
    plt.xlabel("SNRpb = Eb/N0 (dB)")
    plt.ylabel("Battery life (days)")
    plt.title("Battery life vs SNRpb (3 J, 1e6 bits/day)")
    plt.grid(True)
    path = os.path.join(out_dir, "fig_battery_vs_snr.png")
    plt.savefig(path, bbox_inches="tight", dpi=160)
    plt.close()

def main(argv: List[str]) -> int:
    if len(argv) < 2:
        print(__doc__)
        return 2
    csv_path = argv[1]
    if not os.path.isfile(csv_path):
        print(f"CSV not found: {csv_path}")
        return 2
    out_dir = os.path.dirname(os.path.abspath(csv_path)) or "."
    data = read_csv(csv_path)

    snrdb = data["SNRdB"]
    rz, nrz, nrzt = data["RZ"], data["NRZ"], data["NRZT"]
    ebtx = data["Eb_tx_Jbit"]
    eday = data["EnergyDay_J"]
    bdays = data["BatteryDays"]

    # Plots
    plot_teb_vs_snr(out_dir, snrdb, rz, nrz, nrzt)
    plot_energy_vs_snr(out_dir, snrdb, ebtx)
    plot_battery_vs_snr(out_dir, snrdb, bdays)

    # Thresholds @ 1e-3
    thr = {}
    for name, y in [("RZ", rz), ("NRZ", nrz), ("NRZT", nrzt)]:
        xthr = find_threshold_x(snrdb, y, TARGET_BER)
        thr[name] = xthr

    # Choose the "best energy" among the forms at the target BER (lowest SNR)
    best_form = None
    best_snr = None
    for name in ("NRZ", "NRZT", "RZ"):
        if thr[name] is None:
            continue
        if best_snr is None or thr[name] < best_snr:
            best_form, best_snr = name, thr[name]

    # Interpolate energy metrics at the best_snr
    def interp(x, y, x0):
        return float(np.interp(x0, x, y)) if (x0 is not None) else float('nan')

    best_eb   = interp(snrdb, ebtx, best_snr)
    best_eday = interp(snrdb, eday, best_snr)
    best_bday = interp(snrdb, bdays, best_snr)

    # Summary text
    lines = []
    lines.append("=== Environnement 1 — Synthesis ===")
    lines.append(f"Target BER: {TARGET_BER:.1e}")
    for name in ("RZ", "NRZ", "NRZT"):
        if thr[name] is None:
            lines.append(f"- {name}: no crossing at BER ≤ {TARGET_BER:.1e} within the SNR sweep.")
        else:
            lines.append(f"- {name}: threshold SNRpb ≈ {thr[name]:.2f} dB")

    if best_form is not None:
        lines.append("")
        lines.append(f"Chosen waveform (min energy at spec): {best_form}")
        lines.append(f"SNRpb at BER=1e-3: {best_snr:.2f} dB")
        lines.append(f"TX energy per bit: {format_eng(best_eb, 'J')}")
        lines.append(f"Energy per day (1e6 bits): {format_eng(best_eday, 'J')}")
        lines.append(f"Battery life (3 J): {best_bday:.2f} days")
    else:
        lines.append("No waveform reached the BER target in the sweep; extend the SNR range.")

    # Save summary
    summary_path = os.path.join(out_dir, "summary.txt")
    with open(summary_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    # Print to console too
    print("\n".join(lines))
    print(f"\nSaved figures and summary in: {out_dir}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
