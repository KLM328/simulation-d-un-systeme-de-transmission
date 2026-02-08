package elements.transmetteurs;

import information.Information;
import information.InformationNonConformeException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.is;

public class TransmetteurBruiteTest {

    private TransmetteurBruite transmetteurBruitee1;
    private Information information;

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Before
    public void setUp() throws Exception {
        transmetteurBruitee1 = new TransmetteurBruite();

        LinkedList<Float> l = new LinkedList<>();
        l.add(0f);
        l.add(1f);
        l.add(2f);
        l.add(3f);
        l.add(4f);
        l.add(5f);
        information = new Information(l.toArray());
        transmetteurBruitee1.recevoir(information);
        transmetteurBruitee1.emettre();
    }

    @Test
    public void recevoir() throws InformationNonConformeException {
        transmetteurBruitee1.recevoir(information);
        collector.checkThat("Error recevoir()", transmetteurBruitee1.getInformationRecue(), is(information));
    }

    @Test
    public void emettre() throws InformationNonConformeException {
        collector.checkThat("Error emission", transmetteurBruitee1.emettre(),is(transmetteurBruitee1.getInformationEmise()));
    }

    @Test
    public void bruiter() {
        collector.checkThat("Error bruiter()", transmetteurBruitee1.bruiter(transmetteurBruitee1.getInformationRecue(),0f).nbElements(), is(information.nbElements()));
        collector.checkThat("Error bruiter()", transmetteurBruitee1.bruiter(transmetteurBruitee1.getInformationRecue(),5.6f).nbElements(), is(information.nbElements()));
        collector.checkThat("Error bruiter()", transmetteurBruitee1.bruiter(transmetteurBruitee1.getInformationRecue(),Float.POSITIVE_INFINITY).equals(information), is(true));
        collector.checkThat("Error bruiter()", transmetteurBruitee1.bruiter(transmetteurBruitee1.getInformationRecue(),5.6f).equals(information), is(false));
    }
}