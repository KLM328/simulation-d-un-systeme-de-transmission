package demo;

import elements.codeurs.Codeur;
import elements.sources.Source;
import elements.sources.SourceFixe;
import elements.transmetteurs.TransmetteurBruite;
import elements.visualisations.Sonde;
import elements.visualisations.SondeAnalogique;
import information.InformationNonConformeException;
import information.ParametreTrajet;
import simulateur.Mode;

import java.util.LinkedList;

public class E4MultiTrajet {
    public static void main(String[] args) throws InformationNonConformeException {
        Sonde<Float> sonde = new SondeAnalogique("MultiTrajet");

        Source source = new SourceFixe("01010101001");
        Codeur codeur = new Codeur(Mode.RZ, 30, new Float[] {-4f, 4f},false);
        LinkedList<ParametreTrajet> params = new LinkedList<>();
//        params.add(new ParametreTrajet(5, 0.1));
//        params.add(new ParametreTrajet(15, 0.2));
        TransmetteurBruite transmetteurBruite = new TransmetteurBruite(0f, 30, params);
        SondeAnalogique sondeEntre = new SondeAnalogique("Entrée Transmetteur");
        SondeAnalogique sondeSortie = new SondeAnalogique("Sortie Transmetteur");

        source.connecter(codeur);
        codeur.connecter(transmetteurBruite);
        codeur.connecter(sondeEntre);
        transmetteurBruite.connecter(sondeSortie);

        source.emettre();


    }
}
