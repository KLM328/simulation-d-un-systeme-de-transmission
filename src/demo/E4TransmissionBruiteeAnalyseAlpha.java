package demo;

import simulateur.Mode;
import simulateur.Simulateur;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** TEB(α) avec un écho d’amplitude α et retard τ aléatoire à chaque essai.
 *  Trajet direct a0 = sqrt(1-α^2), écho = α ; SNR fixé. Résultat: out/teb_alpha.csv. */
public class E4TransmissionBruiteeAnalyseAlpha {

    // --- Paramètres chaîne / Monte-Carlo ---
    static final int nbBitsParEssai = 1000;     // bits par essai
    static final int nbEssais       = 100;      // essais par point alpha
    static final int nbEch          = 30;       // échantillons/bit
    static final Float[] amplitudeCodage = {-4.0f, 4.0f};

    // --- Balayage alpha ---
    static final float alphaMin = 0.0f;
    static final float alphaMax = 0.9f;         // garder < 1 pour a0 = sqrt(1-alpha^2)
    static final int   nbPointsAlpha = 31;      // par ex. pas ≈ 0.03

    // --- Retard aléatoire de l’écho (en échantillons) ---
    static final int dtMax = 25;                // 1..dtMax, typiquement <= nbEch

    // --- SNR fixé (dB) pour l’expérience ---
    static final float snrpb_dB = 3.0f;

    // --- Sortie ---
    static final File DOSSIER_SORTIE = new File("out");
    static final File FICHIER_CSV    = new File(DOSSIER_SORTIE, "teb_alpha.csv");

    // --- Graine pour reproductibilité ---
    static final long graineBase = 20251003L;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        if (!DOSSIER_SORTIE.exists()) DOSSIER_SORTIE.mkdirs();

        float[] alphas = creerPlageAlpha(alphaMin, alphaMax, nbPointsAlpha);

        double[] rz   = mesurerPourForme(Mode.RZ,   alphas);
        double[] nrz  = mesurerPourForme(Mode.NRZ,  alphas);
        double[] nrzt = mesurerPourForme(Mode.NRZT, alphas);

        ecrireCSV(FICHIER_CSV, alphas, rz, nrz, nrzt);
        System.out.println("CSV prêt : " + FICHIER_CSV.getAbsolutePath());
    }

    /** Mesure TEB moyen pour une forme donnée sur toute la plage d'alpha. */
    private static double[] mesurerPourForme(Mode forme, float[] alphas) throws Exception {
        double[] tebMoyen = new double[alphas.length];
        Random rng = new Random(graineBase + 10_000L * forme.ordinal());

        for (int i = 0; i < alphas.length; i++) {
            float alpha = alphas[i];
            // amplitude du trajet direct normalisée
            float a0 = (float) Math.sqrt(Math.max(0.0, 1.0 - alpha * alpha));
            double sommeTeb = 0.0;

            for (int essai = 0; essai < nbEssais; essai++) {
                // τ aléatoire (en échantillons) pour l’écho
                int dt = 1 + rng.nextInt(Math.max(1, dtMax));

                // arguments simulateur : direct puis écho
                ArrayList<String> argsList = new ArrayList<>();
                argsList.addAll(Arrays.asList(
                        "-mess", Integer.toString(nbBitsParEssai),
                        "-form", forme.name(),
                        "-nbEch", Integer.toString(nbEch),
                        "-ampl", floatStr(amplitudeCodage[0]), floatStr(amplitudeCodage[1]),
                        "-snrpb", floatStr(snrpb_dB),
                        "-ti", "0", floatStr(a0),         // trajet direct
                        "-ti", Integer.toString(dt), floatStr(alpha) // écho aléatoire
                ));

                Simulateur simu = new Simulateur(argsList.toArray(new String[0]));
                simu.execute();
                sommeTeb += simu.calculTauxErreurBinaire();
            }
            tebMoyen[i] = sommeTeb / nbEssais;
        }
        return tebMoyen;
    }

    /** Écrit un CSV : colonnes alpha,RZ,NRZ,NRZT + meta (#...) */
    private static void ecrireCSV(File fichier, float[] alphas, double[] rz, double[] nrz, double[] nrzt)
            throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8))) {
            bw.write(("# type=ALPHA, nbBitsParEssai=%d, nbEssais=%d, nbEch=%d, ampl=[%.2f,%.2f], snrpb=%.2f, dtMax=%d%n")
                    .formatted(nbBitsParEssai, nbEssais, nbEch, amplitudeCodage[0], amplitudeCodage[1], snrpb_dB, dtMax));
            bw.write("alpha,RZ,NRZ,NRZT\n");
            for (int i = 0; i < alphas.length; i++) {
                bw.write(String.format(Locale.US, "%.6f,%.6e,%.6e,%.6e%n",
                        alphas[i], rz[i], nrz[i], nrzt[i]));
            }
        }
    }

    // ---------- utilitaires ----------
    private static float[] creerPlageAlpha(float debut, float fin, int n) {
        float[] v = new float[n];
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
