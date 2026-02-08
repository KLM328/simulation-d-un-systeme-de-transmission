package destinations;



/** 
 * Interface d'un composant ayant le comportement d'une destination
 * d'informations dont les éléments sont de type T
 * @author prou
 */
public  interface DestinationInterface <T>  {   
   
    /**
     * pour obtenir la dernière information reçue par une destination.
     * @return une information   
     */  
    public information.Information<T> getInformationRecue();
   	 
    /**
     * pour recevoir une information de la source qui nous est
     * connectée
     * @param information  l'information  à recevoir
     * @throws information.InformationNonConformeException si l'Information comporte des anomalies
     */
    public void recevoir(information.Information<T> information) throws information.InformationNonConformeException;
   
}
