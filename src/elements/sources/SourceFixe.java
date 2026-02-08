package elements.sources;

import information.Information;

import java.util.Objects;

public class SourceFixe extends Source<Boolean>{

	 /**
     * Une source qui envoie toujours le même message
     */
	public SourceFixe(Information<Boolean> information) {
		super();
		informationGeneree = information;
	}

	/**
	 * Construit une source fixe à partir d'une chaîne de 0 et 1
	 * @param motif chaîne de 0 et 1
	 */
	public SourceFixe(String motif) {
		super();
		Objects.requireNonNull(motif, "motif ne doit pas être null");
		Boolean[] data = new Boolean[motif.length()];
		for (int i = 0; i < motif.length(); i++) {
			char c = motif.charAt(i);
			if (c == '0') data[i] = Boolean.FALSE;
			else if (c == '1') data[i] = Boolean.TRUE;
			else throw new IllegalArgumentException("motif invalide: uniquement '0' ou '1'");
		}
		this.informationGeneree = new Information<>(data);
	}


	public SourceFixe(){
		super();
		informationGeneree = new Information<>();
	}
}
