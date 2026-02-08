#!/usr/bin/env python3

import argparse
import csv
import math

import matplotlib.pyplot as plt  # dépendance unique

def read_csv(path):
    snr = []
    cols = ["with_RZ", "without_RZ", "with_NRZ", "without_NRZ", "with_NRZT", "without_NRZT"]
    data = {c: [] for c in cols}

    with open(path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for c in ["snr_db"] + cols:
            if c not in reader.fieldnames:
                raise ValueError(f"Colonne manquante dans le CSV : {c}")
        for row in reader:
            snr.append(float(row["snr_db"]))
            for c in cols:
                v = row[c]
                data[c].append(float(v) if v.strip() != "" else float("nan"))
    return snr, data

def main():
    p = argparse.ArgumentParser(description="Trace TEB vs SNRpb depuis ./out/teb_vs_snr.csv")
    p.add_argument("--csv", default="out/teb_vs_snr.csv", help="Chemin du CSV (défaut: out/teb_vs_snr.csv)")
    p.add_argument("--save", default="teb_vs_snr.png", help="Nom du PNG à sauver (défaut: teb_vs_snr.png)")
    p.add_argument("--no-save", action="store_true", help="Ne pas sauvegarder le PNG")
    p.add_argument("--show", action="store_true", help="Afficher la fenêtre interactive")
    p.add_argument("--no-show", action="store_true", help="Ne pas afficher la fenêtre")
    p.add_argument("--logy", action="store_true", help="Axe Y en log (utile quand TEB proche de 0)")
    p.add_argument("--title", default="TEB vs SNRpb codage",
                   help="Titre du graphe")
    args = p.parse_args()

    COLORS = {
        "NRZ":  "#0c2340",  # navy
        "RZ":   "#00b8de",  # cyan
        "NRZT": "#00bf63",  # green
        "HELP": "#99cc99",  # repères
    }

    snr, data = read_csv(args.csv)

    # --- Figure 1 : TEB vs SNRpb ---
    plt.figure(figsize=(10, 6))
    plt.plot(snr, data["with_RZ"],     marker="o", color=COLORS["RZ"], lw=0.5, alpha=0.6,  label="AVEC codage (RZ)")
    plt.plot(snr, data["without_RZ"],  marker="o", color=COLORS["RZ"], label="SANS codage (RZ)")
    plt.plot(snr, data["with_NRZ"],    marker="s", color=COLORS["NRZ"], lw=0.5, alpha=0.6, label="AVEC codage (NRZ)")
    plt.plot(snr, data["without_NRZ"], marker="s", color=COLORS["NRZ"], label="SANS codage (NRZ)")
    plt.plot(snr, data["with_NRZT"],   marker="^", color=COLORS["NRZT"], lw=0.5, alpha=0.6, label="AVEC codage (NRZT)")
    plt.plot(snr, data["without_NRZT"],marker="^", color=COLORS["NRZT"], label="SANS codage (NRZT)")

    plt.xlabel("SNRpb (dB)")
    plt.ylabel("TEB")
    plt.title(args.title)
    plt.grid(True, which="both", linestyle="--", alpha=0.4)

    def min_pos(values, default=1e-6):
        vals = [v for v in values if not math.isnan(v) and v > 0]
        return min(vals) if vals else default

    ymins = [
        min_pos(data["with_RZ"]),
        min_pos(data["without_RZ"]),
        min_pos(data["with_NRZ"]),
        min_pos(data["without_NRZ"]),
        min_pos(data["with_NRZT"]),
        min_pos(data["without_NRZT"]),
    ]
    ymin = max(min(ymins), 1e-7)  # plancher numérique robuste
    plt.yscale("log")
    plt.ylim(bottom=ymin, top=1.0)

    plt.legend()
    plt.tight_layout()

    # --- Figure 2 : Gain du codage (optionnel) ---
    def delta(a, b):
        return [ (x - y) if (not math.isnan(x) and not math.isnan(y)) else float("nan")
                 for x, y in zip(a, b) ]

    plt.figure(figsize=(10, 4))
    plt.plot(snr, delta(data["without_RZ"],  data["with_RZ"]),  marker="o", color=COLORS["RZ"], label="Gain codage (RZ)")
    plt.plot(snr, delta(data["without_NRZ"], data["with_NRZ"]), marker="s", color=COLORS["NRZ"], label="Gain codage (NRZ)")
    plt.plot(snr, delta(data["without_NRZT"],data["with_NRZT"]),marker="^", color=COLORS["NRZT"], label="Gain codage (NRZT)")
    plt.xlabel("SNRpb (dB)")
    plt.ylabel("ΔTEB (sans - avec)")
    plt.title("Gain de codage")
    plt.grid(True, linestyle="--", alpha=0.4)
    plt.legend()
    plt.tight_layout()

    if not args.no_save:
        plt.savefig(args.save, dpi=150)
        base = args.save.rsplit(".", 1)[0]
        plt.figure(2)
        plt.savefig(base + "_gain.png", dpi=150)

    if args.no_show and not args.show:
        plt.close("all")
    else:
        plt.show()

if __name__ == "__main__":
    main()
