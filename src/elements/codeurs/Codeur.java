package elements.codeurs;

import elements.Element;
import elements.sources.SourceFixe;
import elements.transmetteurs.TransmetteurParfait;
import information.Information;
import information.InformationNonConformeException;
import simulateur.Mode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Codeur extends Element<Boolean, Float> {

    private Integer dernierBit;
    private Integer bitSuivant;
    private Mode mode;
    private Float[] amplitude;
    private int nbEch;
    private boolean choixCodage;
    private Information<Boolean> informationCodee;

    /**
     * Constructeur du décodeur.
     *
     * @param mode      Le mode de décodage (NRZ, NRZT, RZ).
     * @param nbEch     Le nombre d'échantillons par bit.
     * @param amplitude    Amplitude minimale et maximale du signal
     */
    public Codeur(Mode mode, int nbEch, Float[] amplitude, boolean choixCodage) {
        this.mode = mode;
        this.nbEch = nbEch;
        this.amplitude = amplitude ;
        this.choixCodage = choixCodage;
        this.informationEmise = new Information<>();
        this.informationCodee = new Information<>();
    }

    public Information<Boolean> recevoir(Information<Boolean> information) throws InformationNonConformeException {
        if (information == null || information.estVide()) {
            throw new InformationNonConformeException("Information reçue est null");
        }
        return super.recevoir(information);
    }

    /**
     * Permet de transformer notre information initiale booléenne en information flottante,
     * appel de fonctions différentes selon le mode souhaité.
     *
     * @return la conversion de l'information reçue (boolean) en float
     * @throws InformationNonConformeException
     */
    public Information<Float> transformer() throws InformationNonConformeException {
        Information<Float> informationFloat = new Information<>();

        if (informationCodee.estVide() || informationCodee == null) {
            informationCodee = informationRecue;
        }
        Float[] symbole = new Float[nbEch];
        List<Boolean> bitsList = new ArrayList<>();

        Map<Boolean, Integer> convertion = new HashMap<>();
        convertion.put(true, 1);
        convertion.put(false, 0);

        // Convertir l'information en une liste de bits
        for (Boolean b : informationCodee) {
            bitsList.add(b);
        }

        // Convertir la liste en tableau
        Boolean[] bits = bitsList.toArray(new Boolean[0]);
        for (int i = 0; i < bits.length; i++) {
            // Déterminer le bit suivant (par défaut, on suppose qu'il n'y a pas de bit suivant)
            if (i == bits.length - 1) {
                bitSuivant = -1;
            }else{
                bitSuivant = convertion.get(bits[i+1]);
            }

            if (i == 0){
                dernierBit = -1;
            }
            else{
                dernierBit = convertion.get(bits[i-1]);
            }


            // Forme d'onde rectangulaire
            if (mode == Mode.NRZ) {
                symbole = miseEnFormeSymboleNRZ(bits[i]);
            } else if (mode == Mode.NRZT) { // Forme d'onde trapézoïdale
                symbole = miseEnFormeSymboleNRZT(convertion.get(bits[i]), dernierBit, bitSuivant);
            } else if (mode == Mode.RZ) { // Forme d'onde impulsionnelle
                symbole = miseEnFormeSymboleRZ(bits[i]);
            }
            for (Float f : symbole) {
                informationFloat.add(f);
            }
        }
        return informationFloat;
    }


    /**
     * Mise en forme de l'information en NRZ
     *
     * @param information information à mettre en forme selon le mode NRZ
     * @return un tableau de float qu'il faut par la suite ajouté dans Information
     */
    private Float[] miseEnFormeSymboleNRZ(boolean information) {
        Float[] palier = new Float[nbEch];

        // Itération du nombre d'échantillons souhaité
        for (int i = 0; i < nbEch; i++) {
            if (information) palier[i] = amplitude[1]; // Conversion de 1 en ampMax
            else palier[i] = amplitude[0]; // Conversion de 0 en ampMin
        }
        return palier;
    }

    /**
     * Mise en forme de l'information en NRZT
     *
     * @param valeur information à mettre en forme
     * @param precedent information precedente
     * @param suivant information suivante
     * @return un tableau de float qu'il faut par la suite ajouté dans Information
     */
    private Float[] miseEnFormeSymboleNRZT(int valeur, int precedent, int suivant) {
        float min = amplitude[0];
        float max = amplitude[1];

        Float[] symbole = new Float[nbEch];
        Map<Integer, Float> convertion = new HashMap<>();
        convertion.put(0, min);
        convertion.put(1, max);
        for (int i = 0; i < nbEch/3; i++) {

            if (precedent == -1){
                float x = ((float) i /(nbEch /3f));
                symbole[i] = convertion.get(valeur)*x;
            }else {
                float x = (((float) i /(nbEch /3f))/2f) + 0.5f;
                symbole[i] = convertion.get(precedent) - (convertion.get(precedent) - convertion.get(valeur))*x;
            }

        }
        for (int i = nbEch/3; i < 2*nbEch/3; i++) {
            symbole[i] = convertion.get(valeur);
        }
        for (int i = 2*nbEch/3; i < nbEch; i++) {

            if (suivant == -1){
                float x = (float) ((i - 2f*nbEch/3f) / (nbEch /3f));
                symbole[i] = convertion.get(valeur) - (convertion.get(valeur))*x;
            }
            else {
                float x = (float) (((i - 2f*nbEch/3f) / (nbEch /3f))/2f);
                symbole[i] = convertion.get(valeur) - (convertion.get(valeur) - convertion.get(suivant))*x;
            }

        }
        return symbole;
    }


    /**
     * Mise en forme de l'information en RZ
     *
     * @param information information à mettre en forme
     * @return un tableau de float avec les informations traitées
     */
    private Float[] miseEnFormeSymboleRZ(boolean information) {
        Float[] palier = new Float[nbEch];

        for (int i = 0; i < (nbEch / 3); i++) {
            palier[i] = 0.0f;
        }
        for (int i = nbEch / 3; i < 2 * (nbEch / 3); i++) {
            if (information) palier[i] = amplitude[1];
            else palier[i] = 0.0f;
        }
        for (int i = 2 * (nbEch / 3); i < nbEch; i++) {
            palier[i] = 0.0f;
        }
        return palier;
    }

    /**
     * Utilisation d'un codage de canal afin de pouvoir détecter des erreurs issues de la transmission bruitée
     *
     * @return information mise en forme
     * @throws InformationNonConformeException
     */
    public Information<Boolean> codageCanal() throws InformationNonConformeException, NullPointerException {
        if(informationRecue == null || informationRecue.estVide()) {
            throw new InformationNonConformeException("Information Vide");
        }

        informationCodee = new Information<>();

        // Itération des informations reçues
        for(Boolean b : informationRecue){
            if(!b){ // Cas ou le bit est à 0
                informationCodee.add(false);
                informationCodee.add(true);
                informationCodee.add(false);
            }
            if(b){ //Cas ou le bit est à 1
                informationCodee.add(true);
                informationCodee.add(false);
                informationCodee.add(true);
            }
        }
        return informationCodee;
    }

    @Override
    public Information<Float> emettre() throws InformationNonConformeException {
        if(choixCodage) informationCodee = codageCanal();
        informationEmise = transformer();

        return super.emettre();
    }


    /*public static void main(String[] args) throws Exception {
        Float[] amp = {0.0f, 4.0f};
        SourceFixe s = new SourceFixe("011100001");
        Codeur c = new Codeur(Mode.NRZ, 1,amp , true);
        TransmetteurParfait t = new TransmetteurParfait();
        s.connecter(c);
        c.connecter(t);

        s.emettre();

        System.out.println("Information reçue :"+ c.getInformationRecue());
        System.out.println("Information codee" + c.getInformationEmise());
    }*/

}
