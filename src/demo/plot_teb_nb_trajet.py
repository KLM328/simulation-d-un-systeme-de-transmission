#!/usr/bin/env python3
import sys, csv, math, os, glob, re
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.ticker import LogLocator, NullFormatter

# -------------------- I/O --------------------

def charger_csv(path):
    """Retourne dict: {'meta':{...}, 'cols':list, 'data':dict[col]->np.array, 'path':path}"""
    meta = {}
    rows = []
    with open(path, "r", newline="") as f:
        first = f.readline()
        if first.startswith("#"):
            # Parse meta type=..., nbEcho=..., etc.
            try:
                for kv in first[1:].split(","):
                    kv = kv.strip()
                    if "=" in kv:
                        k, v = kv.split("=", 1)
                        meta[k.strip()] = v.strip()
            except Exception:
                pass
        else:
            f.seek(0)
        rdr = csv.DictReader(f)
        cols = rdr.fieldnames
        for r in rdr:
            rows.append(r)
    if not rows:
        return None
    data = {}
    for c in cols:
        try:
            data[c] = np.array([float(r[c]) for r in rows], dtype=float)
        except Exception:
            pass
    return {"meta": meta, "cols": cols, "data": data, "path": path}

# -------------------- Utils --------------------

def plancher_log(arrs):
    vals = np.concatenate([a[a>0] for a in arrs if a is not None and np.any(a>0)]) if arrs else np.array([])
    if vals.size == 0:
        return 1e-6
    return max(float(np.min(vals)/3.0), 1e-7)

def bornes_log_auto(*arrs):
    a = [x for x in arrs if x is not None and x.size>0]
    if not a:
        return 1e-6, 1.0
    y_min = float(min(map(np.min, a)))
    y_max = float(max(map(np.max, a)))
    y_min = max(min(y_min, 1.0), 1e-12)
    y_max = min(max(y_max, 1e-12), 1.0)
    def dec_floor(x): return 10.0**math.floor(math.log10(x))
    def dec_ceil (x): return 10.0**math.ceil (math.log10(x))
    ymin = dec_floor(y_min)
    ymax = min(dec_ceil(y_max*1.2), 1.0)
    if ymin >= ymax:
        ymin = max(y_min/3.0, 1e-6)
        ymax = min(y_max*3.0, 1.0)
    return ymin, ymax

def extraire_K(d):
    """K via méta 'nbEcho', sinon depuis le nom de fichier *K<number>*"""
    metaK = d["meta"].get("nbEcho")
    if metaK is not None:
        try:
            return int(metaK)
        except:
            pass
    m = re.search(r"K(\d+)", os.path.basename(d["path"]))
    if m:
        return int(m.group(1))
    return None

# -------------------- New: 1 figure / K, with 3 modes --------------------

def tracer_par_K_3modes(parK_RZ, parK_NRZ, parK_NRZT):
    """
    parK_*: dict int K -> (x, y) where:
       x = SNRdB array, y = TEB array
    For each K present in any dict, draw RZ/NRZ/NRZT vs SNR on one figure.
    """
    all_K = sorted(set(parK_RZ.keys()) | set(parK_NRZ.keys()) | set(parK_NRZT.keys()))
    if not all_K:
        return False

    for K in all_K:
        plt.figure(figsize=(9,5), dpi=130)

        # Récupère (x,y) pour chaque mode si dispo
        xr, yr = (parK_RZ.get(K)   if K in parK_RZ   else (None, None))
        xn, yn = (parK_NRZ.get(K)  if K in parK_NRZ  else (None, None))
        xz, yz = (parK_NRZT.get(K) if K in parK_NRZT else (None, None))

        # Harmonise SNR (si besoin)
        # Hyp: toutes les séries ont le même x (sinon on trace tel quel)
        if xr is not None and xn is not None and not np.array_equal(xr, xn):
            # pas d'interpolation automatique; on trace comme fourni
            pass
        if xr is not None and xz is not None and not np.array_equal(xr, xz):
            pass

        # Safe floor for log
        floor = plancher_log([yr, yn, yz])
        if yr is not None: yr = np.where(yr>0, yr, floor)
        if yn is not None: yn = np.where(yn>0, yn, floor)
        if yz is not None: yz = np.where(yz>0, yz, floor)

        # Courbes
        if xr is not None and yr is not None: plt.plot(xr, yr, '-o',  linewidth=2, markersize=4, label="RZ")
        if xn is not None and yn is not None: plt.plot(xn, yn, '--o', linewidth=2, markersize=4, label="NRZ")
        if xz is not None and yz is not None: plt.plot(xz, yz, '-.o', linewidth=2, markersize=4, label="NRZT")

        plt.xlabel("SNRpb (dB)")
        plt.ylabel("TEB")
        plt.yscale("log")
        ymin, ymax = bornes_log_auto(*[v for v in [yr, yn, yz] if v is not None])
        plt.ylim(ymin, ymax)

        ax = plt.gca()
        ax.grid(which="major", linewidth=0.8, alpha=0.3)
        ax.grid(which="minor", linewidth=0.5, alpha=0.15)
        ax.yaxis.set_minor_locator(LogLocator(base=10.0, subs=(0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9)))
        ax.yaxis.set_minor_formatter(NullFormatter())

        plt.title(f"TEB vs SNR — K={K} trajets (RZ/NRZ/NRZT)")
        plt.legend(loc="best")
        plt.tight_layout()

    return True

