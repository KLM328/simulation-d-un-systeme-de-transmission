package simulateur;


import bruits.Bruit;
import elements.codeurs.Codeur;
import elements.decodeurs.Decodeur;
import elements.destinations.Destination;
import elements.destinations.DestinationFinale;
import elements.destinations.DestinationInterface;
import elements.sources.Source;
import elements.sources.SourceAleatoire;
import elements.sources.SourceFixe;
import elements.sources.SourceInterface;
import elements.transmetteurs.Transmetteur;
import elements.transmetteurs.TransmetteurBruite;
import elements.transmetteurs.TransmetteurParfait;
import elements.visualisations.SondeAnalogique;
import elements.visualisations.SondeLogique;
import information.Information;
import information.InformationNonConformeException;
import information.ParametreTrajet;

import java.util.LinkedList;

import static java.lang.Math.log10;


/**
 * La classe Simulateur permet de construire et simuler une chaîne de
 * transmission composée d'une Source, d'un nombre variable de
 * Transmetteur(s) et d'une Destination.
 *
 * @author cousin
 * @author prou
 */
public class Simulateur {

    /**
     * indique si le Simulateur utilise des sondes d'affichage
     */
    protected boolean affichage = false;

    /**
     * indique si le Simulateur utilise un message généré de manière aléatoire (message imposé sinon)
     */
    protected boolean messageAleatoire = true;

    /**
     * indique si le Simulateur utilise un germe pour initialiser les générateurs aléatoires
     */
    protected boolean aleatoireAvecGerme = false;

    /**
     * la valeur de la semence utilisée pour les générateurs aléatoires
     */
    protected Integer seed = null; // pas de semence par défaut

    /**
     * la longueur du message aléatoire à transmettre si un message n'est pas imposé
     */
    protected int nbBitsMess = 100;

    /**
     * la chaîne de caractères correspondant à m dans l'argument -mess m
     */
    protected String messageString = "";

    /**
     * le  composant Source de la chaine de transmission
     */
    protected Source<Boolean> source = null;

    /**
     * le composant Codeur transforme le signal logique en signal analogique
     */
    protected Codeur codeur = null;

    /**
     * le  composant Transmetteur parfait logique de la chaine de transmission
     */
    protected Transmetteur<Boolean> transmetteurLogique = null;

    protected Transmetteur<Float> transmetteurAnalogique = null;

    /**
     * le composant Decodeur transforme le signal analogique en signal logique
     */
    protected Decodeur decodeur = null;

    /**
     * le  composant Destination de la chaine de transmission
     */
    protected Destination<Boolean> destination = null;

    protected Mode form = Mode.RZ;

    protected int nbEch = 30;

    protected Float[] amplitude = {0.0f, 1.0f};

    protected Float snrpb = Float.POSITIVE_INFINITY;

    protected LinkedList<ParametreTrajet> params;

    protected boolean transmissionAnalogique;

    protected boolean codageCanal = false;

    /**
     * pour connaitre le nombre d'enchantillon
     *
     * @return nbEch
     */
    public int getNbEch(){
        return nbEch;
    }

