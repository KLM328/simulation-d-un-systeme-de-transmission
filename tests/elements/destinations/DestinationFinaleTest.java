package elements.destinations;

import elements.destinations.DestinationFinale;
import information.Information;
import information.InformationNonConformeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;


public class DestinationFinaleTest {

    private ErrorCollector collector;
    private Information<Boolean> informationRecue;
    private DestinationFinale destinationFinale;

    @Before
    public void setUp() throws Exception {
        destinationFinale = new DestinationFinale();
        informationRecue = new Information<>();
        informationRecue.add(true);
        collector = new ErrorCollector();

    }

    @Test
    public void recevoir() throws InformationNonConformeException {
        destinationFinale.recevoir(informationRecue);
        collector.checkThat("Error reception", destinationFinale.getInformationRecue(), is(informationRecue));
    }

    @Test (expected = InformationNonConformeException.class)
    public void recevoirKO() throws InformationNonConformeException{
        destinationFinale.recevoir(null);
    }
}