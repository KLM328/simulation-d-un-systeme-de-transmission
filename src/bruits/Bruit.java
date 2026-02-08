package bruits;

import information.Information;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;

/**
 * La classe Bruit représente un bruit sous forme d'une information contenant des valeurs flottantes.
 * Elle étend la classe Information<Float> et fournit des méthodes pour analyser les propriétés statistiques du bruit.
 */
public class Bruit extends Information<Float> {

    /**
     * Constructeur pour créer un bruit avec un nombre spécifique d'éléments.
     * Chaque élément est initialisé à 0.
     *
     * @param nbElement Le nombre d'éléments dans le bruit.
     */
    public Bruit(int nbElement) {
        super(new Float[nbElement]);
        for (int i = 0; i < nbElement; i++) {
            setIemeElement(i, 0f);
        }
    }

    /**
     * Calcule la moyenne des valeurs du bruit.
     *
     * @return La moyenne des valeurs du bruit.
     */
    public double mean(){
        double sum = 0;
        for(float f : getContent()){
            sum += f;
        }
        return sum/nbElements();
    }
    /**
     * Calcule la distribution des valeurs du bruit en fonction d'une précision donnée.
     * Les valeurs sont regroupées en intervalles de taille égale à la précision.
     *
     * @param precision La taille des intervalles pour regrouper les valeurs.
     * @return Une carte (map) où les clés sont les valeurs arrondies et les valeurs sont les comptages des occurrences.
     */
    public Map<Double, Integer> distribution(double precision){
        Map<Double, Integer> repartition = new HashMap<>();

        for (int i = 0; i < nbElements(); i++) {
            double key = floor(iemeElement(i)/precision)*precision;
            repartition.merge(key, 1, Integer::sum);
        }
        return repartition;
    }

    /**
     * Calcule la variance des valeurs du bruit.
     * La variance est calculée en utilisant la distribution des valeurs avec une précision de 0.01.
     *
     * @return La variance des valeurs du bruit.
     */
    public double variance(){
        Map<Double, Integer> distribution = distribution(0.01f);
        Map<Double, Float> probabilite = new HashMap<>();
        for(double f : distribution.keySet()){
            probabilite.put(f, distribution.get(f)/(float) nbElements());
        }
        double sum = 0;
        for(double f : probabilite.keySet()){
            sum += f*f*probabilite.get(f);
        }
        return sum - pow(mean(), 2);
    }

    /**
     * Calcule l'écart-type des valeurs du bruit.
     * L'écart-type est la racine carrée de la variance.
     *
     * @return L'écart-type des valeurs du bruit.
     */
    public double ecartType(){
        return sqrt(variance());
    }
}