    /**
     * Le constructeur de Simulateur construit une chaîne de
     * transmission composée d'une Source <Boolean>, d'une Destination
     * <Boolean> et de Transmetteur(s) [voir la méthode
     * analyseArguments]...  <br> Les différents composants de la
     * chaîne de transmission (Source, Transmetteur(s), Destination,
     * Sonde(s) de visualisation) sont créés et connectés.
     *
     * @param args le tableau des différents arguments.
     * @throws ArgumentsException si un des arguments est incorrect
     */
    public Simulateur(String[] args) throws ArgumentsException {
        // analyser et récupérer les arguments

//        args = new String[7];
//        args[0] = "-mess";
//        args[1] = "1010111001";
//        args[2] = "-form";
//        args[3] = "RZ";
//        args[4] = "-nbEch";
//        args[5] = "99";
//        args[6] = "-s";
        this.transmissionAnalogique = false;

        analyseArguments(args);


        if (messageAleatoire) {
            if (aleatoireAvecGerme) {
                source = new SourceAleatoire(nbBitsMess, seed);
            } else {
                source = new SourceAleatoire(nbBitsMess);
            }
        } else {
            Information<Boolean> information = new Information<>();
            for (char e : messageString.toCharArray()) {
                if (e == '0') {
                    information.add(Boolean.FALSE);
                } else if (e == '1') {
                    information.add(Boolean.TRUE);
                } else {
                    throw new ArgumentsException("Caractere invalide");
                }
            }
            source = new SourceFixe(information);
        }
        codeur = new Codeur(form, nbEch, amplitude, codageCanal);
        if(transmissionAnalogique){
            if(snrpb == Float.POSITIVE_INFINITY && params == null){
                transmetteurAnalogique = new TransmetteurParfait<>();
            }
            else {
                if (params == null){
                    if(aleatoireAvecGerme){
                        transmetteurAnalogique = new TransmetteurBruite(snrpb, nbEch, seed);
                    }else{
                        transmetteurAnalogique = new TransmetteurBruite(snrpb, nbEch);
                    }

                }
                else {
                    if(aleatoireAvecGerme){
                        transmetteurAnalogique = new TransmetteurBruite(snrpb, nbEch, params, seed);
                    }
                    else{
                        transmetteurAnalogique = new TransmetteurBruite(snrpb, nbEch, params);
                    }

                }

            }

        } else {
            transmetteurLogique = new TransmetteurParfait<>();
        }

        decodeur = new Decodeur(form, nbEch, amplitude, codageCanal);
        destination = new DestinationFinale();

        if (transmissionAnalogique){
            source.connecter(codeur);
            codeur.connecter(transmetteurAnalogique);
            transmetteurAnalogique.connecter(decodeur);
            decodeur.connecter(destination);
        }else{
            source.connecter(transmetteurLogique);
            transmetteurLogique.connecter(destination);
        }



        if (affichage) {
            source.connecter(new SondeLogique("1. Source", 200));
            if (!transmissionAnalogique){
                transmetteurLogique.connecter(new SondeLogique("2. Transmetteur Logique", 200));


            }
            else{
                decodeur.connecter(new SondeLogique("3. Decodeur", 200));
                transmetteurAnalogique.connecter(new SondeAnalogique("2. Transmetteur Analogique"));

            }

        }
    }

    protected int setNbBitsMess(int nbBitsMess) {
        this.nbBitsMess = nbBitsMess;
        return nbBitsMess;
    }

    protected SourceInterface<Boolean> setSource(SourceInterface<Boolean> sourceInterface) {
        // Si c'est déjà une instance de Source, on l'utilise directement
        if (sourceInterface instanceof Source) {
            this.source = (Source<Boolean>) sourceInterface;
        } else {
            // Sinon, on crée un wrapper qui étend Source
            this.source = new Source<Boolean>() {
                @Override
                public Information<Boolean> emettre() throws InformationNonConformeException {
                    sourceInterface.emettre();
                    return getInformationEmise();
                }

                @Override
                public Information<Boolean> getInformationEmise() {
                    return sourceInterface.getInformationEmise();
                }

                @Override
                public void connecter(DestinationInterface<Boolean> destination) {
                    sourceInterface.connecter(destination);
                }
            };
        }
        return sourceInterface;
    }

    protected DestinationInterface<Boolean> setDestination(DestinationInterface<Boolean> destinationInterface) {
        // Si c'est déjà une instance de Destination, on l'utilise directement
        if (destinationInterface instanceof Destination) {
            this.destination = (Destination<Boolean>) destinationInterface;
        } else {
            // Sinon, on crée un wrapper qui étend Destination
            this.destination = new Destination<Boolean>() {
                @Override
                public Information<Boolean> recevoir(Information<Boolean> information) throws InformationNonConformeException {
                    destinationInterface.recevoir(information);
                    return getInformationRecue();
                }

                @Override
                public Information<Boolean> getInformationRecue() {
                    return destinationInterface.getInformationRecue();
                }
            };
        }
        return destinationInterface;
    }


