#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse, re, os, math
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

# --- Charte couleurs ---
COLORS = {
    "NRZ":  "#0c2340",  # navy
    "RZ":   "#00b8de",  # cyan
    "NRZT": "#00bf63",  # green
    "HELP": "#99cc99",  # repères
}

def dbm_to_watts(dbm):
    return 1e-3 * (10.**(dbm/10.))

def parse_header_meta(first_line):
    meta = {"N0_dBm_Hz": None, "atten_dB": None, "bits_per_day": None, "battery_J": None}
    if not first_line or not first_line.lstrip().startswith("#"):
        return meta
    l = first_line.replace(",", ".")
    m = re.search(r"N0\s*=\s*([+-]?\d+(\.\d+)?)", l, re.I);                meta["N0_dBm_Hz"]   = float(m.group(1)) if m else None
    m = re.search(r"Atten(nuation)?\s*\(dB\)\s*=\s*([+-]?\d+(\.\d+)?)", l, re.I); meta["atten_dB"]     = float(m.group(2)) if m else None
    m = re.search(r"bits\s*\(par\s*jour\)\s*=\s*([+-]?\d+(\.\d+)?)", l, re.I);    meta["bits_per_day"] = float(m.group(1)) if m else None
    m = re.search(r"batterie\s*\(J\)\s*=\s*([+-]?\d+(\.\d+)?)", l, re.I);         meta["battery_J"]    = float(m.group(1)) if m else None
    return meta

def robust_read(csv_path):
    with open(csv_path, "r", encoding="utf-8", errors="ignore") as f:
        lines = f.readlines()
    meta = parse_header_meta(lines[0] if lines else "")
    data_lines = [ln for ln in lines if not ln.lstrip().startswith("#")]
    from io import StringIO
    buf = StringIO("".join(data_lines))
    try:
        df = pd.read_csv(buf)
    except Exception:
        buf.seek(0)
        df = pd.read_csv(buf, sep=";")

    def norm(s):
        s = str(s).strip().lower()
        for a,b in [("é","e"),("è","e"),("ê","e"),("à","a"),("ô","o")]:
            s = s.replace(a,b)
        return (s.replace("(","").replace(")","")
                .replace("[","").replace("]","")
                .replace("/","_").replace("\\","_"))

    df.columns = [norm(c) for c in df.columns]
    return meta, df

def find_col(df, options):
    for w in options:
        for c in df.columns:
            if c == w or w in c:
                return c
    return None

def compute_eb_from_snr(SNR_dB, N0_dBm_Hz, atten_dB):
    ebn0_lin = 10.**(SNR_dB/10.)
    N0_W_Hz  = dbm_to_watts(N0_dBm_Hz)
    Eb_rx    = ebn0_lin * N0_W_Hz
    Eb_tx    = Eb_rx * (10.**(atten_dB/10.))
    return Eb_tx  # J/bit transmis côté TX

def interpolate_threshold(SNR, TEB, target=1e-3):
    d = pd.DataFrame({"snr":SNR, "teb":TEB}).dropna().sort_values("snr")
    if d.empty:
        return float("nan")
    x = d["snr"].to_numpy(float)
    y = np.clip(d["teb"].to_numpy(float), 1e-15, 1.0)
    logy = np.log10(y); tgt = math.log10(target)
    for i in range(len(x)-1):
        if (logy[i] >= tgt and logy[i+1] <= tgt) or (logy[i] <= tgt and logy[i+1] >= tgt):
            t = (tgt-logy[i])/(logy[i+1]-logy[i]) if logy[i+1]!=logy[i] else 0.0
            return x[i] + t*(x[i+1]-x[i])
    return float("nan")

