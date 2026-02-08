package demo;

import simulateur.Mode;
import simulateur.Simulateur;
import java.util.Arrays;
import java.util.LinkedList;
import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Math.*;

public class E6EtudeCahierDesChargesEnv2 {
    static int nbEchMin = 3;
    static int nbEchMax = 300;
    static int snrMin = 0;
    static int snrMax = 15;
    static int longueurMess = 10000;
    static float tBit = (float) (5*pow(10, -6));
    static float ampl1 = 1f;
    static float ampl2 = 0.5f;
    static int tau1 = (int) round((pow(10, -5)/tBit));
    static int tau2 = tau1 * 2;
    static float[] amplitudeCodage = {-4f, 4f};
    static float snrpb;
    static Simulateur simulateur;

    public static void main(String[] args) throws Exception {
        // Créer un FileWriter pour écrire dans un fichier CSV
        try (FileWriter writer = new FileWriter("out/resultat_simulation_environnement2.csv")) {
            // Écrire l'en-tête du CSV
            writer.write("Mode,nbEch,snr,tauxErreurBinaire\n");

            for (Mode mode : new Mode[]{Mode.NRZ, Mode.NRZT}) {
                for (int nbEch = nbEchMin; nbEch <= nbEchMax; nbEch+=3) {
                    for(int snr = snrMin; snr <= snrMax; snr++){
                        snrpb = (float) (snr - 3 + 10 * log10(nbEch));
                        LinkedList<String> argsList = new LinkedList<>();
                        argsList.addAll(Arrays.asList(
                                "-mess", Integer.toString(longueurMess),
                                "-form", mode.name(),
                                "-nbEch", Integer.toString(nbEch),
                                "-ampl", floatStr(amplitudeCodage[0]), floatStr(amplitudeCodage[1]),
                                "-snrpb", floatStr(snrpb),
                                "-ti", Integer.toString(tau1), floatStr(ampl1), Integer.toString(tau2), floatStr(ampl2),
                                "-codeur"));

                        Simulateur simu = new Simulateur(argsList.toArray(new String[0]));
                        simu.execute();
                        double tauxErreurBinaire = simu.calculTauxErreurBinaire();
                        double snrCalcule = simu.calculSNRdB();

                        // Écrire les résultats dans le fichier CSV
                        writer.write(mode.name() + "," + nbEch + "," + snrCalcule + "," + tauxErreurBinaire + "\n");

                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --------- utilitaires ---------
    private static String floatStr(float f) {
        if (Float.isFinite(f) && Math.rint(f) == f) return Integer.toString((int) f);
        return Float.toString(f);
    }
}
