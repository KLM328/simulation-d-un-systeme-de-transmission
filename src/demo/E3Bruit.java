package demo;

import bruits.BruitBlancGaussien;
import elements.transmetteurs.TransmetteurParfait;
import elements.visualisations.Sonde;
import elements.visualisations.SondeAnalogique;
import information.Information;
import information.InformationNonConformeException;

public class E3Bruit {
    public static void main(String[] args) throws InformationNonConformeException {
        Sonde<Float> sonde = new SondeAnalogique("bruit");
        Information<Float> bruit = new BruitBlancGaussien(1000, 30f, 20, 300);
        TransmetteurParfait<Float> tr = new TransmetteurParfait<>();
        tr.connecter(sonde);
        tr.recevoir(bruit);
        tr.emettre();

    }
}