    /**
     * La méthode analyseArguments extrait d'un tableau de chaînes de
     * caractères les différentes options de la simulation.  <br>Elle met
     * à jour les attributs correspondants du Simulateur.
     *
     * @param args le tableau des différents arguments.
     *             <br>
     *             <br>Les arguments autorisés sont :
     *             <br>
     *             <dl>
     *             <dt> -mess m  </dt><dd> m (String) constitué de 7 ou plus digits à 0 | 1, le message à transmettre</dd>
     *             <dt> -mess m  </dt><dd> m (int) constitué de 1 à 6 digits, le nombre de bits du message "aléatoire" à transmettre</dd>
     *             <dt> -s </dt><dd> pour demander l'utilisation des sondes d'affichage</dd>
     *             <dt> -seed v </dt><dd> v (int) d'initialisation pour les générateurs aléatoires</dd>
     *             <dt> -form f </dt><dd> f (NRZ, NRZT, RZ) : forme de transmission analogique</dd>
     *             <dt> -nbEch ne </dt><dd> ne (int >2) : nombre d'échantillons pour la transmission analogique</dd>
     *             <dt> -ampl min max </dt><dd> min et max (float) : amplitude minimale et maximale pour la transmission analogique</dd>
     *             <dt> -snrpb s </dt><dd> s (float) : rapport signal/bruit par bit en dB</dd>
     *             <dt> -ti dt ar </dt><dd> paramètres des trajets multiples (1 à 5 couples dt/ar)</dd>
     *             <dt> -codeur </dt><dd> active le codage canal</dd>
     *             </dl>
     * @throws ArgumentsException si un des arguments est incorrect.
     */
    protected void analyseArguments(String[] args) throws ArgumentsException {

        for (int i = 0; i < args.length; i++) { // traiter les arguments 1 par 1

            if (args[i].matches("-s")) {
                affichage = true;
            } else if (args[i].matches("-seed")) {
                aleatoireAvecGerme = true;
                i++;
                // traiter la valeur associee
                try {
                    seed = Integer.valueOf(args[i]);
                } catch (Exception e) {
                    throw new ArgumentsException("Valeur du parametre -seed  invalide :" + args[i]);
                }
            } else if (args[i].matches("-mess")) {
                i++;
                // traiter la valeur associee
                messageString = args[i];
                if (args[i].matches("[0,1]{7,}")) {
                    messageAleatoire = false;
                    nbBitsMess = args[i].length();
                } else if (args[i].matches("[0-9]{1,6}")) { // de 1 à 6 chiffres
                    messageAleatoire = true;
                    nbBitsMess = Integer.valueOf(args[i]);
                    if (nbBitsMess < 1)
                        throw new ArgumentsException("Valeur du parametre -mess invalide : " + nbBitsMess);
                } else throw new ArgumentsException("Valeur du parametre -mess invalide : " + args[i]);
            } else if (args[i].matches("-form")) {
                transmissionAnalogique = true;
                i++;
                // traiter la valeur associee
                if (args[i].toUpperCase().matches("NRZ|NRZT|RZ")) {
                    if (args[i].toUpperCase().matches("NRZ")) {
                        form = Mode.NRZ;
                    } else if (args[i].toUpperCase().matches("NRZT")) {
                        form = Mode.NRZT;
                    } else if (args[i].toUpperCase().matches("RZ")) {
                        form = Mode.RZ;
                    }
                } else {
                    throw new ArgumentsException("Valeur du parametre -form invalide : " + args[i]);
                }
            } else if (args[i].matches("-nbEch")) {
                transmissionAnalogique = true;
                // traiter la valeur associee
                i++;
                if ((i < args.length) && (args[i].toUpperCase().matches("[1-9]\\d*"))) {
                    nbEch = Integer.parseInt(args[i]);
                    if(nbEch <= 2) throw new ArgumentsException("Valeur du parametre -nbEch invalide : " + args[i]
                            + "doit être strictement supérieur à 2");
                    if(nbEch % 3 != 0) throw new ArgumentsException("Valeur du parametre -nbEch invalide : " + args[i] +
                            ". Le nbEch doit être multiple de 3");
                } else {
                    nbEch = 30;
                }
            } else if (args[i].matches("-ampl")) {
                transmissionAnalogique = true;
                if (i + 2 >= args.length) {
                    throw new ArgumentsException("Pas assez d'arguments après -ampl");
                }
                String minString = args[++i];
                String maxString = args[++i];
                try {
                    float min = Float.parseFloat(minString);
                    float max = Float.parseFloat(maxString);
                    if (min > max) throw new ArgumentsException("Valeur du parametre -ampl invalide : " + minString);
                    amplitude[0] = min;
                    amplitude[1] = max;
                } catch (Exception e) {
                    throw new ArgumentsException("Valeur du parametre -ampl invalide : " + args[i]);
                }
            } else if(args[i].matches("-snrpb")) {
                transmissionAnalogique = true;
                i++;
                try{
                    snrpb = Float.parseFloat(args[i]); //on récupère la valeur du snr (en dB)
                } catch(Exception e) {
                    throw new ArgumentsException("Valeur du parametre -snrpb  invalide :" + args[i]);
                }
            } else if(args[i].matches("-ti")) {
                transmissionAnalogique = true;
                params = new LinkedList<>();

                int j=1;
                while(i+j < args.length && args[i+j].matches("[0-9.]*")){
                    j++;
                }
                int nbTrajets =  j / 2; //nombre d'arguments de -ti / nombre de trajets (paire d'arguments)
                if (nbTrajets <= 0 || nbTrajets > 5) {
                    throw new ArgumentsException("Nombre incorrect de couples dt/ar après -ti (doit être entre 1 et 5)");
                }
                for (int k = 0; k < nbTrajets; k++) {
                    if (i + k*2 + 2 >= args.length) {
                        throw new ArgumentsException("Pas assez d'arguments après -ti");
                    }
                    try {
                        int dt = Integer.parseInt(args[i + k*2 + 1]);
                        float ar = Float.parseFloat(args[i + k*2 + 2]);
                        params.add(new ParametreTrajet(dt, ar));
                    } catch (Exception e) {
                        throw new ArgumentsException("Valeur incorrecte pour les paramètres dt/ar après -ti");
                    }
                }
                if (nbEch == 0){
                    nbEch = 30;
                }
                i+=nbTrajets*2;
            } else if (args[i].matches("-codeur")) {
                codageCanal = true;
            }

            //TODO : ajouter ci-après le traitement des nouvelles options

            else throw new ArgumentsException("Option invalide :" + args[i]);
        }
    }


