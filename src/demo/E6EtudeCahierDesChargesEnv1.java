package demo;

import simulateur.Mode;
import simulateur.Simulateur;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Locale;

public class E6EtudeCahierDesChargesEnv1 {

    public static Float attenuationDb = 40f;
    public static Float n0 = -80f; // dBm / Hz
    static int nbBits = 200;
    static int nbEch = 30;
    static Float[] amplitudeCodage = {-4.0f, 4.0f};
    static long seed = 20251005L;

    static final int bitJour = 1_000_000;  // bits d'information / jour
    static final double batterieJoules = 3.0;
    static int nbEssais = 100;

    static final File dossierSortie = new File("out");

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        if (!dossierSortie.exists()) dossierSortie.mkdirs();

        LinkedList<Float[]> plage = creerPlageSNRdB(-20f, 20f, 100);

        // Mesure TEB pour chaque forme, une seule fois
        runCampagne(plage, false); // uncoded
        runCampagne(plage, true);  // codé canal (R=1/3)
    }

    private static void runCampagne(LinkedList<Float[]> plage, boolean codageOn) throws Exception {
        double[] rz   = mesurerPourForme(Mode.RZ,   plage, codageOn);
        double[] nrz  = mesurerPourForme(Mode.NRZ,  plage, codageOn);
        double[] nrzt = mesurerPourForme(Mode.NRZT, plage, codageOn);

        String suffix = codageOn ? "_coded_R-1_3" : "_uncoded";
        File fichierCSV = new File(dossierSortie, "teb_snr_env1" + suffix + ".csv");
        ecrireCSV(fichierCSV, plage, rz, nrz, nrzt, codageOn);
        System.out.println("CSV prêt : " + fichierCSV.getAbsolutePath());
    }

    /** Mesure le TEB moyen pour une forme donnée sur toute la plage SNR. */
    private static double[] mesurerPourForme(Mode forme, LinkedList<Float[]> snrDb, boolean codageOn) throws Exception {
        double[] tebMoyen = new double[snrDb.size()];
        for (int i = 0; i < snrDb.size(); i++) {
            float snr = snrDb.get(i)[1];
            double sommeTeb = 0.0;
            for (int essai = 0; essai < nbEssais; essai++) {
                String[] argsSimu = new String[]{
                        "-mess", Integer.toString(nbBits),
                        "-form", forme.name(),
                        "-nbEch", Integer.toString(nbEch),
                        "-ampl", floatStr(amplitudeCodage[0]), floatStr(amplitudeCodage[1]),
                        "-snrpb", floatStr(snr),
                        "-seed", Long.toString(seed + essai),
                };
                if (codageOn) {
                    // on étend le tableau avec l’option -codeur
                    String[] withCodeur = new String[argsSimu.length + 1];
                    System.arraycopy(argsSimu, 0, withCodeur, 0, argsSimu.length);
                    withCodeur[withCodeur.length - 1] = "-codeur";
                    argsSimu = withCodeur;
                }

                Simulateur simu = new Simulateur(argsSimu);
                simu.execute();
                sommeTeb += simu.calculTauxErreurBinaire(); // TEB post-décodage si -codage on
            }
            tebMoyen[i] = sommeTeb / nbEssais;
        }
        return tebMoyen;
    }

    private static double dbmToW(double dbm) {
        return Math.pow(10.0, (dbm - 30.0) / 10.0); // W
    }

    /** Eb_tx (J/bit transmis sur le canal) à partir de SNRpb (Eb/N0) + N0 + atténuation */
    private static double ebTxJPerBit(double snrDb) {
        double EbN0 = Math.pow(10.0, snrDb / 10.0);
        double N0   = dbmToW(n0);
        double L    = Math.pow(10.0, attenuationDb / 10.0);
        return EbN0 * N0 * L;
    }

    /** Écrit un CSV : SNRdB,E,RZ,NRZ,NRZT,Eb_tx (J/bit),Energie consommée (par jour),Duree batterie (jour) */
    private static void ecrireCSV(File fichier, LinkedList<Float[]> snrDb,
                                  double[] rz, double[] nrz, double[] nrzt, boolean codageOn) throws IOException {

        // Taux de code (mapping 1 bit -> 3 bits transmis)
        double R = codageOn ? (1.0/3.0) : 1.0;

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8))) {

            bw.write("# nbBitsParEssai=" + nbBits + ", nbEssais=" + nbEssais + ", nbEch=" + nbEch
                    + ", amplitudeCodage=[" + amplitudeCodage[0] + "," + amplitudeCodage[1] + "]"
                    + ", N0=" + n0 + " dBm/Hz, Attenuation(dB)=" + attenuationDb
                    + ", bits_info_par_jour=" + bitJour + ", batterie(J)=" + batterieJoules
                    + ", codage=" + (codageOn ? "ON" : "OFF")
                    + ", R=" + R + "\n");

            bw.write("SNRdB,E,RZ,NRZ,NRZT,Eb_tx (J/bit),Energie consommée (par jour),Duree batterie (jour)\n");

            for (int i = 0; i < snrDb.size(); i++) {
                double snrDbVal = snrDb.get(i)[0];

                // énergie / bit transmis (physique), indépendante du codage
                double ebTxJ     = ebTxJPerBit(snrDbVal);

                // si codage actif et que l’objectif est 1e6 bits d'INFORMATION/jour,
                // alors on transmet (bitJour / R) bits sur le canal (surdébit)
                double bitsCanalParJour = bitJour / R;

                double eDayJ     = ebTxJ * bitsCanalParJour;
                double days      = (eDayJ > 0) ? (batterieJoules / eDayJ) : Double.POSITIVE_INFINITY;

                bw.write(String.format(Locale.US,
                        "%.6f,%.6f,%.6e,%.6e,%.6e,%.6e,%.6e,%.6e%n",
                        snrDbVal,
                        snrDbVal,
                        rz[i], nrz[i], nrzt[i],
                        ebTxJ, eDayJ, days
                ));
            }
        }
    }

    private static LinkedList<Float[]> creerPlageSNRdB(float snrDebut, float snrFin, int nPoints) {
        LinkedList<Float[]> list = new LinkedList<>();
        if (nPoints <= 1) { list.add(new Float[]{snrDebut, snrFin}); return list; }
        float step = (snrFin - snrDebut) / (nPoints - 1);
        for (int i = 0; i < nPoints; i++) {
            float v = snrDebut + i * step;
            list.add(new Float[]{v, v});
        }
        return list;
    }

    private static String floatStr(float f) {
        if (Float.isFinite(f) && Math.rint(f) == f) return Integer.toString((int) f);
        return Float.toString(f);
    }
}
