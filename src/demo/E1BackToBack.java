package demo;

import elements.sources.SourceFixe;
import elements.transmetteurs.TransmetteurParfait;
import elements.destinations.DestinationFinale;
import information.Information;

public class E1BackToBack {

    // Convertit Information<Boolean> en chaîne "0101..."
    private static String as01(Information<Boolean> info) {
        StringBuilder sb = new StringBuilder(info.nbElements());
        for (int i = 0; i < info.nbElements(); i++) {
            sb.append(Boolean.TRUE.equals(info.iemeElement(i)) ? '1' : '0');
        }
        return sb.toString();
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
        SourceFixe src = new SourceFixe("01001100011001010010000001110000011100100110111101101010011001010111010000100000011001000010011101001000110000111010100101101100110000111010100101101110011000010010110000100000010101000110100101110000011010000110000101101001011011100110010100101100001000000100100101101110110000111010100001110011001000000110010101110100001000000101000101110101011001010110111001110100011010010110111000101110");
        TransmetteurParfait tx = new TransmetteurParfait();
        DestinationFinale dst = new DestinationFinale();

        // 2) Connecter la chaîne
        src.connecter(tx);
        tx.connecter(dst);

        // 3) Émettre depuis la source (la propagation se fait ensuite automatiquement)
        src.emettre();

        // 4) Récupérer informationEmise / informationRecue et calculer le TEB
        Information<Boolean> emise = src.getInformationEmise();
        Information<Boolean> recue = dst.getInformationRecue();

        String sEmise = as01(emise);
        String sRecue = as01(recue);
        float teb = calculTEB(emise, recue);

        System.out.println("emise = " + sEmise);
        System.out.println("recue = " + sRecue);
        System.out.println("TEB   = " + teb);
    }
}
