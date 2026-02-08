package demo;

import elements.codeurs.Codeur;
import elements.decodeurs.Decodeur;
import elements.destinations.DestinationFinale;
import elements.sources.SourceFixe;
import elements.transmetteurs.TransmetteurBruite;
import elements.transmetteurs.TransmetteurParfait;
import elements.visualisations.VueCourbe;
import information.Information;
import simulateur.Mode;

public class E3TransmissionAnalogiqueBruitee {

    // Convertit Information<Boolean> en chaîne "0101..."
    private static String as01(Information<Boolean> info) {
        StringBuilder sb = new StringBuilder(info.nbElements());
        for (int i = 0; i < info.nbElements(); i++) {
            sb.append(Boolean.TRUE.equals(info.iemeElement(i)) ? '1' : '0');
        }
        return sb.toString();
    }

    private static String asAmplitude(Information<Float> info) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < info.nbElements(); i++) {
            sb.append(String.format("%.2f ", info.iemeElement(i)));
        }
        return sb.toString();
    }

    private static boolean[] toBooleanArray(Information<Boolean> info) {
        boolean[] tab = new boolean[info.nbElements()];
        for (int i = 0; i < info.nbElements(); i++) {
            tab[i] = Boolean.TRUE.equals(info.iemeElement(i));
        }
        return tab;
    }

    private static float[] toFloatArray(Information<Float> info) {
        float[] tab = new float[info.nbElements()];
        for (int i = 0; i < info.nbElements(); i++) {
            tab[i] = info.iemeElement(i);
        }
        return tab;
    }

    // Calcul TEB = erreurs / N
    private static float calculTEB(Information<Boolean> emise, Information<Boolean> recue) {
        int n = emise.nbElements();
        if (n != recue.nbElements()) throw new IllegalStateException("Tailles différentes !");
        int erreurs = 0;
        for (int i = 0; i < n; i++) {
            boolean a = Boolean.TRUE.equals(emise.iemeElement(i));
            boolean b = Boolean.TRUE.equals(recue.iemeElement(i));
            if (a != b) erreurs++;
        }
        return (n == 0) ? 0f : ((float) erreurs) / n;
    }

    public static void main(String[] args) throws Exception {
        // 1) Instancier les composants (comme dans l’énoncé)
        Float[] amplitude = {-1f, 1.0f};
        SourceFixe src = new SourceFixe("11111000000100100001000100000000");
        TransmetteurParfait tx = new TransmetteurParfait();
        DestinationFinale dst = new DestinationFinale();
        Codeur cdr = new Codeur(Mode.NRZT, 30, amplitude,false);
        Decodeur dec = new Decodeur(Mode.NRZT, 30, amplitude,false);
        TransmetteurBruite bruite = new TransmetteurBruite(-10f, 100);



        // 2) Connecter la chaîne
        src.connecter(cdr);
        cdr.connecter(bruite);
        bruite.connecter(dec);
        //tx.connecter(dec);
        dec.connecter(dst);

        // 3) Émettre depuis la source (la propagation se fait ensuite automatiquement)
        src.emettre();

        // 4) Récupérer informationEmise / informationRecue et calculer le TEB
        Information<Boolean> emise = src.getInformationEmise();
        Information<Float> codEmis = cdr.getInformationEmise();
        Information<Float> b = bruite.getInformationEmise();
        //Information<Boolean> decEmis = dec.getInformationEmise();
        Information<Boolean> recue = dst.getInformationRecue();

        String sEmise = as01(emise);
        String cdrEmis = asAmplitude(codEmis);
        //String decEmise = as01(decEmis);
        String sRecue = as01(recue);
        float teb = calculTEB(emise, recue);

        System.out.println("emise = " + sEmise);
        System.out.println("cdrEmis = " + cdrEmis);
        //System.out.println("cdrRecue = " + decEmise);
        System.out.println("recue = " + sRecue);
        System.out.println("TEB   = " + teb);

        // Affichage graphique
        new VueCourbe(toBooleanArray(emise), 200, "Message émis par la source");
        new VueCourbe(toFloatArray(b), "Message émis par le transmetteur bruité (signal analogique)");
        new VueCourbe(toBooleanArray(recue), 200, "Message reçu en destination");
    }
}
