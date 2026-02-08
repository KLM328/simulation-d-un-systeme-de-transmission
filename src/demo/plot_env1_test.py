#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse, os, sys, math
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from scipy.special import erfc


PALETTE = {
    "navy":   "#0c2340",
    "cyan":   "#00b8de",
    "green":  "#00bf63",
    "lgreen": "#99cc99",
}

def read_csv_skip_comments(path):
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        lines = [ln for ln in f if not ln.lstrip().startswith("#")]
    from io import StringIO
    buf = StringIO("".join(lines))
    try:
        df = pd.read_csv(buf)
    except Exception:
        buf.seek(0)
        df = pd.read_csv(buf, sep=";")
    return df

def norm_cols(df):
    cols = []
    for c in df.columns:
        cc = str(c).strip().lower()
        cc = (cc
              .replace("é","e").replace("è","e").replace("ê","e")
              .replace("à","a").replace("û","u").replace("ô","o"))
        cc = (cc
              .replace("(","").replace(")","")
              .replace("[","").replace("]","")
              .replace("/","_").replace("\\","_"))
        cols.append(cc)
    df.columns = cols
    return df

def find_col(df, options):
    for opt in options:
        for c in df.columns:
            if c == opt or opt in c:
                return c
    return None

def to_numeric(df, cols):
    for c in cols:
        if c and df[c].dtype == object:
            df[c] = df[c].str.replace(",","%", regex=False)  # temp for decimal commas
            df[c] = df[c].str.replace("%",".", regex=False)
        if c:
            df[c] = pd.to_numeric(df[c], errors="coerce")

def interpolate_threshold(x_snr, y_teb, target=1e-3):
    d = pd.DataFrame({"snr": x_snr, "teb": y_teb}).dropna().sort_values("snr")
    if d.empty:
        return float("nan")
    x = d["snr"].values.astype(float)
    y = np.clip(d["teb"].values.astype(float), 1e-15, 1.0)
    logy = np.log10(y)
    target_log = math.log10(target)
    idx = None
    for i in range(len(x)-1):
        if (logy[i] >= target_log and logy[i+1] <= target_log) or (logy[i] <= target_log and logy[i+1] >= target_log):
            idx = i
            break
    if idx is None:
        return float("nan")
    x0,x1 = x[idx], x[idx+1]
    y0,y1 = logy[idx], logy[idx+1]
    if y1 == y0:
        return float("nan")
    t = (target_log - y0) / (y1 - y0)
    return x0 + t*(x1-x0)

def main():
    ap = argparse.ArgumentParser(description="SIT213 Env1 — Génère des graphiques Matplotlib à partir du CSV large (TEB/SNR RZ/NRZ/NRZT).")
    ap.add_argument("input_csv", help="CSV d'entrée (format large)")
    ap.add_argument("--outdir", default=".", help="Dossier de sortie (images PNG)")
    ap.add_argument("--target_ber", type=float, default=1e-3, help="TEB cible pour les marqueurs/seuils (par défaut 1e-3)")
    args = ap.parse_args()

    os.makedirs(args.outdir, exist_ok=True)

    df = read_csv_skip_comments(args.input_csv)
    df = norm_cols(df)

    # Detect columns
    snr_col  = find_col(df, ["snrdb","snr_db","snrpb","snr","e"])
    rz_col   = find_col(df, ["rz"])
    nrz_col  = find_col(df, ["nrz"])
    nrzt_col = find_col(df, ["nrzt","nrz t","nrz-t"])
    ebit_col = find_col(df, ["eb_tx j_bit","eb_tx j","eb_tx","ebtx","eb_tx_j_bit","eb_tx_j"])
    days_col = find_col(df, ["duree batterie","battery days"])

    needed = [snr_col, rz_col, nrz_col, nrzt_col]
    if any(c is None for c in needed):
        print("ERREUR: colonnes introuvables. Colonnes disponibles:")
        print(df.columns.tolist())
        sys.exit(2)

    to_numeric(df, [snr_col, rz_col, nrz_col, nrzt_col, ebit_col, days_col])

    # Build per-waveform series
    SNR = df[snr_col].values.astype(float)
    series = {
        "RZ":   df[rz_col].values.astype(float),
        "NRZ":  df[nrz_col].values.astype(float),
        "NRZT": df[nrzt_col].values.astype(float),
    }

    # 1) TEB vs SNR — semilogy
    plt.figure(figsize=(7, 4.2))
    colors = {"RZ": PALETTE["cyan"], "NRZ": PALETTE["navy"], "NRZT": PALETTE["green"]}

    # --- Courbe théorique NRZ binaire sur canal BBAG (AWGN) ---
    # Pe = Q(sqrt(2*Eb/N0)) = 0.5 * erfc( sqrt(Eb/N0) )
    ebn0_linear = 10.0 ** (SNR / 10.0)
    pe_theo = 0.5 * erfc(np.sqrt(ebn0_linear))


    # Tracé de la courbe théorique (en #e14d4b)
    plt.semilogy(SNR, pe_theo, color="#e14d4b", linewidth=2.2,
                 label="TEB théorique NRZ")
    # Tracé des courbes simulées
    for wf in ["NRZ", "RZ", "NRZT"]:
        y = np.clip(series[wf], 1e-12, 1.0)
        plt.semilogy(SNR, y, label=wf, linewidth=2.0, color=colors[wf])


    # Ligne horizontale au TEB cible
    plt.axhline(args.target_ber, linestyle="--", color=PALETTE["lgreen"], linewidth=1.5,
                label=f"TEB cible = 10e-3")

    # Limiter la fenêtre utile en Y
    plt.ylim(1e-5, 1)

    plt.xlabel("SNRpb (dB)")
    plt.ylabel("TEB")
    plt.title("TEB en fonction du SNRpb")
    plt.grid(True, which="both", linestyle="--", linewidth=0.5, alpha=0.7)
    plt.legend()
    out1 = os.path.join(args.outdir, "env1_teb_vs_snr.png")
    plt.tight_layout()
    plt.savefig(out1, dpi=300)
    plt.close()