    /**
     * La méthode execute effectue un envoi de message par la source
     * de la chaîne de transmission du Simulateur.
     *
     * @throws Exception si un problème survient lors de l'exécution
     */
    public void execute() throws Exception {
        source.emettre();
    }


    /**
     * La méthode qui calcule le taux d'erreur binaire en comparant
     * les bits du message émis avec ceux du message reçu.
     *
     * @return La valeur du Taux dErreur Binaire.
     */
    public float calculTauxErreurBinaire() {
        Information<Boolean> informationEmise = source.getInformationEmise();
        Information<Boolean> informationRecue = destination.getInformationRecue();

        float nbErreurs = 0.0f;

        for (int i = 0; i < nbBitsMess; i++) {
            if (!informationEmise.iemeElement(i).equals(informationRecue.iemeElement(i))) {
                nbErreurs++;
            }
        }
        return nbErreurs / nbBitsMess;
    }

    /**
     * Calcul du SNR en dB (puissance du signal sur puissance du bruit)
     *
     * @return valeur float du SNR en dB
     */
    public float calculSNRdB(){
        //Récupération signaux analogiques
        Information<Float> emis = codeur.getInformationEmise();
        Information<Float> recu = decodeur.getInformationRecue();
        Bruit bruit;

        if (transmetteurAnalogique instanceof TransmetteurBruite) {
            bruit = ((TransmetteurBruite) transmetteurAnalogique).getBruit();
        }
        else {
            bruit = new Bruit(recu.nbElements());
        }


        double pSignal = emis.calculerPuissance();
        double pBruit = bruit.calculerPuissance();
        double snrLin = 0.0;


        snrLin = pSignal/pBruit;
        if(snrLin == 1){
            return 0;
        }
        return (float) (10* log10(snrLin));
    }


    /**
     * La fonction main instancie un Simulateur à l'aide des
     * arguments paramètres et affiche le résultat de l'exécution
     * d'une transmission.
     *
     * @param args les différents arguments qui serviront à l'instanciation du Simulateur.
     */
    public static void main(String[] args) {

        Simulateur simulateur = null;

        try {
            simulateur = new Simulateur(args);
        } catch (Exception e) {
            System.out.println(e);
            System.exit(-1);
        }

        try {
            simulateur.execute();
            String s = "java  Simulateur  ";
            for (int i = 0; i < args.length; i++) { //copier tous les paramètres de simulation
                s += args[i] + "  ";
            }
            System.out.println(s + "  =>   TEB : " + simulateur.calculTauxErreurBinaire());
//            System.out.println("Information Emise : " + simulateur.source.getInformationEmise());
//            System.out.println("Information Recue : " + simulateur.destination.getInformationRecue());
//            System.out.println("Erreur : " + (simulateur.calculSNRdB() + 10*log10(simulateur.nbEch) - 3));
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            System.exit(-2);
        }
    }


}

