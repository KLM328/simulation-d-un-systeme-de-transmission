#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os, math, argparse
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from scipy.special import erfc  # pour la courbe théorique

PALETTE = {
    "navy":   "#0c2340",  # NRZ
    "cyan":   "#00b8de",  # RZ
    "green":  "#00bf63",  # NRZT
    "lgreen": "#99cc99",  # repères
}

def read_csv_skip_comments(path):
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        lines = [ln for ln in f if not ln.lstrip().startswith("#")]
    from io import StringIO
    buf = StringIO("".join(lines))
    try:
        df = pd.read_csv(buf)
    except Exception:
        buf.seek(0); df = pd.read_csv(buf, sep=";")
    return df

def norm_cols(df):
    cols = []
    for c in df.columns:
        cc = str(c).strip().lower()
        for a,b in [("é","e"),("è","e"),("ê","e"),("à","a"),("û","u"),("ô","o")]:
            cc = cc.replace(a,b)
        cc = cc.replace("(","").replace(")","").replace("[","").replace("]","")
        cc = cc.replace("/","_").replace("\\","_")
        cols.append(cc)
    df.columns = cols
    return df

def find_col(df, keys):
    for k in keys:
        for c in df.columns:
            if k in c:
                return c
    return None

def load_series(path):
    df = read_csv_skip_comments(path)
    df = norm_cols(df)
    snr = df[find_col(df,["snrdb","snr"])].to_numpy(float)
    data = {
        "SNR": snr,
        "RZ": df[find_col(df,["rz"])].to_numpy(float),
        "NRZ": df[find_col(df,["nrz"])].to_numpy(float),
        "NRZT": df[find_col(df,["nrzt"])].to_numpy(float),
        "Eb_tx": df[find_col(df,["eb_tx"])].to_numpy(float),
        "Days": df[find_col(df,["duree","batterie","days"])].to_numpy(float)
    }
    return data

def interp_threshold(snr, teb, target=1e-3):
    snr, teb = np.asarray(snr,float), np.asarray(teb,float)
    y = np.clip(teb, 1e-15, 1.0)
    ly, lt = np.log10(y), math.log10(target)
    for i in range(len(snr)-1):
        if (ly[i]>=lt and ly[i+1]<=lt) or (ly[i]<=lt and ly[i+1]>=lt):
            a = (lt-ly[i])/(ly[i+1]-ly[i])
            return snr[i]+a*(snr[i+1]-snr[i])
    return np.nan

def plot_teb_vs_snr_one(data, outpng, title, show_theory=True, target_ber=1e-3):
    colors = {"NRZ":PALETTE["navy"], "RZ":PALETTE["cyan"], "NRZT":PALETTE["green"]}
    snr = data["SNR"]

    plt.figure(figsize=(8.4,4.8))
    for wf in ["NRZ","RZ","NRZT"]:
        plt.semilogy(snr, np.clip(data[wf],1e-12,1), color=colors[wf], lw=2.2, label=wf)

    if show_theory:
        ebn0 = 10.0**(snr/10.0)
        pe = 0.5*erfc(np.sqrt(ebn0))
        plt.semilogy(snr, pe, color="#e14d4b", lw=2.0, label="TEB théorique NRZ (sans codage)")

    # Ligne de spec (libellé exigé)
    plt.axhline(target_ber, ls="--", color=PALETTE["lgreen"], lw=1.4, label="TEB cible = 10e-3")

    # Seuils + encarts lisibles (un seul graphique → pas de chevauchement)
    thr = {}
    for wf in ["NRZ","RZ","NRZT"]:
        thr[wf] = interp_threshold(snr, data[wf], target_ber)
        if np.isfinite(thr[wf]):
            ytxt = 4e-4 if wf=="NRZ" else (1.2e-4 if wf=="RZ" else 7e-4)
            plt.axvline(thr[wf], color=colors[wf], ls=":", lw=1.6, alpha=0.9)
            plt.annotate(f"{wf}\n≈ {thr[wf]:.2f} dB",
                         xy=(thr[wf], ytxt), xytext=(6,8), textcoords="offset points",
                         fontsize=8, bbox=dict(boxstyle="round,pad=0.25", fc="white",
                                               ec=colors[wf], lw=1))

    plt.ylim(1e-5, 1)
    plt.xlabel("SNRpb (dB)")
    plt.ylabel("TEB")
    plt.title(title)
    plt.grid(True, which="both", ls="--", lw=0.5, alpha=0.7)
    plt.legend(ncol=2)
    plt.tight_layout()
    plt.savefig(outpng, dpi=300)
    plt.close()
    return thr

