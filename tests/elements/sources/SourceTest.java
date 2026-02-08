package elements.sources;

import information.Information;
import information.InformationNonConformeException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import elements.transmetteurs.TransmetteurParfait;

import static org.hamcrest.core.Is.is;

public class SourceTest {

    @Rule
    public ErrorCollector collector= new ErrorCollector();

    Information information;
    SourceFixe fixe;
    SourceAleatoire aleatoire1;
    SourceAleatoire aleatoire2;
    TransmetteurParfait t1;
    TransmetteurParfait t2;
    TransmetteurParfait t3;

    @Before
    public void setUp() throws InformationNonConformeException {
        information = new Information();
        information.add(true);
        fixe = new SourceFixe(information);
        aleatoire1 = new SourceAleatoire(8);
        aleatoire2 = new SourceAleatoire(8,2);
        t1 = new TransmetteurParfait();
        t2 = new TransmetteurParfait();
        t3 = new TransmetteurParfait();
        aleatoire1.connecter(t1);
        aleatoire2.connecter(t2);
        fixe.connecter(t3);
        aleatoire1.emettre();
        aleatoire2.emettre();
        fixe.emettre();
    }

    @Test
    public void testEmettre() throws InformationNonConformeException {
        collector.checkThat("Test emmetre 1", aleatoire1.informationEmise, is(aleatoire1.informationGeneree));
        collector.checkThat("Test emmetre 2", aleatoire2.informationEmise, is(aleatoire2.informationGeneree));
        collector.checkThat("Test emmetre 3", fixe.informationEmise, is(fixe.informationGeneree));
    }

    @Test
    public void testGetInformationEmise() throws InformationNonConformeException {
        collector.checkThat("Test getInformationEmise 1", aleatoire1.getInformationEmise(),is(aleatoire1.informationEmise));
        collector.checkThat("Test getInformationEmise 2", aleatoire2.getInformationEmise(),is(aleatoire2.informationEmise));
        collector.checkThat("Test getInformationEmise 3", fixe.getInformationEmise(),is(fixe.informationEmise));
    }

    @Test
    public void testConnecter() {
        collector.checkThat("Test connecter 1", aleatoire1.destinationsConnectees.size() ,is(1));
        collector.checkThat("Test connecter 2", aleatoire1.destinationsConnectees.contains(t1),is(true));
        collector.checkThat("Test connecter 2", aleatoire1.destinationsConnectees.contains(t2),is(false));
        aleatoire1.connecter(t2);
        collector.checkThat("Test connecter 2", aleatoire1.destinationsConnectees.size() ,is(2));
        collector.checkThat("Test connecter 2", aleatoire1.destinationsConnectees.contains(t2),is(true));

    }

    @Test
    public void testDeconnecter() {
        collector.checkThat("Test deconnecter 1", aleatoire2.destinationsConnectees.size() ,is(1));
        collector.checkThat("Test connecter 2", aleatoire2.destinationsConnectees.contains(t1),is(false));
        collector.checkThat("Test connecter 2", aleatoire2.destinationsConnectees.contains(t2),is(true));
        aleatoire2.deconnecter(t2);
        collector.checkThat("Test deconnecter 2", aleatoire2.destinationsConnectees.size() ,is(0));
        collector.checkThat("Test connecter 2", aleatoire2.destinationsConnectees.contains(t1),is(false));
        collector.checkThat("Test connecter 2", aleatoire2.destinationsConnectees.contains(t2),is(false));
    }
}