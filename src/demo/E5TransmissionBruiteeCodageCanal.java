package demo;

import elements.codeurs.Codeur;
import elements.decodeurs.Decodeur;
import elements.transmetteurs.TransmetteurBruite;
import information.Information;
import information.InformationNonConformeException;
import information.ParametreTrajet;
import information.SizeInformationException;
import simulateur.Mode;

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;

public class E5TransmissionBruiteeCodageCanal {

    // ---------- Paramètres de base ----------
    static final int NB_BITS = 200;
    static final int NB_ECH = 30;
    static final Float[] AMPL = {-4.0f, 4.0f};
    static final long BASE_SEED = 20251005L;

    static double[] SNR_GRID() {
        final int SNR_MIN = -20;
        final int SNR_MAX =  20;
        int n = (SNR_MAX - SNR_MIN) + 1; // inclusif
        double[] g = new double[n];
        for (int i = 0; i < n; i++) g[i] = SNR_MIN + i;
        return g;
    }

    static final int NB_ESSAIS = 100;

    // Multi-trajets
    static LinkedList<ParametreTrajet> trajets() {
        LinkedList<ParametreTrajet> l = new LinkedList<>();
        // l.add(new ParametreTrajet(12, 0.3f));
        return l;
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);

        // Courbes moyennées sur NB_ESSAIS messages par SNR
        Mode[] modes = new Mode[]{Mode.RZ, Mode.NRZ, Mode.NRZT};
        double[] snrs = SNR_GRID();

        // Colonnes CSV : snr, with_RZ, without_RZ, with_NRZ, without_NRZ, with_NRZT, without_NRZT
        File outDir = new File("out");
        if (!outDir.exists()) outDir.mkdirs();
        File csv = new File(outDir, "teb_vs_snr.csv");

        try (PrintWriter pw = new PrintWriter(csv, "UTF-8")) {
            pw.println("snr_db,with_RZ,without_RZ,with_NRZ,without_NRZ,with_NRZT,without_NRZT");

            for (double snr : snrs) {
                // Accumulate TEB for each (mode, with/without)
                double withRZ = 0, withoutRZ = 0, withNRZ = 0, withoutNRZ = 0, withNRZT = 0, withoutNRZT = 0;

                for (int essai = 0; essai < NB_ESSAIS; essai++) {
                    long seedBaseForThisRow = BASE_SEED + (long) (snr * 10_000L) + essai;

                    // -- RZ
                    Information<Boolean> bitsRZ = bitsAleatoires(NB_BITS, seedBaseForThisRow ^ 0xA11CE);
                    withRZ    += chaineAvecCodage(bitsRZ, Mode.RZ, (float) snr).teb;
                    withoutRZ += chaineSansCodage(bitsRZ, Mode.RZ, (float) snr).teb;

                    // -- NRZ
                    Information<Boolean> bitsNRZ = bitsAleatoires(NB_BITS, seedBaseForThisRow ^ 0xBEEFL);
                    withNRZ    += chaineAvecCodage(bitsNRZ, Mode.NRZ, (float) snr).teb;
                    withoutNRZ += chaineSansCodage(bitsNRZ, Mode.NRZ, (float) snr).teb;

                    // -- NRZT
                    Information<Boolean> bitsNRZT = bitsAleatoires(NB_BITS, seedBaseForThisRow ^ 0xC0FFEE);
                    withNRZT    += chaineAvecCodage(bitsNRZT, Mode.NRZT, (float) snr).teb;
                    withoutNRZT += chaineSansCodage(bitsNRZT, Mode.NRZT, (float) snr).teb;
                }

                // Moyennes
                withRZ     /= NB_ESSAIS;
                withoutRZ  /= NB_ESSAIS;
                withNRZ    /= NB_ESSAIS;
                withoutNRZ /= NB_ESSAIS;
                withNRZT   /= NB_ESSAIS;
                withoutNRZT/= NB_ESSAIS;

                pw.printf(Locale.US,
                        "%.0f,%.6g,%.6g,%.6g,%.6g,%.6g,%.6g%n",
                        snr, withRZ, withoutRZ, withNRZ, withoutNRZ, withNRZT, withoutNRZT);
            }
        }

        // Optionnel : petit message (supprime si tu veux zéro console)
        System.out.println("[OK] CSV écrit : " + csv.getPath());
    }

    // ---------- Chaînes avec / sans codage (paramétrées par mode et snr) ----------
    private static Resultat chaineAvecCodage(Information<Boolean> bitsSource, Mode forme, float snrDb)
            throws InformationNonConformeException, SizeInformationException {

        Codeur codeur = new Codeur(forme, NB_ECH, AMPL, true);
        codeur.recevoir(bitsSource);
        Information<Float> ondeCodee = codeur.emettre();

        TransmetteurBruite canal = new TransmetteurBruite(snrDb, NB_ECH, trajets());
        canal.recevoir(ondeCodee);
        Information<Float> ondeRecue = canal.emettre();

        Decodeur decodeur = new Decodeur(forme, NB_ECH, AMPL.clone(),false);
        //Information<Float>[] blocs = new Information[]{ondeRecue};
        //Information<Boolean>[] dec = decodeur.decodeCanal(blocs);
        Information<Boolean> bitsRecus = decodeur.decodeCanal(ondeRecue);

        double teb = calculTEB(bitsSource, bitsRecus);
        return new Resultat(bitsRecus, teb);
    }

    private static Resultat chaineSansCodage(Information<Boolean> bitsSource, Mode forme, float snrDb)
            throws InformationNonConformeException {

        Codeur codeur = new Codeur(forme, NB_ECH, AMPL, false);
        codeur.recevoir(bitsSource);
        Information<Float> onde = codeur.emettre();

        TransmetteurBruite canal = new TransmetteurBruite(snrDb, NB_ECH, trajets());
        canal.recevoir(onde);
        Information<Float> ondeRecue = canal.emettre();

        Decodeur decodeur = new Decodeur(forme, NB_ECH, AMPL.clone(),false);
        decodeur.recevoir(ondeRecue);
        Information<Boolean> bitsRecus = decodeur.emettre();

        double teb = calculTEB(bitsSource, bitsRecus);
        return new Resultat(bitsRecus, teb);
    }

    // ---------- Utilitaires ----------
    private static Information<Boolean> bitsAleatoires(int n, long graine) {
        Random r = new Random(graine);
        Information<Boolean> info = new Information<>();
        for (int i = 0; i < n; i++) info.add(r.nextBoolean());
        return info;
    }

    private static double calculTEB(Information<Boolean> ref, Information<Boolean> recu) {
        int n = Math.min(ref.nbElements(), recu.nbElements());
        if (n == 0) return Double.NaN;
        int err = 0;
        for (int i = 0; i < n; i++) {
            boolean a = Boolean.TRUE.equals(ref.iemeElement(i));
            boolean b = Boolean.TRUE.equals(recu.iemeElement(i));
            if (a != b) err++;
        }
        return (double) err / n;
    }

    private record Resultat(Information<Boolean> recus, double teb) {}
}
