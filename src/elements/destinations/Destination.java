package elements.destinations;

import information.*;

/** 
 * Classe Abstraite d'un composant destination d'informations dont les
 * éléments sont de type T
 * @author prou
 */
public  abstract class Destination <T> implements DestinationInterface <T> {
    
    /** 
     * l'information reçue par la destination
     */
    protected Information <T>  informationRecue;
    
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
    public Information  <T>  getInformationRecue() {
	    return this.informationRecue;
    }
   	    
    /**
     * reçoit une information
     * @param information  l'information  à recevoir
     * @throws InformationNonConformeException si l'Information comporte une anomalie
     */
    public Information<T> recevoir(Information <T> information) throws InformationNonConformeException{
        if (information == null) {
            throw new InformationNonConformeException("Information is null");
        }
        informationRecue = new Information<>(information);
        return getInformationRecue();
    }
}
