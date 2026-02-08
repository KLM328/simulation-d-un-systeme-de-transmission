package destinations;


/** 
 * Classe Abstraite d'un composant destination d'informations dont les
 * éléments sont de type T
 * @author prou
 */
public  abstract class Destination <T> implements destinations.DestinationInterface<T> {
    
    /** 
     * l'information reçue par la destination
     */
    protected information.Information<T> informationRecue;
    
    /** 
     * un constructeur factorisant les initialisations communes aux
     * réalisations de la classe abstraite Destination
     */
    public Destination() {
	informationRecue = null;
    }

    /**
     * retourne la dernière information reçue par la destination
     * @return une information   
     */
    public information.Information<T> getInformationRecue() {
	return this.informationRecue;
    }
   	    
    /**
     * reçoit une information
     * @param information  l'information  à recevoir
     * @throws information.InformationNonConformeException si l'Information comporte une anomalie
     */
    public  abstract void recevoir(information.Information<T> information) throws information.InformationNonConformeException;
}
