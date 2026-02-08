#!/usr/bin/env python3
import sys, csv, math
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.ticker import LogLocator, NullFormatter
from scipy.special import erfc  # nécessaire pour la formule théorique

if len(sys.argv) < 2:
    print("Usage: python plot_teb.py out/teb_snr.csv")
    sys.exit(1)

csv_path = sys.argv[1]

# Lecture CSV (ignore la ligne meta commençant par #)
snr, rz, nrz, nrzt = [], [], [], []
with open(csv_path, "r", newline="") as f:
    first = f.readline()
    if not first.startswith("#"):
        f.seek(0)
    rdr = csv.DictReader(f)
    for row in rdr:
        snr.append(float(row["SNRdB"]))
        rz.append(float(row["RZ"]))
        nrz.append(float(row["NRZ"]))
        nrzt.append(float(row["NRZT"]))

snr  = np.array(snr)
rz   = np.array(rz)
nrz  = np.array(nrz)
nrzt = np.array(nrzt)

# Remplace les zéros (ou valeurs <=0) par un petit plancher pour support log
def floor_from(arrs):
    vals = np.concatenate([a[a > 0] for a in arrs if np.any(a > 0)])
    return max(vals.min() / 3.0, 1e-7) if vals.size > 0 else 1e-6

floor = floor_from([rz, nrz, nrzt])
rz   = np.where(rz  > 0, rz,   floor)
nrz  = np.where(nrz > 0, nrz,  floor)
nrzt = np.where(nrzt> 0, nrzt, floor)

# Calcul de la courbe théorique (NRZ binaire)
ebn0_linear = 10 ** (snr / 10)
pe_theo = 0.5 * erfc(np.sqrt(ebn0_linear))

# Tracé interactif
fig, ax = plt.subplots(figsize=(9, 5), dpi=130)
ax.plot(snr, rz,   "-o", linewidth=2, markersize=4, label="TEB (RZ)")
ax.plot(snr, nrz,  "--o", linewidth=2, markersize=4, label="TEB (NRZ)")
ax.plot(snr, nrzt, "-.o", linewidth=2, markersize=4, label="TEB (NRZT)")
ax.plot(snr, pe_theo, "k-", linewidth=2, label="TEB théorique (NRZ binaire)")

ax.set_xlabel("SNRdB (dB)")
ax.set_ylabel("TEB")
ax.set_yscale("log")

# Bornes Y fixées : de 1e-10 à 1
ax.set_ylim(1.98e-5, 1.0)

ax.grid(which="major", linewidth=0.8, alpha=0.3)
ax.grid(which="minor", linewidth=0.5, alpha=0.15)
ax.yaxis.set_minor_locator(LogLocator(base=10.0, subs=(0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9)))
ax.yaxis.set_minor_formatter(NullFormatter())
ax.legend(loc="best")
fig.tight_layout()
plt.show()