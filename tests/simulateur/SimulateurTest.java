package simulateur;

import elements.destinations.DestinationInterface;
import information.Information;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import elements.sources.SourceInterface;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class SimulateurTest {
    Simulateur simulateur1;
    Simulateur simulateur2;
    Simulateur simulateur3;
    Simulateur simulateur4;
    Simulateur simulateur5;
    Information<Boolean> information1;
    Information<Boolean> information2;
    Information<Boolean> information3;
    SourceInterface<Boolean> sourceMock;
    DestinationInterface<Boolean> destinationMock;

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Before
    public void setUp() throws Exception {
        simulateur1 = new Simulateur(new String[0]);
        simulateur2 = new Simulateur(new String[] {"-mess", "0101010101010111110100101010", "-seed", "1287879754", "-form", "RZ", "-nbEch","60", "-ampl", "-6", "6", "-snrpb", "4"});
        simulateur3 = new Simulateur(new String[] {"-seed", "1287879754", "-form", "NRZ", "-ampl", "0", "4", "-snrpb", "-1.6"});
        simulateur4 = new Simulateur(new String[] {"-form", "NRZT", "-ampl", "-4", "4", "-snrpb", "Infinity"});
        simulateur5 = new Simulateur(new String[] {"-ampl", "0", "4", "-snrpb", "0"});
        information1 = new Information<>();
        information2 = new Information<>();
        information3 = new Information<>();
        information1.add(true);
        information2.add(true);
        information3.add(false);
        information1.add(true);
        information2.add(false);
        information3.add(false);

        // Création des mocks avec Mockito
        sourceMock = mock(SourceInterface.class);
        destinationMock = mock(DestinationInterface.class);

        // Configuration des comportements des mocks
        when(sourceMock.getInformationEmise()).thenReturn(information1);
        when(destinationMock.getInformationRecue())
                .thenReturn(information1)
                .thenReturn(information2)
                .thenReturn(information3);

        simulateur1.setSource(sourceMock);
        simulateur1.setDestination(destinationMock);
        simulateur1.setNbBitsMess(2);
    }

    @Test
    public void calculTauxErreurBinaire() {
        collector.checkThat("Error TEB", simulateur1.calculTauxErreurBinaire(), is(0.0f));
        collector.checkThat("Error TEB", simulateur1.calculTauxErreurBinaire(), is(0.5f));
        collector.checkThat("Error TEB", simulateur1.calculTauxErreurBinaire(), is(1.0f));
    }

    @Test
    public void analyseArgument() throws ArgumentsException {
        collector.checkThat("Error AnalyseArgument -form", simulateur1.form, is(Mode.RZ));

        collector.checkThat("Error AnalyseArgument -mess", simulateur2.messageString, is("0101010101010111110100101010"));
        collector.checkThat("Error AnalyseArgument -mess", simulateur2.messageAleatoire, is(false));
        collector.checkThat("Error AnalyseArgument -seed", simulateur2.aleatoireAvecGerme, is(true));
        collector.checkThat("Error AnalyseArgument -seed", simulateur2.seed, is(1287879754));
        collector.checkThat("Error AnalyseArgument -form", simulateur2.form, is(Mode.RZ));
        collector.checkThat("Error AnalyseArgument -nbEch", simulateur2.nbEch, is(60));
        collector.checkThat("Error AnalyseArgument -ampl", simulateur2.amplitude, is(new Float[] {-6f, 6f}));
        collector.checkThat("Error AnalyseArgument -snrpb", simulateur2.snrpb, is(4f));


        collector.checkThat("Error AnalyseArgument -mess", simulateur3.messageAleatoire, is(true));
        collector.checkThat("Error AnalyseArgument -form", simulateur3.form, is(Mode.NRZ));
        collector.checkThat("Error AnalyseArgument -ampl", simulateur3.amplitude, is(new Float[] {0f, 4f}));
        collector.checkThat("Error AnalyseArgument -snrpb", simulateur3.snrpb, is(-1.6f));
        collector.checkThat("Error AnalyseArgument -nbEch", simulateur3.nbEch, is(30));
        collector.checkThat("Error AnalyseArgument -seed", simulateur3.seed, is(1287879754));
        collector.checkThat("Error AnalyseArgument -seed", simulateur3.aleatoireAvecGerme, is(true));
        collector.checkThat("Error AnalyseArgument -s", simulateur3.affichage, is(false));


        collector.checkThat("Error AnalyseArgument -mess", simulateur4.messageAleatoire, is(true));
        collector.checkThat("Error AnalyseArgument -form", simulateur4.form, is(Mode.NRZT));
        collector.checkThat("Error AnalyseArgument -ampl", simulateur4.amplitude, is(new Float[] {-4f, 4f}));
        collector.checkThat("Error AnalyseArgument -snrpb", simulateur4.snrpb, is(Float.POSITIVE_INFINITY));
        collector.checkThat("Error AnalyseArgument -nbEch", simulateur4.nbEch, is(30));
        collector.checkThat("Error AnalyseArgument -seed", simulateur4.aleatoireAvecGerme, is(false));
        collector.checkThat("Error AnalyseArgument -s", simulateur4.affichage, is(false));
    }

    @Test
    public void snrpb() throws Exception {
        simulateur2.execute();
        simulateur3.execute();
        simulateur4.execute();
        simulateur5.execute();

        collector.checkThat("Error snrpb = 4dB", Math.abs(simulateur2.calculSNRdB() - simulateur2.snrpb + 10 *Math.log10(simulateur2.nbEch) - 3) < 0.24f ,is(true));
        collector.checkThat("Error snrpb = -1.6dB", Math.abs(simulateur3.calculSNRdB() - simulateur3.snrpb+10 * Math.log10(simulateur3.nbEch) - 3) < 0.2f ,is(true));
        collector.checkThat("Error snrpb = Infinity", simulateur4.calculSNRdB() ,is(Float.POSITIVE_INFINITY));
        collector.checkThat("Error snrpb = 0dB", Math.abs(simulateur5.calculSNRdB() - simulateur5.snrpb+ 10 * Math.log10(simulateur5.nbEch) - 3) < 0.2f ,is(true));

    }


}
