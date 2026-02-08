import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
from mpl_toolkits.mplot3d import Axes3D

# Charger les données
df = pd.read_csv('out/resultat_simulation_environnement2.csv')

# Calculer le débit binaire (Rb = 1/Tb * nbEch)
# Tb = 1 (donné dans le code Java)
df['debit_binaire'] = df['nbEch'] / 1.0  # en bits/s

# Calculer Eb/N0 à partir du SNR mesuré
# Relation: SNR = Eb/N0 + 10*log10(nbEch) - 3
# Donc: Eb/N0 = SNR - 10*log10(nbEch) + 3
df['ebno'] = df['snr'] - 10*np.log10(df['nbEch']) + 3

# Question 4: Trouver le débit maximal avec TEB <= 10^-2 et Eb/N0 <= 15 dB
# Filtrer les données selon les contraintes
contrainte_teb = df['tauxErreurBinaire'] <= 0.01  # TEB <= 10^-2
contrainte_snr = df['ebno'] <= 15  # Eb/N0 <= 15 dB

df_filtre = df[contrainte_teb & contrainte_snr]

# Trouver le débit maximal pour chaque mode
print("=" * 80)
print("RÉPONSE À LA QUESTION 4")
print("=" * 80)
print("\nConditions: Canal avec 2 trajets (τ=10µs, amplitudes: 1 et 0.5)")
print("Contraintes: TEB ≤ 10^-2 et Eb/N0 ≤ 15 dB\n")

for mode in ['NRZ', 'NRZT']:
    df_mode = df_filtre[df_filtre['Mode'] == mode]
    if len(df_mode) > 0:
        idx_max = df_mode['debit_binaire'].idxmax()
        debit_max = df_mode.loc[idx_max, 'debit_binaire']
        nbEch_opt = df_mode.loc[idx_max, 'nbEch']
        ebno_opt = df_mode.loc[idx_max, 'ebno']
        snr_opt = df_mode.loc[idx_max, 'snr']
        teb_opt = df_mode.loc[idx_max, 'tauxErreurBinaire']

        print(f"Mode {mode}:")
        print(f"  - Débit binaire maximal: {debit_max:.2f} bits/s")
        print(f"  - Nombre d'échantillons: {nbEch_opt}")
        print(f"  - Eb/N0: {ebno_opt:.2f} dB")
        print(f"  - SNR mesuré: {snr_opt:.2f} dB")
        print(f"  - TEB obtenu: {teb_opt:.2e}")
        print()

# Meilleur mode global
if len(df_filtre) > 0:
    idx_global = df_filtre['debit_binaire'].idxmax()
    meilleur_mode = df_filtre.loc[idx_global, 'Mode']
    debit_global = df_filtre.loc[idx_global, 'debit_binaire']
    ebno_global = df_filtre.loc[idx_global, 'ebno']
    teb_global = df_filtre.loc[idx_global, 'tauxErreurBinaire']
    print(f">>> Proposition: Mode {meilleur_mode} avec un débit de {debit_global:.2f} bits/s")
    print(f"    (Eb/N0 = {ebno_global:.2f} dB, TEB = {teb_global:.2e})")
else:
    print(">>> Aucune configuration ne satisfait les contraintes!")

print("\n" + "=" * 80)

# ========== VISUALISATIONS ==========

# 1. TEB en fonction du débit pour différents Eb/N0
fig1, axes = plt.subplots(1, 2, figsize=(15, 5))

for idx, mode in enumerate(['NRZ', 'NRZT']):
    ax = axes[idx]
    df_mode = df[df['Mode'] == mode]

    # Sélectionner quelques valeurs de SNR représentatives
    ebno_values = [0, 5, 10, 15]

    for ebno_val in ebno_values:
        df_ebno = df_mode[abs(df_mode['ebno'] - ebno_val) < 0.5]
        if len(df_ebno) > 0:
            ax.semilogy(df_ebno['debit_binaire'], df_ebno['tauxErreurBinaire'],
                        marker='o', label=f'Eb/N0={ebno_val} dB', alpha=0.7)

    ax.axhline(y=0.01, color='r', linestyle='--', linewidth=2, label='TEB=10^-2')
    ax.set_xlabel('Débit binaire (bits/s)', fontsize=12)
    ax.set_ylabel('Taux d\'Erreur Binaire (TEB)', fontsize=12)
    ax.set_title(f'TEB vs Débit - Mode {mode}', fontsize=14, fontweight='bold')
    ax.grid(True, alpha=0.3)
    ax.legend()
    ax.set_ylim([1e-4, 1])