# -------------------- Anciennes figures (facultatives) --------------------

def tracer_snr_par_mode(series_par_k, mode_label):
    """Ancien: figure par mode, une courbe par K (conservé si tu veux)"""
    plt.figure(figsize=(9,5), dpi=130)
    for k in sorted(series_par_k.keys()):
        x, y = series_par_k[k]
        plt.plot(x, y, marker='o', linewidth=2, markersize=4, label=f'K={k}')
    plt.xlabel("SNR (dB)")
    plt.ylabel("TEB")
    plt.yscale("log")
    all_y = [series_par_k[k][1] for k in series_par_k]
    ymin, ymax = bornes_log_auto(*all_y)
    plt.ylim(ymin, ymax)
    ax = plt.gca()
    ax.grid(which="major", linewidth=0.8, alpha=0.3)
    ax.grid(which="minor", linewidth=0.5, alpha=0.15)
    ax.yaxis.set_minor_locator(LogLocator(base=10.0, subs=(0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9)))
    ax.yaxis.set_minor_formatter(NullFormatter())
    plt.title(f"TEB vs SNR — {mode_label}")
    plt.legend(loc="best")
    plt.tight_layout()

def tracer_vs_K(datasets):
    """datasets: liste de dicts avec colonnes 'K','RZ','NRZ','NRZT' """
    plt.figure(figsize=(9,5), dpi=130)
    Ks, rz, nrz, nrzt = [], [], [], []
    for d in datasets:
        K  = d["data"].get("K",  None)
        RZ = d["data"].get("RZ", None)
        NRZ = d["data"].get("NRZ", None)
        NRZT= d["data"].get("NRZT",None)
        if K is None or RZ is None: continue
        Ks.append(K); rz.append(RZ)
        if NRZ is not None: nrz.append(NRZ)
        if NRZT is not None: nrzt.append(NRZT)
    if not Ks:
        return False
    K = np.concatenate(Ks)
    RZ = np.concatenate(rz)
    NRZ = np.concatenate(nrz) if nrz else None
    NRZT= np.concatenate(nrzt) if nrzt else None

    uk = np.unique(K)
    def agg(y):
        if y is None: return None
        out = np.zeros_like(uk, dtype=float)
        for i, kk in enumerate(uk):
            m = (K == kk)
            out[i] = float(np.mean(y[m])) if np.any(m) else np.nan
        return out
    RZg   = agg(RZ)
    NRZg  = agg(NRZ)
    NRZTg = agg(NRZT)

    floor = plancher_log([RZg, NRZg, NRZTg])
    if RZg is not None:   RZg   = np.where(RZg>0,   RZg,   floor)
    if NRZg is not None:  NRZg  = np.where(NRZg>0,  NRZg,  floor)
    if NRZTg is not None: NRZTg = np.where(NRZTg>0, NRZTg, floor)

    if RZg is not None:   plt.plot(uk, RZg,   '-o', linewidth=2, markersize=4, label="RZ")
    if NRZg is not None:  plt.plot(uk, NRZg,  '--o', linewidth=2, markersize=4, label="NRZ")
    if NRZTg is not None: plt.plot(uk, NRZTg, '-.o', linewidth=2, markersize=4, label="NRZT")
    plt.xlabel("Nombre de trajets K")
    plt.ylabel("TEB")
    plt.yscale("log")
    ymin, ymax = bornes_log_auto(*(x for x in [RZg, NRZg, NRZTg] if x is not None))
    plt.ylim(ymin, ymax)
    ax = plt.gca()
    ax.grid(which="major", linewidth=0.8, alpha=0.3)
    ax.grid(which="minor", linewidth=0.5, alpha=0.15)
    ax.yaxis.set_minor_locator(LogLocator(base=10.0, subs=(0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9)))
    ax.yaxis.set_minor_formatter(NullFormatter())
    plt.title("TEB vs Nombre de trajets K")
    plt.legend(loc="best")
    plt.tight_layout()
    return True

