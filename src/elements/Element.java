package elements;

import elements.destinations.Destination;
import elements.destinations.DestinationInterface;
import elements.sources.SourceInterface;
import information.Information;
import information.InformationNonConformeException;

import java.util.LinkedList;

public abstract class Element<R, E> implements ElementInterface<R,E> {
    /**
     * la liste des composants destination connectés en sortie du transmetteur
     */
    protected LinkedList<DestinationInterface<E>> destinationsConnectees;

    /**
     * l'information reçue en entrée du transmetteur
     */
    protected Information<R> informationRecue;

    /**
     * l'information émise en sortie du transmetteur
     */
    protected Information<E> informationEmise;

    public Element() {
        destinationsConnectees = new LinkedList<DestinationInterface<E>>();
        informationRecue = null;
        informationEmise = null;
    }

    /**
     * retourne la dernière information reçue en entrée du
     * transmetteur
     *
     * @return une information
     */
    public Information<R> getInformationRecue() {
        return this.informationRecue;
    }

    /**
     * retourne la dernière information émise en sortie du
     * transmetteur
     *
     * @return une information
     */
    public Information<E> getInformationEmise() {
        return this.informationEmise;
    }

    /**
     * retourne la liste des destinations connectées
     *
     * @return une liste de destination
     */
    public LinkedList<DestinationInterface<E>> getDestinationsConnectees() {
        return destinationsConnectees;
    }

    /**
     * connecte une destination à la sortie du transmetteur
     *
     * @param destination la destination à connecter
     */
    public void connecter(DestinationInterface<E> destination) {
        destinationsConnectees.add(destination);
    }

    /**
     * déconnecte une destination de la la sortie du transmetteur
     *
     * @param destination la destination à déconnecter
     */
    public void deconnecter(DestinationInterface<E> destination) {
        destinationsConnectees.remove(destination);
    }

    /**
     * reçoit une information.  Cette méthode, en fin d'exécution,
     * appelle la méthode émettre.
     *
     * @param information l'information  reçue
     * @throws InformationNonConformeException si l'Information comporte une anomalie
     */
    public Information<R> recevoir(Information<R> information) throws InformationNonConformeException{
        informationRecue = new Information<>(information);
        emettre();
        return informationRecue;
    }

    /**
     * émet l'information construite par le transmetteur
     *
     * @throws InformationNonConformeException si l'Information comporte une anomalie
     */
    public Information<E> emettre() throws InformationNonConformeException{
        if (this.informationEmise == null || this.informationEmise.estVide()) {
            throw new InformationNonConformeException("Aucune information à émettre");
        }

        for(DestinationInterface<E> destination : destinationsConnectees) {
            destination.recevoir(informationEmise);
        }
        return getInformationEmise();
    }

}
