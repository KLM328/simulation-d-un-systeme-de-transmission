package elements.transmetteurs;

import elements.destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

public class TransmetteurParfait<T> extends Transmetteur<T>{

    /**
     * Constructeur pour un transmetteur parfait
     */
	public TransmetteurParfait() {
		super();
	}

    /**
     * Émet l’information
     *
     * @return information émise
     * @throws InformationNonConformeException si erreur
     */
	public Information<T> emettre() throws InformationNonConformeException {
		informationEmise = informationRecue;
		return super.emettre();
	}

}
