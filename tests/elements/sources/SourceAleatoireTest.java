package elements.sources;

import elements.sources.SourceAleatoire;
import information.InformationNonConformeException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.core.Is.is;

public class SourceAleatoireTest {

    @Rule
    public ErrorCollector collector= new ErrorCollector();

    SourceAleatoire aleatoire1;
    SourceAleatoire aleatoire2;

    @Before
    public void setUp() throws InformationNonConformeException {
        aleatoire1 = new SourceAleatoire(8);
        aleatoire2 = new SourceAleatoire(8,2);
    }

    @Test
    public void testEmettre() throws InformationNonConformeException {
        aleatoire1.emettre();
        aleatoire2.emettre();
        collector.checkThat("Test emmetre 1", aleatoire1.informationEmise, is(aleatoire1.informationGeneree));
        collector.checkThat("Test emmetre 2", aleatoire2.informationEmise, is(aleatoire2.informationGeneree));
    }
}