plt.tight_layout()
plt.savefig('out/teb_vs_debit_env2.png', dpi=300, bbox_inches='tight')
print("\n📊 Graphique 1 sauvegardé: teb_vs_debit_env2.png")

# 2. Carte de performance 2D (Débit vs Eb/N0) avec contours TEB
fig2, axes = plt.subplots(1, 2, figsize=(15, 6))

for idx, mode in enumerate(['NRZ', 'NRZT']):
    ax = axes[idx]
    df_mode = df[df['Mode'] == mode]

    # Créer une grille pour le contour
    debit = df_mode['debit_binaire'].values
    ebno = df_mode['ebno'].values
    teb = df_mode['tauxErreurBinaire'].values

    # Créer un scatter plot avec colormap
    scatter = ax.scatter(debit, ebno, c=np.log10(teb + 1e-10),
                         cmap='RdYlGn_r', s=50, alpha=0.6, edgecolors='black', linewidth=0.5)

    # Ajouter la zone de contrainte
    ax.axhline(y=15, color='red', linestyle='--', linewidth=2, label='Eb/N0 max = 15 dB')

    # Colorbar
    cbar = plt.colorbar(scatter, ax=ax)
    cbar.set_label('log10(TEB)', fontsize=10)

    ax.set_xlabel('Débit binaire (bits/s)', fontsize=12)
    ax.set_ylabel('Eb/N0 (dB)', fontsize=12)
    ax.set_title(f'Carte de Performance - Mode {mode}', fontsize=14, fontweight='bold')
    ax.grid(True, alpha=0.3)
    ax.legend()

plt.tight_layout()
plt.savefig('out/carte_performance_env2.png', dpi=300, bbox_inches='tight')
print("📊 Graphique 2 sauvegardé: carte_performance_env2.png")

# 3. Surface 3D: TEB = f(Débit, Eb/N0)
fig3 = plt.figure(figsize=(16, 6))

for idx, mode in enumerate(['NRZ', 'NRZT']):
    ax = fig3.add_subplot(1, 2, idx+1, projection='3d')
    df_mode = df[df['Mode'] == mode]

    debit = df_mode['debit_binaire'].values
    ebno = df_mode['ebno'].values
    teb = df_mode['tauxErreurBinaire'].values

    # Plot surface
    surf = ax.scatter(debit, ebno, teb, c=teb, cmap='RdYlGn_r',
                      s=30, alpha=0.6, edgecolors='black', linewidth=0.3)

    # Plan TEB = 10^-2
    xx, yy = np.meshgrid(np.linspace(debit.min(), debit.max(), 10),
                         np.linspace(ebno.min(), ebno.max(), 10))
    zz = np.ones_like(xx) * 0.01
    ax.plot_surface(xx, yy, zz, alpha=0.2, color='red')

    ax.set_xlabel('Débit (bits/s)', fontsize=10)
    ax.set_ylabel('Eb/N0 (dB)', fontsize=10)
    ax.set_zlabel('TEB', fontsize=10)
    ax.set_title(f'Surface TEB - Mode {mode}', fontsize=12, fontweight='bold')
    ax.view_init(elev=20, azim=45)

    # Colorbar
    plt.colorbar(surf, ax=ax, shrink=0.5, aspect=5)

plt.tight_layout()
plt.savefig('out/surface_3d_env2.png', dpi=300, bbox_inches='tight')
print("📊 Graphique 3 sauvegardé: surface_3d_env2.png")

# 4. Comparaison NRZ vs NRZT
fig4, axes = plt.subplots(2, 2, figsize=(15, 10))

# a) TEB vs Eb/N0 pour différents débits
ax = axes[0, 0]
debits_test = [30, 60, 100, 150, 200]
for debit in debits_test:
    for mode in ['NRZ', 'NRZT']:
        df_temp = df[(df['Mode'] == mode) & (abs(df['debit_binaire'] - debit) < 2)]
        if len(df_temp) > 0:
            style = '-' if mode == 'NRZ' else '--'
            ax.semilogy(df_temp['ebno'], df_temp['tauxErreurBinaire'],
                        style, marker='o', label=f'{mode}, D={debit} bits/s', alpha=0.7)

