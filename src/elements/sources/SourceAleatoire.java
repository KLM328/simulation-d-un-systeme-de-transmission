    package elements.sources;

    import java.util.Random;

    import information.*;

    public class SourceAleatoire extends Source<Boolean> {

        private int nbBits;
        private Random random;

        /**
         * Une source qui envoie un message aléatoire sur un nombre de bits fixés
         *
         * @param nbBits détermine la taille du message
         */
        public SourceAleatoire(int nbBits) {
            super();
            this.nbBits = nbBits;
            this.random = new Random();
        }

        /**
         * Une source qui envoie un message aléatoire sur un nombre de bits fixés
         *
         * @param nbBits détermine la taille du message
         * @param seed détermine la semence à utiliser pour le rendom
         */
        public SourceAleatoire(int nbBits, Integer seed) {
            super();
            this.nbBits = nbBits;
            this.random = new Random();
            random.setSeed(seed);
        }

         /**
          * Emission du message aléatoire en utilisant la fonction de la classe mère
          */
        public Information<Boolean> emettre() throws InformationNonConformeException {
            genererMessageAleatoire();
            super.emettre();
            return getInformationEmise();
        }

        /**
         * Permet de générer le message aléatoire
         */
        private void genererMessageAleatoire() {
            informationGeneree = new Information<Boolean>();
            for (int i = 0; i < nbBits; i++) { // on itère sur la taille du message
                informationGeneree.add(random.nextBoolean()); // ajout aléatoire de 0 ou 1
            }
        }

    }