# 2) Énergie/bit vs SNR (nJ)
    out2 = None
    if ebit_col:
        Eb_nJ = df[ebit_col].values.astype(float) * 1e9
        plt.figure(figsize=(7,4.2))
        plt.semilogy(SNR, np.clip(Eb_nJ, 1e-3, None), color=PALETTE["navy"], linewidth=2.0)
        plt.xlabel("SNRpb (dB)")
        plt.ylabel("Énergie par bit côté TX (nJ)")
        plt.title("Énergie par bit au transmetteur en fonction du SNRpb")
        plt.grid(True, which="both", linestyle="--", linewidth=0.5, alpha=0.7)
        out2 = os.path.join(args.outdir, "env1_energie_vs_snr.png")
        plt.tight_layout()
        plt.savefig(out2, dpi=300)
        plt.close()

    # 3) Durée de batterie vs SNR (jours)
    out3 = None
    if days_col:
        days = df[days_col].values.astype(float)
        plt.figure(figsize=(7,4.2))
        plt.plot(SNR, days, color=PALETTE["cyan"], linewidth=2.0)
        plt.xlabel("SNRpb (dB)")
        plt.ylabel("Durée de vie de la batterie (jours)")
        plt.title("Autonomie de la batterie en fonction du SNRpb")
        plt.grid(True, linestyle="--", linewidth=0.5, alpha=0.7)
        out3 = os.path.join(args.outdir, "env1_duree_batterie_vs_snr.png")
        plt.tight_layout()
        plt.savefig(out3, dpi=300)
        plt.close()

    # 4–5) Résumés aux seuils
    thresholds = {}
    for wf, vals in series.items():
        thr = interpolate_threshold(SNR, vals, target=args.target_ber)
        thresholds[wf] = thr

    out4 = None
    out5 = None

    # Énergie aux seuils
    if ebit_col:
        eb_summary = []
        for wf, thr in thresholds.items():
            if thr is None or math.isnan(thr):
                continue
            eb_at_thr = float(np.interp(thr, SNR, df[ebit_col].values.astype(float))) * 1e9
            eb_summary.append((wf, eb_at_thr))
        if eb_summary:
            labels = [w for w,_ in eb_summary]
            values = [v for _,v in eb_summary]
            plt.figure(figsize=(6.5,4.2))
            bar_colors = [PALETTE["navy"] if w=="NRZ" else (PALETTE["cyan"] if w=="RZ" else PALETTE["green"]) for w in labels]
            plt.bar(labels, values, color=bar_colors)
            for i,v in enumerate(values):
                plt.text(i, v*1.02, f"{v:.0f} nJ", ha="center", va="bottom", fontsize=9)
            plt.ylabel("Énergie par bit côté TX (nJ)")
            plt.title(f"Énergie par bit aux seuils TEB = {args.target_ber:g}")
            plt.grid(axis="y", linestyle="--", linewidth=0.5, alpha=0.6)
            out4 = os.path.join(args.outdir, "env1_resume_energie_par_forme.png")
            plt.tight_layout()
            plt.savefig(out4, dpi=300)
            plt.close()

    # Autonomie aux seuils
    if days_col:
        days_summary = []
        for wf, thr in thresholds.items():
            if thr is None or math.isnan(thr):
                continue
            days_at_thr = float(np.interp(thr, SNR, df[days_col].values.astype(float)))
            days_summary.append((wf, days_at_thr))
        if days_summary:
            labels = [w for w,_ in days_summary]
            values = [v for _,v in days_summary]
            plt.figure(figsize=(6.5,4.2))
            bar_colors = [PALETTE["navy"] if w=="NRZ" else (PALETTE["cyan"] if w=="RZ" else PALETTE["green"]) for w in labels]
            plt.bar(labels, values, color=bar_colors)
            for i,v in enumerate(values):
                plt.text(i, v*1.02, f"{v:.2f} j", ha="center", va="bottom", fontsize=9)
            plt.ylabel("Durée de vie de la batterie (jours)")
            plt.title(f"Autonomie aux seuils TEB = {args.target_ber:g}")
            plt.grid(axis="y", linestyle="--", linewidth=0.5, alpha=0.6)
            out5 = os.path.join(args.outdir, "env1_resume_autonomie_par_forme.png")
            plt.tight_layout()
            plt.savefig(out5, dpi=300)
            plt.close()

    print("✅ Graphiques générés :")
    print(" -", os.path.abspath(out1))
    if out2: print(" -", os.path.abspath(out2))
    if out3: print(" -", os.path.abspath(out3))
    if out4: print(" -", os.path.abspath(out4))
    if out5: print(" -", os.path.abspath(out5))

if __name__ == "__main__":
    main()
