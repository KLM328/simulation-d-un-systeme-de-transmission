package elements.transmetteurs;

import elements.destinations.DestinationFinale;
import information.Information;
import information.InformationNonConformeException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.is;


public class TransmetteurParfaitTest {

    private TransmetteurParfait transmetteurParfait1;
    private DestinationFinale destination1;
    private DestinationFinale destination2;
    private Information information;

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Before
    public void setUp() throws Exception {
        transmetteurParfait1 = new TransmetteurParfait();
        destination1 = new DestinationFinale();
        destination2 = new DestinationFinale();
        transmetteurParfait1.connecter(destination1);

        LinkedList<Boolean> l = new LinkedList<>();
        l.add(true);
        l.add(false);
        l.add(false);
        l.add(false);
        l.add(true);
        information = new Information(l.toArray());
        transmetteurParfait1.recevoir(information);
        transmetteurParfait1.emettre();
    }

    @Test
    public void recevoir() throws InformationNonConformeException {
        transmetteurParfait1.recevoir(information);
        collector.checkThat("Error recevoir()", transmetteurParfait1.getInformationRecue(), is(information));
    }

    @Test
    public void emettre() throws InformationNonConformeException {
        transmetteurParfait1.recevoir(information);
        transmetteurParfait1.emettre();
        collector.checkThat("Error emission", transmetteurParfait1.getInformationEmise(),is(information));
    }

    @Test
    public void connecter() {
        collector.checkThat("Error connecter", transmetteurParfait1.getDestinationsConnectees().contains(destination1), is(true));
        collector.checkThat("Error connecter", transmetteurParfait1.getDestinationsConnectees().contains(destination2), is(false));
        collector.checkThat("Error connecter", transmetteurParfait1.getDestinationsConnectees().size() ,is(1));
        transmetteurParfait1.connecter(destination2);
        collector.checkThat("Error connecter", transmetteurParfait1.getDestinationsConnectees().contains(destination1), is(true));
        collector.checkThat("Error connecter", transmetteurParfait1.getDestinationsConnectees().contains(destination2), is(true));
        collector.checkThat("Error connecter", transmetteurParfait1.getDestinationsConnectees().size() ,is(2));

    }

    @Test
    public void deconnecter() {
        collector.checkThat("Error deconnecter", transmetteurParfait1.getDestinationsConnectees().contains(destination1), is(true));
        collector.checkThat("Error deconnecter", transmetteurParfait1.getDestinationsConnectees().contains(destination2), is(false));
        collector.checkThat("Error deconnecter", transmetteurParfait1.getDestinationsConnectees().size() ,is(1));
        transmetteurParfait1.deconnecter(destination1);
        collector.checkThat("Error deconnecter", transmetteurParfait1.getDestinationsConnectees().contains(destination1), is(false));
        collector.checkThat("Error deconnecter", transmetteurParfait1.getDestinationsConnectees().contains(destination2), is(false));
        collector.checkThat("Error deconnecter", transmetteurParfait1.getDestinationsConnectees().size() ,is(0));
    }

    @Test
    public void getInformationRecue(){
        collector.checkThat("Error getInformationRecue()", transmetteurParfait1.getInformationRecue(), is(information));
    }

    @Test
    public void getInformationEmise(){
        collector.checkThat("Error getInformationEmise()", transmetteurParfait1.getInformationEmise(), is(information));
    }
}