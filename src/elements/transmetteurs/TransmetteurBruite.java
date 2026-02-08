package elements.transmetteurs;

import bruits.Bruit;
import bruits.BruitBlancGaussien;
import information.Information;
import information.InformationNonConformeException;
import information.ParametreTrajet;

import java.util.LinkedList;

public class TransmetteurBruite extends Transmetteur<Float> {

    private Float snrpb;
    private int nbEch;
    private int seed;
    private LinkedList<ParametreTrajet> params;
    private boolean aleatoireAvecGerme;
    private Bruit bruit;

    /**
     * Constructeur d'un transmetteur bruité avec SNRpb et échantillons
     *
     * @param snrpb SNR par bit
     * @param nbEch nombre d'échantillons
     */
    public TransmetteurBruite(Float snrpb, int nbEch) {
        super();
        this.snrpb = snrpb;
        this.nbEch = nbEch;
        this.aleatoireAvecGerme = false;
        this.params = new LinkedList<>();
    }

    /**
     * Constructeur par défaut d’un transmetteur sans bruit avec 30 échantillons et liste de trajets multiples vide
     */
    public TransmetteurBruite() {
        super();
        this.snrpb = Float.POSITIVE_INFINITY;
        this.nbEch = 30;
        this.aleatoireAvecGerme = false;
        this.params = new LinkedList<>();
    }

    /**
     * Constructeur d'un transmetteur bruité avec SNRpb, échantillons et trajet multiple
     *
     * @param snrpb SNR par bit
     * @param nbEch nombre d'échantillons
     * @param params paramètres des trajets mutliples
     */
    public TransmetteurBruite(Float snrpb, int nbEch, LinkedList<ParametreTrajet> params) {
        super();
        this.snrpb = snrpb;
        this.nbEch = nbEch;
        this.params = params;
        this.aleatoireAvecGerme = false;
    }

    /**
     * Constructeur d’un transmetteur bruité avec SNRpb, échantillons, trajet multiple et seed
     *
     * @param snrpb SNR par bit
     * @param nbEch nombre d'échantillons
     * @param params paramètres des trajets multiples
     * @param seed graine aléatoire
     */
    public TransmetteurBruite(Float snrpb, int nbEch, LinkedList<ParametreTrajet> params, int seed) {
        super();
        this.snrpb = snrpb;
        this.nbEch = nbEch;
        this.params = params;
        this.seed = seed;
        this.aleatoireAvecGerme = true;
    }

    /**
     * Constructeur d’un transmetteur bruité avec SNR, échantillons et seed
     *
     * @param snrpb SNR par bit
     * @param nbEch nombre d'échantillons
     * @param seed graine aléatoire
     */
    public TransmetteurBruite(Float snrpb, int nbEch, int seed) {
        super();
        this.snrpb = snrpb;
        this.nbEch = nbEch;
        this.seed = seed;
        this.aleatoireAvecGerme = true;
        this.params = new LinkedList<>();
    }

    public Bruit getBruit(){
        return bruit;
    }

    /**
     * Simule un trjaet à trajets multiples sur l'information reçue
     *
     * @return information résultante après ajout des trajets multiples
     */
    public Information<Float> multiTrajet() {
        Information<Float> informationMulti = new Information<>(this.getInformationRecue());
        for (ParametreTrajet parametreTrajet : params) {
            informationMulti.somme(informationMulti.genererInformationRetardee(parametreTrajet));
        }
        return informationMulti;
    }

    /**
     * Ajoute un bruit blanc gaussien à l’information donnée selon le SNR spécifié
     *
     * @param informationABruiter information à bruiter
     * @param snrpb rapport signal/bruit par bit à utiliser
     * @return information à bruitée
     */
    public Information<Float> bruiter(Information<Float> informationABruiter, Float snrpb) {
        Information<Float> informationBruitee = new Information<>(informationABruiter);
        bruit = new BruitBlancGaussien(informationABruiter.nbElements(), snrpb, this.getInformationRecue().calculerPuissance(), nbEch);
        return informationBruitee.ajouterBruit(bruit); //faut il bruiter en fonction de l'info contenant les multi trajet
    }

    /**
     * Ajoute un bruit blanc gaussien à l’information donnée selon le SNR spécifié, avec une seed aléatoire
     *
     * @param informationABruiter information à bruiter
     * @param snrpb rapport signal/bruit par bit à utiliser
     * @param seed seed utilisée pour la génération du bruit
     * @return information à bruitée
     */
    public Information<Float> bruiter(Information<Float> informationABruiter, Float snrpb, int seed) {
        Information<Float> informationBruitee = new Information<>(informationABruiter);
        bruit = new BruitBlancGaussien(informationABruiter.nbElements(), snrpb, this.getInformationRecue().calculerPuissance(), nbEch, seed);
        return informationBruitee.ajouterBruit(bruit); //faut il bruiter en fonction de l'info contenant les multi trajet
    }

    /**
     * Émet l’information après ajout du bruit et des trajets multiples
     *
     * @return information émise
     * @throws InformationNonConformeException si l'emission échoue
     */
    public Information<Float> emettre() throws InformationNonConformeException {
        if (aleatoireAvecGerme) informationEmise = new Information<>(bruiter(multiTrajet(), snrpb, seed));
        else informationEmise = new Information<>(bruiter(multiTrajet(), snrpb)) ;
        return super.emettre();
    }
}
