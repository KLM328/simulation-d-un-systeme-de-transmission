package information;

public class ParametreTrajet {
    private int tau = 0;
    private float alpha = 0.0f;

    /**
     * Crée un paramètre de trajet avec un délai et un facteur d’atténuation
     *
     * @param tau le délai ajouté
     * @param alpha le facteur d'atténuation (entre 0 et 1)
     * @throws IllegalArgumentException si alpha n'est pas compris entre 0 et 1
     */
    public ParametreTrajet(int tau, float alpha) throws IllegalArgumentException {
        if ((alpha >= 0.0) && (alpha <= 1.0)) {
            this.alpha = alpha;
            this.tau = tau;
        } else {  // AJOUT DU ELSE MANQUANT
            throw new IllegalArgumentException("Le paramètre alpha doit être compris entre 0 et 1");
        }
    }

    /**
     * Retourne le facteur d'atténuation du trajet
     *
     * @return le facteur alpha
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * Retourne le délai du trajet
     *
     * @return le délai tau
     */
    public int getTau() {
        return tau;
    }
}
