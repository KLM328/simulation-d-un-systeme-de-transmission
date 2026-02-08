package elements;

import elements.destinations.DestinationInterface;
import elements.sources.SourceInterface;
import information.Information;
import information.InformationNonConformeException;

import java.util.LinkedList;

public interface ElementInterface<R,E> extends DestinationInterface<R>, SourceInterface<E> {

    /**
     * retourne la dernière information reçue en entrée du
     * transmetteur
     *
     * @return une information
     */
    public Information<R> getInformationRecue();

    /**
     * retourne la dernière information émise en sortie du
     * transmetteur
     *
     * @return une information
     */
    public Information<E> getInformationEmise();

    /**
     * retourne la liste des destinations connectées
     *
     * @return une liste de destination
     */
    public LinkedList<DestinationInterface<E>> getDestinationsConnectees();

    /**
     * connecte une destination à la sortie du transmetteur
     *
     * @param destination la destination à connecter
     */
    public void connecter(DestinationInterface<E> destination);

    /**
     * déconnecte une destination de la la sortie du transmetteur
     *
     * @param destination la destination à déconnecter
     */
    public void deconnecter(DestinationInterface<E> destination);

    /**
     * reçoit une information.  Cette méthode, en fin d'exécution,
     * appelle la méthode émettre.
     *
     * @param information l'information  reçue
     * @throws InformationNonConformeException si l'Information comporte une anomalie
     */
    public abstract Information<R> recevoir(Information<R> information) throws InformationNonConformeException;

    /**
     * émet l'information construite par le transmetteur
     *
     * @throws InformationNonConformeException si l'Information comporte une anomalie
     */
    public abstract Information<E> emettre() throws InformationNonConformeException;
}
