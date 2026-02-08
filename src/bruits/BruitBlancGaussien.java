package bruits;

import java.util.Random;

/**
 * La classe BruitBlancGaussien représente un bruit blanc gaussien.
 * Elle étend la classe Bruit et fournit des méthodes pour générer un bruit blanc gaussien
 * en fonction de divers paramètres comme le rapport signal-sur-bruit (SNR) et la puissance du signal.
 */
public class BruitBlancGaussien extends Bruit {

    private int nbEch;

    private Random random;

    /**
     * Constructeur pour créer un bruit blanc gaussien avec un nombre spécifique d'éléments.
     * Chaque élément est initialisé à 0.
     *
     * @param nbElements Le nombre d'éléments dans le bruit blanc gaussien.
     */
    public BruitBlancGaussien(int nbElements) {
        super(nbElements);
    }

    /**
     * Constructeur pour créer un bruit blanc gaussien avec un nombre spécifique d'éléments,
     * un rapport signal-sur-bruit (SNR), une puissance de signal et un nombre d'échantillons.
     *
     * @param nbElements Le nombre d'éléments dans le bruit blanc gaussien.
     * @param snrpb Le rapport signal-sur-bruit en décibels (dB).
     * @param pSignal La puissance du signal.
     * @param nbEch Le nombre d'échantillons utilisés pour le calcul.
     */
    public BruitBlancGaussien(int nbElements, Float snrpb, double pSignal, int nbEch) {
        super(nbElements);
        this.nbEch = nbEch;
        this.random = new Random();
        generer(snrpb, pSignal);
    }

    /**
     * Constructeur pour créer un bruit blanc gaussien avec un nombre spécifique d'éléments,
     * un rapport signal-sur-bruit (SNR), une puissance de signal, un nombre d'échantillons
     * et une graine pour le générateur de nombres aléatoires.
     *
     * @param nbElements Le nombre d'éléments dans le bruit blanc gaussien.
     * @param snrpb Le rapport signal-sur-bruit en décibels (dB).
     * @param pSignal La puissance du signal.
     * @param nbEch Le nombre d'échantillons utilisés pour le calcul.
     * @param seed La graine pour le générateur de nombres aléatoires.
     */
    public BruitBlancGaussien(int nbElements, Float snrpb, double pSignal, int nbEch, int seed) {
        super(nbElements);
        this.nbEch = nbEch;
        this.random = new Random();
        random.setSeed(seed);
        generer(snrpb, pSignal);
    }

    /**
     * Génère les valeurs du bruit blanc gaussien en utilisant l'algorithme de Box-Muller.
     * Les valeurs sont générées en fonction du rapport signal-sur-bruit (SNR) et de la puissance du signal.
     *
     * @param snrpb Le rapport signal-sur-bruit en décibels (dB).
     * @param pSignal La puissance du signal.
     */
    public void generer(Float snrpb, double pSignal) {
        double snrLin = Math.pow(10, snrpb / 10.0); //convertion en lineaire
        double ecartType = Math.sqrt((pSignal * nbEch) / (snrLin * 2));//calcul de l'ecart-type du bruit

        for (int i = 0; i < nbElements(); i += 2) {
            double u1 = random.nextDouble();
            double u2 = random.nextDouble();
            double z0 = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
            double z1 = Math.sqrt(-2.0 * Math.log(u1)) * Math.sin(2.0 * Math.PI * u2);
            if (i < nbElements()) {
                setIemeElement(i, (float) (z0 * ecartType));
            }
            if (i + 1 < nbElements()){
                setIemeElement(i + 1, (float) (z1 * ecartType));
            }
        }
    }

//        System.out.println("Puissance du signal : " + pSignal);
//        System.out.println("SNRdB attendu : " + snrpb);
//        System.out.println("EcartType : " + ecartType);
//        System.out.println("Puissance de bruit : " + calculerPuissance());
//        System.out.println("Nouveau SNR : " + 10 * Math.log10(pSignal/calculerPuissance()));


}