def build_curves(input_csv, target_ber, rate=None):
    """
    Lit un CSV, calcule Eb_tx si absent, puis l'autonomie (jours).
    Si 'rate' est défini (ex: 1/3), on applique le surdébit bits/jour = bits_utiles/rate.
    """
    meta, df = robust_read(input_csv)
    snr_col  = find_col(df, ["snrdb","snr_db","snrpb","snr","e"])
    rz_col   = find_col(df, ["rz"])
    nrz_col  = find_col(df, ["nrz"])
    nrzt_col = find_col(df, ["nrzt","nrz t","nrz-t"])
    ebit_col = find_col(df, ["eb_tx j_bit","eb_tx (j_bit)","eb_tx j","eb_tx","ebtx","eb_tx_j_bit","eb_tx_j"])
    days_col = find_col(df, ["duree","batterie","days"])

    for c in [snr_col, rz_col, nrz_col, nrzt_col, ebit_col, days_col]:
        if c and df[c].dtype == object:
            df[c] = df[c].str.replace(",", ".", regex=False)
        if c:
            df[c] = pd.to_numeric(df[c], errors="coerce")

    SNR = df[snr_col].to_numpy(float)

    # Eb côté TX : depuis CSV si dispo, sinon via N0/atténuation
    if ebit_col is None:
        Eb_tx_J = compute_eb_from_snr(SNR, meta["N0_dBm_Hz"], meta["atten_dB"])
    else:
        Eb_tx_J = df[ebit_col].to_numpy(float)

    bits_per_day_useful = meta["bits_per_day"] or 1e6
    battery_J           = meta["battery_J"] or 3.0

    # bits transmis par jour (on applique R si fourni)
    if rate is None:
        bits_per_day_tx = bits_per_day_useful
    else:
        bits_per_day_tx = bits_per_day_useful / rate

    energy_day_J = Eb_tx_J * bits_per_day_tx
    battery_days = (battery_J / np.clip(energy_day_J, 1e-30, None))

    teb = {
        "RZ":   df[rz_col].to_numpy(float),
        "NRZ":  df[nrz_col].to_numpy(float),
        "NRZT": df[nrzt_col].to_numpy(float),
    }

    # zones conformes (TEB <= cible)
    duree_par_forme = {}
    for wf in ["NRZ","RZ","NRZT"]:
        mask = teb[wf] <= target_ber
        y = battery_days.copy()
        y[~mask] = np.nan
        duree_par_forme[wf] = y

    # seuils SNRpb (dB) aux intersections TEB=cible
    thresholds = {wf: interpolate_threshold(SNR, teb[wf], target=target_ber) for wf in ["NRZ","RZ","NRZT"]}

    return dict(
        meta=meta, SNR=SNR, battery_days=battery_days,
        duree_par_forme=duree_par_forme, thresholds=thresholds
    )

def plot_autonomy(SNR, duree_par_forme, battery_days, thresholds, out_path, title):
    fig, ax = plt.subplots(figsize=(9,5.2))
    for wf in ["NRZ","RZ","NRZT"]:
        ax.plot(SNR, duree_par_forme[wf], color=COLORS[wf], lw=2.2, label=f"{wf} (TEB ≤ 0.001)")

    ax.set_xlabel("SNRpb (dB)")
    ax.set_ylabel("Durée de vie de la batterie (jours)")
    ax.set_title(title)
    ax.grid(True, ls="--", lw=0.6, alpha=0.7)
    ax.legend()

    # décalages pensés pour éviter le chevauchement
    offsets = {
        "NRZ":  ( 8,  12),
        "RZ":   (-70,-20),
        "NRZT": ( 8, -30),
    }

    for wf in ["NRZ","RZ","NRZT"]:
        thr = thresholds[wf]
        if math.isnan(thr):
            continue
        y_thr = float(np.interp(thr, SNR, battery_days))
        ax.axvline(thr, ymin=0, ymax=1, color=COLORS[wf], ls=":", lw=1.8)
        ax.plot([thr],[y_thr], marker="o", color=COLORS[wf], ms=6)
        dx, dy = offsets.get(wf, (8,12))
        ax.annotate(
            f"{wf}\nSNRpb ≈ {thr:.2f} dB\n≈ {y_thr:.2f} j",
            xy=(thr, y_thr),
            xytext=(dx, dy),
            textcoords="offset points",
            fontsize=9,
            bbox=dict(boxstyle="round,pad=0.3", fc="white", ec=COLORS[wf], lw=1),
            arrowprops=dict(arrowstyle="->", color=COLORS[wf], lw=1)
        )

    fig.tight_layout()
    fig.savefig(out_path, dpi=300)
    print(f"✅ Graphique créé : {os.path.abspath(out_path)}")

def main():
    ap = argparse.ArgumentParser(description="Env1 — Autonomie vs SNRpb (avec & sans codage canal) avec annotations lisibles.")
    ap.add_argument("uncoded_csv", help="CSV sans codage")
    ap.add_argument("coded_csv", help="CSV avec codage")
    ap.add_argument("--out_uncoded", default="env1_duree_par_forme_uncoded.png")
    ap.add_argument("--out_coded",   default="env1_duree_par_forme_coded_R-1_3.png")
    ap.add_argument("--target_ber",  type=float, default=1e-3)
    ap.add_argument("--rate",        type=float, default=1/3, help="taux de code (ex: 1/3)")
    args = ap.parse_args()

    # --- SANS codage (bits_tx/jour = bits_utiles/jour)
    U = build_curves(args.uncoded_csv, target_ber=args.target_ber, rate=None)
    plot_autonomy(
        SNR=U["SNR"],
        duree_par_forme=U["duree_par_forme"],
        battery_days=U["battery_days"],
        thresholds=U["thresholds"],
        out_path=args.out_uncoded,
        title=""
    )

    # --- AVEC codage (bits_tx/jour = bits_utiles/jour / R)
    C = build_curves(args.coded_csv, target_ber=args.target_ber, rate=args.rate)
    plot_autonomy(
        SNR=C["SNR"],
        duree_par_forme=C["duree_par_forme"],
        battery_days=C["battery_days"],
        thresholds=C["thresholds"],
        out_path=args.out_coded,
        title=f""
    )

if __name__ == "__main__":
    main()