def tracer_vs_tau(datasets):
    """datasets: colonnes 'tau','RZ','NRZ','NRZT' """
    Ts, rz, nrz, nrzt = [], [], [], []
    for d in datasets:
        T  = d["data"].get("tau", None)
        RZ = d["data"].get("RZ",  None)
        NRZ = d["data"].get("NRZ", None)
        NRZT= d["data"].get("NRZT",None)
        if T is None or RZ is None: continue
        Ts.append(T); rz.append(RZ)
        if NRZ is not None: nrz.append(NRZ)
        if NRZT is not None: nrzt.append(NRZT)
    if not Ts:
        return False

    T = np.concatenate(Ts)
    RZ = np.concatenate(rz)
    NRZ = np.concatenate(nrz) if nrz else None
    NRZT= np.concatenate(nrzt) if nrzt else None

    ut = np.unique(T)
    def agg(y):
        if y is None: return None
        out = np.zeros_like(ut, dtype=float)
        for i, tt in enumerate(ut):
            m = (T == tt)
            out[i] = float(np.mean(y[m])) if np.any(m) else np.nan
        return out
    RZg   = agg(RZ)
    NRZg  = agg(NRZ)
    NRZTg = agg(NRZT)

    floor = plancher_log([RZg, NRZg, NRZTg])
    if RZg is not None:   RZg   = np.where(RZg>0,   RZg,   floor)
    if NRZg is not None:  NRZg  = np.where(NRZg>0,  NRZg,  floor)
    if NRZTg is not None: NRZTg = np.where(NRZTg>0, NRZTg, floor)

    plt.figure(figsize=(9,5), dpi=130)
    if RZg is not None:   plt.plot(ut, RZg,   '-o', linewidth=2, markersize=4, label="RZ")
    if NRZg is not None:  plt.plot(ut, NRZg,  '--o', linewidth=2, markersize=4, label="NRZ")
    if NRZTg is not None: plt.plot(ut, NRZTg, '-.o', linewidth=2, markersize=4, label="NRZT")
    plt.xlabel("τ (échantillons)")
    plt.ylabel("TEB")
    plt.yscale("log")
    ymin, ymax = bornes_log_auto(*(x for x in [RZg, NRZg, NRZTg] if x is not None))
    plt.ylim(ymin, ymax)
    ax = plt.gca()
    ax.grid(which="major", linewidth=0.8, alpha=0.3)
    ax.grid(which="minor", linewidth=0.5, alpha=0.15)
    ax.yaxis.set_minor_locator(LogLocator(base=10.0, subs=(0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9)))
    ax.yaxis.set_minor_formatter(NullFormatter())
    plt.title("TEB vs τ (retard)")
    plt.legend(loc="best")
    plt.tight_layout()
    return True

# -------------------- Main --------------------

def main():
    args = sys.argv[1:]
    if not args:
        print("Usage: python plot_teb_multi.py out/  (ou liste de .csv)")
        sys.exit(1)

    paths = []
    for a in args:
        if os.path.isdir(a):
            paths.extend(glob.glob(os.path.join(a, "*.csv")))
        else:
            paths.append(a)
    paths = [p for p in paths if os.path.isfile(p)]

    datasets = []
    for p in paths:
        d = charger_csv(p)
        if d is not None:
            datasets.append(d)

    if not datasets:
        print("Aucun CSV lisible.")
        sys.exit(1)

    # Série SNR->TEB : on veut SNRdB + RZ/NRZ/NRZT + K (via méta ou nom)
    snr_datasets = [d for d in datasets if "SNRdB" in d["cols"]]
    parK_RZ, parK_NRZ, parK_NRZT = {}, {}, {}
    for d in snr_datasets:
        K = extraire_K(d)
        if K is None:
            continue
        x = d["data"]["SNRdB"].astype(float)

        floor = plancher_log([d["data"].get("RZ"), d["data"].get("NRZ"), d["data"].get("NRZT")])
        def safe(col):
            y = d["data"].get(col)
            if y is None: return None
            y = y.astype(float)
            return np.where(y>0, y, floor)

        yRZ   = safe("RZ")
        yNRZ  = safe("NRZ")
        yNRZT = safe("NRZT")

        if yRZ   is not None: parK_RZ[K]   = (x, yRZ)
        if yNRZ  is not None: parK_NRZ[K]  = (x, yNRZ)
        if yNRZT is not None: parK_NRZT[K] = (x, yNRZT)

    ok = tracer_par_K_3modes(parK_RZ, parK_NRZ, parK_NRZT)

    # anciennes vues :
    # if parK_RZ:   tracer_snr_par_mode(parK_RZ,   "RZ")
    # if parK_NRZ:  tracer_snr_par_mode(parK_NRZ,  "NRZ")
    # if parK_NRZT: tracer_snr_par_mode(parK_NRZT, "NRZT")

    K_datasets   = [d for d in datasets if "K"   in d["cols"]]
    tau_datasets = [d for d in datasets if "tau" in d["cols"]]
    page4 = False
    if K_datasets:
        page4 = tracer_vs_K(K_datasets)
    elif tau_datasets:
        page4 = tracer_vs_tau(tau_datasets)

    if not (ok or page4):
        print("Aucun groupe exploitable (SNRdB/K/tau). Vérifie les colonnes et les fichiers.")

    plt.show()

if __name__ == "__main__":
    main()
