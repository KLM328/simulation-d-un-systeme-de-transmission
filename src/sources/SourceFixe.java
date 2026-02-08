package sources;

import information.Information;

public class SourceFixe extends sources.Source<Boolean> {

	private Information<Boolean> informationGeneree;

	 /**
     * Une source qui envoie toujours le même message
     */
	public SourceFixe() {
		informationGeneree = new Information<Boolean>();
        informationGeneree.add(true);
        informationGeneree.add(false);
        informationGeneree.add(true);
        informationGeneree.add(true);
        informationGeneree.add(false);
        informationGeneree.add(true); 
	}
	
	public void recevoir(Information<Boolean> info) {
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	

}
