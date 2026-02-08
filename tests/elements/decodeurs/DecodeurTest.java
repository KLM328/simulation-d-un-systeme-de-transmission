package elements.decodeurs;

import information.Information;
import information.InformationNonConformeException;
import information.SizeInformationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import simulateur.Mode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class DecodeurTest {

    private Information<Float> informationRecue;
    private Information<Boolean> informationAttenduDecodage;
    private Information<Boolean> informationAttenduDecodage2;
    private Information<Boolean> informationDecodage;

    private int nbEch = 30;
    private Float[] amplitude = {0.0f, 1.0f};
    private Decodeur decodeur;

    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();

    @Before
    public void setUp() {
        informationAttenduDecodage = new Information<>();
        informationAttenduDecodage2 = new Information<>();
        informationDecodage = new Information<>();

        informationAttenduDecodage.add(true);
        informationAttenduDecodage.add(false);
        informationAttenduDecodage.add(true);
        informationAttenduDecodage.add(false);
        informationAttenduDecodage.add(true);
        informationAttenduDecodage.add(false);
        informationAttenduDecodage.add(true);
        informationAttenduDecodage.add(false);
        informationAttenduDecodage.add(true);

        informationAttenduDecodage2.add(true);
        informationAttenduDecodage2.add(false);
        informationAttenduDecodage2.add(true);
        informationAttenduDecodage2.add(false);
        informationAttenduDecodage2.add(true);
    }

    @Test
    public void testDecodeNRZ() throws InformationNonConformeException {
        decodeur = new Decodeur(Mode.NRZ,  nbEch, amplitude, false);
        boolean[] messageTest = {true, false, true, true, false, false, true, false, true};
        Information<Boolean> message = toInfo(messageTest);

        Information<Float> trame = genererSignalNRZ(message);
        decodeur.recevoir(trame);

        Information<Boolean> sortie = decodeur.getInformationEmise();
        assertEquals("NRZ : problème de décodage", message, sortie);
    }

    @Test
    public void testDecodeNRZT() throws InformationNonConformeException {
        decodeur = new Decodeur(Mode.NRZT,  nbEch, amplitude, false);
        boolean[] messageBool = {true, true, false};
        Information<Boolean> message = toInfo(messageBool);

        Information<Float> signal = genererSignalNRZT(message);
        decodeur.recevoir(signal);

        Information<Boolean> sortie = decodeur.getInformationEmise();
        assertEquals("NRZT : problème de décodage", message, sortie);
    }

    @Test
    public void testDecodeRZ() throws InformationNonConformeException {
        boolean[] messageTest = { false, true, true, false, true, false, false, true, true, false };
        Information<Boolean> message = toInfo(messageTest);

        decodeur = new Decodeur(Mode.RZ, nbEch, amplitude, false);
        Information<Float> trame = genererSignalRZ(message);
        decodeur.recevoir(trame);

        Information<Boolean> sortie = decodeur.getInformationEmise();
        assertEquals("RZ : problème de décodage", message, sortie);
    }

    @Test
    public void testDecodeTransitionsNRZT() throws InformationNonConformeException {
        boolean[] messageTest = {
                true,true,true,
                true,true,false,
                false,true,true,
                false,true,false,
                true,false,true,
                true,false,false,
                false,false,true,
                false,false,false
        };
        Information<Boolean> message = toInfo(messageTest);

        decodeur = new Decodeur(Mode.NRZT, nbEch, amplitude, false);
        Information<Float> trame = genererSignalNRZT(message);
        decodeur.recevoir(trame);

        Information<Boolean> sortie = decodeur.getInformationEmise();
        assertEquals("NRZT : problème de décodage", message, sortie);
    }

    // ---------- Méthodes pour générer des signaux ----------
    private Information<Float> genererSignalRZ(Information<Boolean> val) {
        informationRecue = new Information<>();
        int t = nbEch / 3;
        int fin = nbEch - 2 * t;
        for (boolean b : val) {
            for (int i = 0; i < t; i++) informationRecue.add(amplitude[0]);
            for (int i = 0; i < t; i++) informationRecue.add(b ? amplitude[1] : amplitude[0]);
            for (int i = 0; i < fin; i++) informationRecue.add(amplitude[0]);
        }
        return informationRecue;
    }

    private Information<Float> genererSignalNRZ(Information<Boolean> val) {
        informationRecue = new Information<>();
        for (boolean b : val) {
            float toChange = b ? amplitude[1] : amplitude[0];
            for (int i = 0; i < nbEch; i++) informationRecue.add(toChange);
        }
        return informationRecue;
    }

    private Information<Float> genererSignalNRZT(Information<Boolean> signal) {
        Information<Float> message = new Information<>();
        float min = amplitude[0], max = amplitude[1];
        int transition = Math.max(1, nbEch / 3);
        int plateau = Math.max(0, nbEch - 2 * transition);

        int n = signal.nbElements();
        for (int i = 0; i < n; i++) {
            boolean valeur = signal.iemeElement(i);
            boolean precedent = (i == 0) ? valeur : signal.iemeElement(i - 1);
            boolean suivant = (i == n - 1) ? valeur : signal.iemeElement(i + 1);

            float haut = valeur ? max : min;
            float bas = valeur ? min : max;

            boolean valPrecedente = (valeur == precedent);
            boolean valSuivante = (valeur == suivant);

            if (valPrecedente && valSuivante) {
                for (int j = 0; j < nbEch; j++) message.add(haut);
            } else if (valPrecedente && !valSuivante) {
                for (int j = 0; j < transition + plateau; j++) message.add(haut);
                pente(message, haut, bas, transition);
            } else if (!valPrecedente && valSuivante) {
                pente(message, bas, haut, transition);
                for (int j = 0; j < transition + plateau; j++) message.add(haut);
            } else {
                pente(message, bas, haut, transition);
                for (int j = 0; j < plateau; j++) message.add(haut);
                pente(message, haut, bas, transition);
            }
        }
        return message;
    }

    private static void pente(Information<Float> message, float a, float b, int pas) {
        if (pas <= 1) { message.add(b); return; }
        for (int i = 0; i < pas; i++) {
            float result = (float) i / (pas - 1);
            message.add(a + (b - a) * result);
        }
    }

    private static Information<Boolean> toInfo(boolean[] tab) {
        Information<Boolean> info = new Information<>();
        for (boolean b : tab) info.add(b);
        return info;
    }

    // ---------- Tests pour decodeCanal ----------
    @Test
    public void testDecodeCanal_OK() throws Throwable {
        decodeur = new Decodeur(Mode.NRZ, nbEch, amplitude, true);

        boolean[] bits = {
                false,false,false,
                false,false,true,
                true,true,false,
                true,true,true
        };
        Information<Float> analog = genererSignalNRZ(toInfo(bits));

        Information<Boolean> attendu = new Information<>();
        attendu.add(false);
        attendu.add(true);
        attendu.add(false);
        attendu.add(true);

        Information<Boolean> res = invokeDecodeCanal(analog);

        assertNotNull("decodeCanal doit retourner un résultat non nul", res);
        assertEquals("Contenu décodé incorrect", attendu, res);
    }

    @Test
    public void testDecodeCanal_TailleNonMultipleDe3() {
        decodeur = new Decodeur(Mode.NRZ, nbEch, amplitude, true);

        boolean[] bits = { true, false, true, false, true };
        Information<Float> analog = genererSignalNRZ(toInfo(bits));

        try {
            invokeDecodeCanal(analog);
            fail("Une SizeInformationException devait être levée (taille non multiple de 3).");
        } catch (Throwable t) {
            assertTrue("Cause attendue: SizeInformationException",
                    t instanceof SizeInformationException);
        }
    }

    // ---------- Méthodes pour invoquer decodeCanal via reflection ----------
    private Method getDecodeCanalMethod() throws NoSuchMethodException {
        Method m = Decodeur.class.getDeclaredMethod("decodeCanal", Information.class);
        m.setAccessible(true);
        return m;
    }

    private Information<Boolean> invokeDecodeCanal(Information<Float> entree) throws Throwable {
        Method m = getDecodeCanalMethod();
        try {
            Object res = m.invoke(decodeur, entree);
            return (Information<Boolean>) res;
        } catch (InvocationTargetException ite) {
            throw ite.getCause();
        }
    }
}