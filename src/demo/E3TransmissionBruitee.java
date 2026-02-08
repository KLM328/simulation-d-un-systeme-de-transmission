package demo;

import simulateur.Simulateur;
import simulateur.Mode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Calcule TEB vs SNR (RZ/NRZ/NRZT) et enregistre un CSV trouvable dans ./out/ */
public class    E3TransmissionBruitee {

    // --- Plage SNR (en dB) lue depuis "amplitude", comme demandé ---
    static final Float[] amplitude = {-20.0f, 20.0f}; // SNR dB de -20 à +20
    static final int nbPointsSNR = 50;                // nombre de points entre min et max (inclus)

    // --- Paramètres de la chaîne ---
    static final int nbBits   = 200;   // bits par essai
    static final int nbEssais = 100;   // essais par point SNR (moyenne)
    static final int nbEch    = 30;    // échantillons/bit
    static final Float[] amplitudeCodage = {-4.0f, 4.0f}; // amplitude analogique (fixe)

    // --- Dossier / fichier de sortie ---
    static final File DOSSIER_SORTIE = new File("out");
    static final File FICHIER_CSV    = new File(DOSSIER_SORTIE, "teb_snr.csv");

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US); // décimaux avec point
        if (!DOSSIER_SORTIE.exists()) DOSSIER_SORTIE.mkdirs();

        Float[] snrDb = creerPlageSNRdB(amplitude[0], amplitude[1], nbPointsSNR);

        double[] rz   = mesurerPourForme(Mode.RZ,   snrDb);
        double[] nrz  = mesurerPourForme(Mode.NRZ,  snrDb);
        double[] nrzt = mesurerPourForme(Mode.NRZT, snrDb);

        ecrireCSV(FICHIER_CSV, snrDb, rz, nrz, nrzt);
        System.out.println("CSV prêt : " + FICHIER_CSV.getAbsolutePath());
    }

    /** Mesure le TEB moyen pour une forme donnée sur toute la plage SNR. */
    private static double[] mesurerPourForme(Mode forme, Float[] snrDb) throws Exception {
        double[] tebMoyen = new double[snrDb.length];
        for (int i = 0; i < snrDb.length; i++) {
            float snr = snrDb[i];
            double sommeTeb = 0.0;
            for (int essai = 0; essai < nbEssais; essai++) {
                String[] argsSimu = new String[]{
                        "-mess", Integer.toString(nbBits),
                        "-form", forme.name(),
                        "-nbEch", Integer.toString(nbEch),
                        "-ampl", floatStr(amplitudeCodage[0]), floatStr(amplitudeCodage[1]),
                        "-snrpb", floatStr(snr)
                };
                Simulateur simu = new Simulateur(argsSimu);
                simu.execute();
                sommeTeb += simu.calculTauxErreurBinaire();
            }
            tebMoyen[i] = sommeTeb / nbEssais;
        }
        return tebMoyen;
    }

    /** Écrit un CSV combiné : SNRdB,RZ,NRZ,NRZT + ligne #meta en tête. */
    private static void ecrireCSV(File fichier, Float[] snrDb, double[] rz, double[] nrz, double[] nrzt)
            throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8))) {
            bw.write("# nbBitsParEssai=" + nbBits + ", nbEssais=" + nbEssais + ", nbEch=" + nbEch
                    + ", amplitudeCodage=[" + amplitudeCodage[0] + "," + amplitudeCodage[1] + "]\n");
            bw.write("SNRdB,RZ,NRZ,NRZT\n");
            for (int i = 0; i < snrDb.length; i++) {
                bw.write(String.format(Locale.US, "%.6f,%.6e,%.6e,%.6e%n", snrDb[i], rz[i], nrz[i], nrzt[i]));
            }
        }
    }

    // --------- utilitaires ---------
    private static Float[] creerPlageSNRdB(float debut, float fin, int n) {
        Float[] v = new Float[n];
        if (n <= 1) { v[0] = debut; return v; }
        float pas = (fin - debut) / (n - 1);
        for (int i = 0; i < n; i++) v[i] = debut + i * pas;
        return v;
    }
    private static String floatStr(float f) {
        if (Float.isFinite(f) && Math.rint(f) == f) return Integer.toString((int) f);
        return Float.toString(f);
    }
}
