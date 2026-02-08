package information;

import bruits.Bruit;

import java.util.*;

import static java.lang.Math.pow;

/**
 * @author prou
 */
public class Information<T> implements Iterable<T> {

    private ArrayList<T> content;

    /**
     * pour construire une information vide
     */
    public Information() {
        this.content = new ArrayList<>();
    }

    /**
     * pour construire à partir d'un tableau de T une information
     *
     * @param content le tableau d'éléments pour initialiser l'information construite
     */
    public Information(T[] content) {
        super();
        this.content = new ArrayList<>();
        this.add(content);
    }

    /**
     * pour construire à partir d'une information
     *
     * @param information une information a copier
     */
    public Information(Information<T> information) {
        super();
        this.content = new ArrayList<>(information.content);
    }

    /**
     * pour connaître le nombre d'éléments d'une information
     *
     * @return le nombre d'éléments de l'information
     */
    public int nbElements() {
        return this.content.size();
    }

    /**
     * pour savoir si une onformation est vide
     *
     * @return true si l'information est vide
     */
    public boolean estVide() {
        return this.content.isEmpty();
    }

    /**
     * pour renvoyer un élément d'une information
     *
     * @param i le rang de l'information à renvoyer (à partir de 0)
     * @return le ieme élément de l'information
     */
    public T iemeElement(int i) {
        return this.content.get(i);
    }

    /**
     * pour connaitre le contenu
     *
     * @return content
     */
    public ArrayList<T> getContent(){
        return content;
    }

    /**
     * pour modifier le ième élément d'une information
     *
     * @param i le rang de l'information à modifier (à partir de 0)
     * @param v la nouvelle ieme information
     */
    public void setIemeElement(int i, T v) {
        this.content.set(i, v);
    }

    /**
     * pour ajouter un élément à la fin de l'information
     *
     * @param valeur l'élément à rajouter
     */
    public void add(T valeur) {
        this.content.add(valeur);
    }

    /**
     * pour ajouter des éléments à la fin de l'information
     *
     * @param valeurs tableau d'éléments à rajouter
     */
    public void add(T[] valeurs) {
        for (T valeur : valeurs) {
            this.add(valeur);
        }
    }


    /**
     * pour comparer l'information courante avec une autre information
     *
     * @param o l'information  avec laquelle se comparer
     * @return "true" si les 2 informations contiennent les mêmes
     * éléments aux mêmes places; "false" dans les autres cas
     */
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        if (!(o instanceof Information))
            return false;
        Information<T> information = (Information<T>) o;
        if (this.nbElements() != information.nbElements())
            return false;
        for (int i = 0; i < this.nbElements(); i++) {
            if (!this.iemeElement(i).equals(information.iemeElement(i)))
                return false;
        }
        return true;
    }


    /**
     * pour afficher une information
     *
     * @return representation de l'information sous forme de String
     */
    public String toString() {
        String s = "";
        for (int i = 0; i < this.nbElements(); i++) {
            s += " " + this.iemeElement(i);
        }
        return s;
    }

    /**
     * Ajoute un bruit à chaque élément de l’information
     *
     * @param bruit le bruit à ajouter
     * @return l'information bruitée
     */
    @SuppressWarnings("unchecked")
    public Information<T> ajouterBruit(Bruit bruit) {
        if (!(this.iemeElement(0) instanceof Float)) {
            throw new UnsupportedOperationException("ajouterBruit n'est disponible que pour les Information<Float>");
        }
        if (bruit.nbElements() != this.nbElements()) {
            throw new SizeInformationException("La taille du bruit est différente de la taille de l'information");
        }
        for (int i = 0; i < this.nbElements(); i++) {
            Float val = (Float) this.iemeElement(i) + (Float) bruit.iemeElement(i);
            this.setIemeElement(i, (T) val);
        }
        return this;
    }

    /**
     * Additionne élément par élément l’information donnée à cette instance
     *
     * @param information information à sommer
     */
    @SuppressWarnings("unchecked")
    public void somme(Information<T> information) {
        // Vérification du type
        if (this.nbElements() > 0 && !(this.iemeElement(0) instanceof Float)) {
            throw new UnsupportedOperationException("somme n'est disponible que pour les Information<Float>");
        }

        int max = Math.max(this.nbElements(), information.nbElements());

        for (int i = 0; i < max; i++) {
            if (i < this.nbElements()) {
                if (i < information.nbElements()) {
                    Float valeur1 = (Float) this.iemeElement(i);
                    Float valeur2 = (Float) information.iemeElement(i);
                    this.setIemeElement(i, (T) (Float) (valeur1 + valeur2));
                }
                // Si information est plus courte, on garde la valeur actuelle
            } else {
                // Si this est plus court, on ajoute les éléments restants de information
                this.add((T) information.iemeElement(i));
            }
        }
    }

    /**
     * Génère une version retardée et atténuée de l’information selon un trajet donné.
     *
     * @param parametreTrajet paramètre du trajet (tau et alpha)
     * @return information retardée et atténuée
     */
    @SuppressWarnings("unchecked")
    public Information<Float> genererInformationRetardee(ParametreTrajet parametreTrajet) {
        // Vérifications préliminaires
        if (parametreTrajet == null) {
            throw new IllegalArgumentException("ParametreTrajet ne peut pas être null");
        }

        // Vérification du type
        if (this.nbElements() > 0 && !(this.iemeElement(0) instanceof Float)) {
            throw new UnsupportedOperationException("genererInformationRetardee n'est disponible que pour les Information<Float>");
        }

        Information<Float> informationRetardee = new Information<>();

        // Ajout du délai (tau échantillons à 0)
        for (int i = 0; i < parametreTrajet.getTau(); i++) {
            informationRetardee.add(0.0f);
        }

        // Ajout des données atténuées par le facteur alpha
        for (int i = 0; i < this.nbElements(); i++) {
            Float valeur = (Float) this.iemeElement(i);
            Float valeurAttenuee = (float) (parametreTrajet.getAlpha() * valeur);
            informationRetardee.add(valeurAttenuee);
        }

        return informationRetardee;
    }



    /**
     * Pour calculer la puissance du signal seulement possible sur une Information<Float>
     *
     * @return puissance du signal
     */
    public double calculerPuissance() {
        if (this.nbElements() == 0) {
            return 0.0;
        }
        double puissanceTotale= 0;
        for (int i = 0; i < this.nbElements(); i++) {
            if (!(this.iemeElement(i) instanceof Float)) {
                throw new UnsupportedOperationException("calculerPuissance n'est disponible que pour les Information<Float>");
            }else {
                puissanceTotale = puissanceTotale + ((float) this.iemeElement(i) *(float) this.iemeElement(i));
            }
        }
        return puissanceTotale/this.nbElements();
    }

    /**
     * Pour utilisation du "for each"
     */
    public Iterator<T> iterator() {
        return content.iterator();
    }


}
