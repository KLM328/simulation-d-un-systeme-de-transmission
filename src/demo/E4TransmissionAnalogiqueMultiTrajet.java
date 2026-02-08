package demo;

import simulateur.Simulateur;
import simulateur.Mode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/** Calcule TEB vs SNR (RZ/NRZ/NRZT) et enregistre un CSV trouvable dans ./out/ */
public class E4TransmissionAnalogiqueMultiTrajet {

    // --- Paramètres de la chaîne ---
    static final int nbBits   = 1000;   // bits par essai
    static final int nbEssais = 100;   // essais par point SNR (moyenne)
    static final int nbEch    = 30;    // échantillons/bit
    static final Float[] amplitudeCodage = {-4.0f, 4.0f}; // amplitude analogique (fixe)

    // --- Plage tau lue depuis "amplitude", comme demandé ---
    static final Integer[] amplitude = {0, nbBits*nbEch};
    static final int nbPointsTau = 50;                // nombre de points entre min et max (inclus)
    static final float alpha = 0.3f;

    static final float snrpb = 3f;

    // --- Dossier / fichier de sortie ---
    static final File DOSSIER_SORTIE = new File("out");
    static final File FICHIER_CSV    = new File(DOSSIER_SORTIE, "teb_tau.csv");

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US); // décimaux avec point
        if (!DOSSIER_SORTIE.exists()) DOSSIER_SORTIE.mkdirs();

        Integer[] tau = creerPlagetau(amplitude[0], amplitude[1], nbPointsTau);

        double[] rz   = mesurerPourForme(Mode.RZ, tau);
        double[] nrz  = mesurerPourForme(Mode.NRZ, tau);
        double[] nrzt = mesurerPourForme(Mode.NRZT, tau);

        ecrireCSV(FICHIER_CSV, tau, rz, nrz, nrzt);
        System.out.println("CSV prêt : " + FICHIER_CSV.getAbsolutePath());
    }

    /** Mesure le TEB moyen pour une forme donnée sur toute la plage SNR. */
    private static double[] mesurerPourForme(Mode forme, Integer[] tau) throws Exception {
        double[] tebMoyen = new double[tau.length];
        for (int i = 0; i < tau.length; i++) {
            float t = tau[i];
            double sommeTeb = 0.0;
            for (int essai = 0; essai < nbEssais; essai++) {
                String[] argsSimu = new String[]{
                        "-mess", Integer.toString(nbBits),
                        "-form", forme.name(),
                        "-nbEch", Integer.toString(nbEch),
                        "-ampl", floatStr(amplitudeCodage[0]), floatStr(amplitudeCodage[1]),
                        "-ti", floatStr(t), floatStr(alpha),
                        "-snrpb", floatStr(snrpb)
                };
//                System.out.println("-ti " + floatStr(t) + " "+  floatStr(alpha));

                Simulateur simu = new Simulateur(argsSimu);
                simu.execute();
                sommeTeb += simu.calculTauxErreurBinaire();
            }
            tebMoyen[i] = sommeTeb / nbEssais;
        }
        return tebMoyen;
    }

    /**
     * Écrit un CSV combiné : tau,RZ,NRZ,NRZT + ligne #meta en tête.
     */
    private static void ecrireCSV(File fichier, Integer[] tau, double[] rz, double[] nrz, double[] nrzt)
            throws IOException {
        // Vérification des entrées
        Objects.requireNonNull(fichier, "Le fichier ne peut pas être null.");
        Objects.requireNonNull(tau, "Le tableau tau ne peut pas être null.");
        Objects.requireNonNull(rz, "Le tableau rz ne peut pas être null.");
        Objects.requireNonNull(nrz, "Le tableau nrz ne peut pas être null.");
        Objects.requireNonNull(nrzt, "Le tableau nrzt ne peut pas être null.");

        if (tau.length != rz.length || tau.length != nrz.length || tau.length != nrzt.length) {
            throw new IllegalArgumentException("Tous les tableaux doivent avoir la même longueur.");
        }

        // Écriture du fichier
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8))) {
            // Ligne de métadonnées
            bw.write(String.format("# nbBitsParEssai=%d, nbEssais=%d, nbEch=%d, amplitudeCodage=[%.2f,%.2f]%n",
                    nbBits, nbEssais, nbEch, amplitudeCodage[0], amplitudeCodage[1]));
            // En-tête des colonnes
            bw.write("tau,RZ,NRZ,NRZT\n");
            // Données
            for (int i = 0; i < tau.length; i++) {
                bw.write(String.format(Locale.US, "%d,%.6e,%.6e,%.6e%n", tau[i], rz[i], nrz[i], nrzt[i]));
            }
        }
    }

    // --------- utilitaires ---------
    private static Integer[] creerPlagetau(int debut, int fin, int n) {
        Integer[] v = new Integer[n];
        if (n <= 1) { v[0] = debut; return v; }
        int pas = (fin - debut) / (n - 1);
        for (int i = 0; i < n; i++) v[i] = debut + i * pas;
        return v;
    }
    private static String floatStr(float f) {
        if (Float.isFinite(f) && Math.rint(f) == f) return Integer.toString((int) f);
        return Float.toString(f);
    }
}
