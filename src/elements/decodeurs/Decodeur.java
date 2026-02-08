package elements.decodeurs;

import elements.Element;
import elements.destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;
import information.SizeInformationException;
import simulateur.Mode;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.round;

/**
 * La classe Decodeur permet de décoder un signal analogique en signal numérique
 * selon différents modes de codage (NRZ, NRZT, RZ).
 */
public class Decodeur extends Element<Float, Boolean> {

    private Mode mode;
    private Float[] amplitude;
    private int nbEch;
    private boolean choixCodage;
    private Information<Boolean> informationDecodee;

    /**
     * Constructeur du décodeur.
     *
     * @param mode      Le mode de décodage (NRZ, NRZT, RZ).
     * @param nbEch     Le nombre d'échantillons par bit.
     * @param amplitude Les amplitudes minimales et maximales du signal.
     */
    public Decodeur(Mode mode, int nbEch, Float[] amplitude, boolean choixCodage) {
        this.mode = mode;
        this.nbEch = nbEch;
        this.amplitude = amplitude;
        this.informationEmise = new Information<>();
        this.informationDecodee = new Information<>();
        this.choixCodage = choixCodage;
    }

    /**
     * Reçoit une information analogique et la décode en signal numérique.
     *
     * @param information L'information analogique à décoder.
     * @throws InformationNonConformeException Si l'information est invalide.
     */
    public Information<Float> recevoir(Information<Float> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("Information reçue est null");
        }
        return super.recevoir(information);
    }

    public Information<Boolean> transformer(Information<Float> information) throws InformationNonConformeException {
        if (choixCodage) informationDecodee = decodeCanal(informationRecue);
        else informationDecodee = decode(information);
        return informationDecodee;
    }

    /**
     * Décode un signal
     *
     * @param information L'information analogique à décoder.
     * @return L'information décodée en signal numérique.
     */
    private Information<Boolean> decode(Information<Float> information) {
        Information<Boolean> informationDecodee = new Information<>();
        int nbBits = information.nbElements() / nbEch;
        int[] bornes = new int[2];

        switch (mode) {
            case RZ:
                amplitude[0] = 0f;
                bornes = new int[]{(int) round(nbEch / 3.0), (int) round(2 * nbEch / 3.0)};
                break;
            case NRZ:
                bornes = new int[]{0, nbEch};
                break;
            case NRZT:
                bornes = new int[]{(int) round(nbEch / 3.0), (int) round(2 * nbEch / 3.0)};
                break;
            default:
                return null;
        }

        for (int i = 0; i < nbBits; i++) {
            float somme = 0;
            int nbValeur = 0;
            for (int j = bornes[0]; j < bornes[1]; j++) {
                somme += information.iemeElement(i * nbEch + j);
                nbValeur++;
            }
            float moyenne = somme / nbValeur;
            informationDecodee.add((moyenne >= (amplitude[0] + amplitude[1]) / 2));
        }

        return informationDecodee;
    }


    /**
     * Émet l'information décodée vers les destinations connectées.
     *
     * @throws InformationNonConformeException Si l'information à émettre est null.
     */
    public Information<Boolean> emettre() throws InformationNonConformeException {
        if (this.informationEmise == null) {
            throw new InformationNonConformeException("Aucune information à émettre");
        }

        informationEmise = new Information<>(transformer(getInformationRecue()));
        return super.emettre();
    }

    /**
     * ;
     * Définit le mode de décodage.
     *
     * @param mode Le mode de décodage (NRZ, NRZT, RZ).
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * Récupère l'information émise.
     *
     * @return L'information émise.
     */
    public Information<Boolean> getInformationEmise() {
        return this.informationEmise;
    }

    /**
     * Décode une suite d’informations flottantes reçues du canal en bits
     *
     * @param information information float reçue à décoder
     * @return information booléenne décodée
     * @throws SizeInformationException si les données sont nulles, vides ou de tailles incohérentes
     */
    public Information<Boolean> decodeCanal(Information<Float> information) throws SizeInformationException {
        if (information == null || information.nbElements() == 0) {
            throw new SizeInformationException("Information nulle ou vide");
        }

        // Décodage de l'information analogique en bits
        Information<Boolean> bitsRecus = decode(information);
        if (bitsRecus == null || bitsRecus.nbElements() == 0) {
            throw new SizeInformationException("Décodage du canal a échoué");
        }

        // Vérification que la taille est un multiple de 3 pour le dictionnaire
        if (bitsRecus.nbElements() % 3 != 0) {
            throw new SizeInformationException(
                    "La taille du message décodé (" + bitsRecus.nbElements() + ") n'est pas un multiple de 3"
            );
        }

        // Dictionnaire de correspondance 3 bits -> 1 bit
        Map<String, Boolean> dict = Map.of(
                "000", false, "001", true,  "010", false, "011", false,
                "100", true,  "101", true,  "110", false, "111", true
        );

        // Application du dictionnaire
        Information<Boolean> informationDecodee = new Information<>();
        for (int i = 0; i < bitsRecus.nbElements(); i += 3) {
            char b0 = bitsRecus.iemeElement(i) ? '1' : '0';
            char b1 = bitsRecus.iemeElement(i + 1) ? '1' : '0';
            char b2 = bitsRecus.iemeElement(i + 2) ? '1' : '0';
            String key = "" + b0 + b1 + b2;

            Boolean sortie = dict.get(key);
            if (sortie == null) {
                throw new SizeInformationException("Combinaison inconnue: " + key);
            }
            informationDecodee.add(sortie);
        }

        return informationDecodee;
    }
}