def main():
    ap = argparse.ArgumentParser(description="Graphes E1: TEB vs SNR (séparés) + Autonomie à TEB=10e-3")
    ap.add_argument("uncoded_csv", help="out/teb_snr_env1_uncoded.csv")
    ap.add_argument("coded_csv", help="out/teb_snr_env1_coded_R-1_3.csv")
    ap.add_argument("--outdir", default="out")
    ap.add_argument("--target_ber", type=float, default=1e-3)
    ap.add_argument("--rate", type=float, default=1/3, help="Taux du code (par défaut 1/3)")
    args = ap.parse_args()
    os.makedirs(args.outdir, exist_ok=True)

    unc = load_series(args.uncoded_csv)
    cod = load_series(args.coded_csv)

    # 1) TEB vs SNRpb — SANS codage (graphique dédié)
    thr_unc = plot_teb_vs_snr_one(
        data=unc,
        outpng=os.path.join(args.outdir, "env1_teb_vs_snr_uncoded.png"),
        title="TEB en fonction du SNRpb — sans codage",
        show_theory=True,
        target_ber=args.target_ber
    )

    # 2) TEB vs SNRpb — AVEC codage (graphique dédié)
    thr_cod = plot_teb_vs_snr_one(
        data=cod,
        outpng=os.path.join(args.outdir, "env1_teb_vs_snr_coded.png"),
        title="TEB en fonction du SNRpb — avec codage",
        show_theory=True,
        target_ber=args.target_ber
    )

    # 3) Autonomie à TEB = 10e-3 — barres par mode (uncoded vs coded)
    labs = ["NRZ","RZ","NRZT"]
    vals_days_unc, vals_days_cod = [], []

    for wf in labs:
        # Jours aux seuils (interpolation sur les courbes Days)
        if np.isfinite(thr_unc[wf]):
            du = float(np.interp(thr_unc[wf], unc["SNR"], unc["Days"]))
        else:
            du = np.nan
        if np.isfinite(thr_cod[wf]):
            dc = float(np.interp(thr_cod[wf], cod["SNR"], cod["Days"]))
        else:
            dc = np.nan
        vals_days_unc.append(du)
        vals_days_cod.append(dc)

    x = np.arange(len(labs)); width = 0.35
    plt.figure(figsize=(7.2,4.4))
    bar_colors = [PALETTE["navy"], PALETTE["cyan"], PALETTE["green"]]

    plt.bar(x - width/2, vals_days_unc, width, label="Sans codage", color=bar_colors)
    plt.bar(x + width/2, vals_days_cod, width, label="Avec codage", color=bar_colors, alpha=0.6)

    for i,v in enumerate(vals_days_unc):
        if np.isfinite(v): plt.text(i-width/2, v*1.02, f"{v:.2f}", ha="center", va="bottom", fontsize=9)
    for i,v in enumerate(vals_days_cod):
        if np.isfinite(v): plt.text(i+width/2, v*1.02, f"{v:.2f}", ha="center", va="bottom", fontsize=9)

    plt.xticks(x, labs)
    plt.ylabel("Durée de vie de la batterie (jours)")
    plt.title("Autonomie à TEB = 10e-3")
    plt.grid(axis="y", ls="--", lw=0.5, alpha=0.6)
    plt.legend()
    plt.tight_layout()
    plt.savefig(os.path.join(args.outdir, "env1_autonomie_10e-3.png"), dpi=300)
    plt.close()

    # Export récap seuils pour ton CR (optionnel mais utile)
    recap = []
    for wf in labs:
        recap.append({"waveform": wf, "mode": "sans codage", "snrpb_at_teb_10e-3_db": None if not np.isfinite(thr_unc[wf]) else round(float(thr_unc[wf]), 3),
                      "autonomie_jours": None if not np.isfinite(thr_unc[wf]) else round(float(np.interp(thr_unc[wf], unc["SNR"], unc["Days"])), 3)})
        recap.append({"waveform": wf, "mode": "avec codage", "snrpb_at_teb_10e-3_db": None if not np.isfinite(thr_cod[wf]) else round(float(thr_cod[wf]), 3),
                      "autonomie_jours": None if not np.isfinite(thr_cod[wf]) else round(float(np.interp(thr_cod[wf], cod["SNR"], cod["Days"])), 3)})
    pd.DataFrame(recap).to_csv(os.path.join(args.outdir, "env1_recap_seuils_autonomie_10e-3.csv"), index=False)

    print("✅ Fichiers créés dans", os.path.abspath(args.outdir))
    print(" - env1_teb_vs_snr_uncoded.png")
    print(" - env1_teb_vs_snr_coded.png")
    print(" - env1_autonomie_10e-3.png")
    print(" - env1_recap_seuils_autonomie_10e-3.csv")

if __name__ == "__main__":
    main()
