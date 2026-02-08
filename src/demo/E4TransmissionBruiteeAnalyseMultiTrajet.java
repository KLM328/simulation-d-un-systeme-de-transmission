package demo;

import information.ParametreTrajet;
import simulateur.Mode;
import simulateur.Simulateur;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Génère des CSV TEB vs SNR pour K trajets (K_echo = 0..5) et 3 modes (RZ/NRZ/NRZT).
 * Un fichier par K : out/teb_snr_K{K}.csv  (colonnes: SNRdB,RZ,NRZ,NRZT)
 * Les trajets (t_i, a_i) sont tirés aléatoirement à chaque essai.
 */
public class E4TransmissionBruiteeAnalyseMultiTrajet {

    // --------- Paramètres SNR (dB) ---------
    static final float snrMin = -20f;
    static final float snrMax = 20f;
    static final int   nbPointsSNR = 50; // pas de 1 dB

    // --------- Paramètres chaîne / Monte-Carlo ---------
    static final int nbBitsParEssai = 2000; // bits/essai
    static final int nbEssais       = 100;   // essais/point SNR
    static final int nbEch          = 30;   // échantillons/bit
    static final Float[] amplitudeCodage = {-4f, 4f}; // amplitude analogique
    // # d'échos à tester (0..max). K_total = 1 (trajet direct) + nbEcho
    static final int nbEchoMax = 5;

    // --------- Canal multi-trajets aléatoire ---------
    static final int    dtMax    = 25;    // retard max (échantillons)
    static final double rhoEcho  = 0.50;  // fraction d'énergie allouée aux échos (reste pour le direct)
    static final long   graineBase = 20251002L;

    // --------- Fichiers ---------
    static final File DOSSIER_SORTIE = new File("out");

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        if (!DOSSIER_SORTIE.exists()) DOSSIER_SORTIE.mkdirs();

        Float[] snrDb = creerPlageSNRdB(snrMin, snrMax, nbPointsSNR);

        for (int nbEcho = 0; nbEcho <= nbEchoMax; nbEcho++) {
            System.out.println("=== Génération K=" + nbEcho + " échos ===");

            double[] rz   = mesurerPourFormeAvecK(Mode.RZ,   snrDb, nbEcho);
            double[] nrz  = mesurerPourFormeAvecK(Mode.NRZ,  snrDb, nbEcho);
            double[] nrzt = mesurerPourFormeAvecK(Mode.NRZT, snrDb, nbEcho);

            File csv = new File(DOSSIER_SORTIE, "teb_snr_K" + nbEcho + ".csv");
            ecrireCSV_SNR(csv, snrDb, rz, nrz, nrzt, nbEcho);
            System.out.println("  -> " + csv.getAbsolutePath());
        }

        System.out.println("\nCSV générés dans : " + DOSSIER_SORTIE.getAbsolutePath());
    }

    /** Mesure TEB moyen pour une forme et un nombre d'échos nbEcho (0..5) sur la plage SNR. */
    private static double[] mesurerPourFormeAvecK(Mode forme, Float[] snrDb, int nbEcho) throws Exception {
        double[] tebMoyen = new double[snrDb.length];
        Random rng = new Random();

        for (int i = 0; i < snrDb.length; i++) {
            float snr = snrDb[i];
            double sommeTeb = 0.0;

            for (int essai = 0; essai < nbEssais; essai++) {
                long graineEssai = rng.nextLong();

                // Génère les trajets : 1 direct + nbEcho échos aléatoires
                ParametreTrajet[] trajets = genererTrajetsAleatoires(nbEcho, graineEssai);

                // Construit args Simulateur (-ti répété)
                ArrayList<String> argsList = new ArrayList<>(16 + 3 * (1 + nbEcho));
                argsList.addAll(Arrays.asList(
                        "-mess", Integer.toString(nbBitsParEssai),
                        "-form", forme.name(),
                        "-nbEch", Integer.toString(nbEch),
                        "-ampl", floatStr(amplitudeCodage[0]), floatStr(amplitudeCodage[1]),
                        "-snrpb", floatStr(snr)
                ));
                for (ParametreTrajet p : trajets) {
                    argsList.add("-ti");
                    argsList.add(Integer.toString(p.getTau())); // token 1
                    argsList.add(floatStr(p.getAlpha()));         // token 2
                }

                Simulateur simu = new Simulateur(argsList.toArray(new String[0]));
                simu.execute();
                sommeTeb += simu.calculTauxErreurBinaire();
            }
            tebMoyen[i] = sommeTeb / nbEssais;
        }
        return tebMoyen;
    }

    /** Génère 1 direct (dt=0, a0=√(1-ρ)) + nbEcho échos avec dt∈[1..dtMax], amplitudes telles que Σ a_i^2 = ρ. */
    private static ParametreTrajet[] genererTrajetsAleatoires(int nbEcho, long graine) {
        ArrayList<ParametreTrajet> liste = new ArrayList<>(1 + nbEcho);

        // Trajet direct :
        float a0 = (float) Math.sqrt(Math.max(0.0, 1.0 - rhoEcho));
        //liste.add(new ParametreTrajet(0, a0));

        if (nbEcho == 0) return liste.toArray(new ParametreTrajet[0]);

        Random rng = new Random(graine);

        // Tirage retards distincts
        ArrayList<Integer> candidats = new ArrayList<>();
        for (int d = 1; d <= dtMax; d++) candidats.add(d);
        Collections.shuffle(candidats, rng);
        List<Integer> dts = candidats.subList(0, Math.min(nbEcho, candidats.size()));

        // Tirage amplitudes brutes puis normalisation en énergie ρ
        double[] w = new double[dts.size()];
        for (int i = 0; i < w.length; i++) {
            // loi exp pour décroissance, clamp min
            double u = Math.max(1e-3, -Math.log(1.0 - Math.max(1e-9, rng.nextDouble())));
            w[i] = u;
        }
        double norme2 = 0.0; for (double x : w) norme2 += x * x;
        double s = (norme2 > 0) ? Math.sqrt(rhoEcho / norme2) : Math.sqrt(rhoEcho / w.length);

        for (int i = 0; i < dts.size(); i++) {
            int dt = dts.get(i);
            float ar = (float) Math.min(0.999, w[i] * s); // petite borne
            liste.add(new ParametreTrajet(dt, ar));
        }

        // Tri par dt (lisibilité)
        liste.sort(Comparator.comparingInt(ParametreTrajet::getTau));
        return liste.toArray(new ParametreTrajet[0]);
    }

    /** Écrit un CSV SNR : colonnes SNRdB,RZ,NRZ,NRZT + meta (#...) avec K=nbEcho. */
    private static void ecrireCSV_SNR(File fichier, Float[] snrDb, double[] rz, double[] nrz, double[] nrzt, int nbEcho)
            throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8))) {
            bw.write(("# type=SNR, nbEcho=%d, nbBitsParEssai=%d, nbEssais=%d, nbEch=%d, ampl=[%.3f,%.3f], dtMax=%d, rhoEcho=%.2f%n")
                    .formatted(nbEcho, nbBitsParEssai, nbEssais, nbEch, amplitudeCodage[0], amplitudeCodage[1], dtMax, rhoEcho));
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