ax.axhline(y=0.01, color='r', linestyle='--', linewidth=2, label='TEB=10^-2')
ax.set_xlabel('Eb/N0 (dB)', fontsize=11)
ax.set_ylabel('TEB', fontsize=11)
ax.set_title('TEB vs Eb/N0 pour différents débits', fontsize=12, fontweight='bold')
ax.grid(True, alpha=0.3)
ax.legend(fontsize=8)

# b) Zone de fonctionnement acceptable
ax = axes[0, 1]
for mode in ['NRZ', 'NRZT']:
    df_ok = df_filtre[df_filtre['Mode'] == mode]
    marker = 'o' if mode == 'NRZ' else 's'
    ax.scatter(df_ok['debit_binaire'], df_ok['ebno'],
               label=f'{mode} (OK)', alpha=0.6, s=40, marker=marker)

ax.axhline(y=15, color='r', linestyle='--', linewidth=2, label='Eb/N0 max')
ax.set_xlabel('Débit binaire (bits/s)', fontsize=11)
ax.set_ylabel('Eb/N0 (dB)', fontsize=11)
ax.set_title('Zone de fonctionnement (TEB ≤ 10^-2)', fontsize=12, fontweight='bold')
ax.grid(True, alpha=0.3)
ax.legend()

# c) Histogramme des débits atteignables
ax = axes[1, 0]
for mode in ['NRZ', 'NRZT']:
    df_mode = df_filtre[df_filtre['Mode'] == mode]
    if len(df_mode) > 0:
        debits_ok = df_mode['debit_binaire'].unique()
        ax.hist(debits_ok, alpha=0.6, label=f'{mode}', bins=20)

ax.set_xlabel('Débit binaire (bits/s)', fontsize=11)
ax.set_ylabel('Nombre de configurations', fontsize=11)
ax.set_title('Distribution des débits atteignables', fontsize=12, fontweight='bold')
ax.legend()
ax.grid(True, alpha=0.3, axis='y')

# d) TEB moyen par débit
ax = axes[1, 1]
for mode in ['NRZ', 'NRZT']:
    df_mode = df[df['Mode'] == mode]
    teb_moyen = df_mode.groupby('debit_binaire')['tauxErreurBinaire'].mean()
    style = '-' if mode == 'NRZ' else '--'
    ax.semilogy(teb_moyen.index, teb_moyen.values, style, marker='o',
                label=f'{mode}', linewidth=2, markersize=4)

ax.axhline(y=0.01, color='r', linestyle='--', linewidth=2, label='TEB=10^-2')
ax.set_xlabel('Débit binaire (bits/s)', fontsize=11)
ax.set_ylabel('TEB moyen', fontsize=11)
ax.set_title('TEB moyen vs Débit', fontsize=12, fontweight='bold')
ax.legend()
ax.grid(True, alpha=0.3)

plt.tight_layout()
plt.savefig('out/comparaison_modes_env2.png', dpi=300, bbox_inches='tight')
print("📊 Graphique 4 sauvegardé: comparaison_modes_env2.png")

# 5. Tableau récapitulatif
print("\n" + "=" * 80)
print("TABLEAU RÉCAPITULATIF DES PERFORMANCES")
print("=" * 80)

summary_data = []
for mode in ['NRZ', 'NRZT']:
    df_mode = df_filtre[df_filtre['Mode'] == mode]
    if len(df_mode) > 0:
        summary_data.append({
            'Mode': mode,
            'Débit Max (bits/s)': df_mode['debit_binaire'].max(),
            'Débit Moyen (bits/s)': df_mode['debit_binaire'].mean(),
            'Nb Config OK': len(df_mode),
            'TEB Min': df_mode['tauxErreurBinaire'].min(),
            'TEB Moyen': df_mode['tauxErreurBinaire'].mean()
        })

df_summary = pd.DataFrame(summary_data)
print("\n", df_summary.to_string(index=False))
print("\n" + "=" * 80)

plt.show()
print("\n✅ Analyse terminée!")