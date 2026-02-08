package information;

import bruits.Bruit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;

public class InformationTest {

    Information i1;
    Information i2;
    Information i3;
    Information i4;
    Information i5;
    Information i6;
    Information i7;
    Information i8;
    Information i9;
    ParametreTrajet parametreTrajet;
    ParametreTrajet parametreTrajet2;
    Bruit b1;

    @Rule
    public ErrorCollector collector = new ErrorCollector();


    @Before
    public void setUp() throws Exception {
        i1 = new Information();
        LinkedList<Boolean> l2 = new LinkedList<>();
        l2.add(true);
        i2 = new Information(l2.toArray());

        LinkedList<Boolean> l3 = new LinkedList<>();
        l3.add(true);
        l3.add(false);
        l3.add(false);
        l3.add(false);
        l3.add(true);
        i3 = new Information(l3.toArray());

        i4 = new Information(l3.toArray());

        LinkedList<Boolean> l5 = new LinkedList<>();
        i5 = new Information(l5.toArray());
        i5.add(false);

        i6 = new Information(new Float[] {5f, 5f, -5f, -5f ,0f});
        i7 = new Information(new Float[]{1f, 2f, 3f});
        i8 = new Information(new Float[]{-2f, 2f, 4f});
        i9 = new Information(new Float[]{0f, 1f});
        parametreTrajet = new ParametreTrajet(2, 0.5f);
        parametreTrajet2 = new ParametreTrajet(0, 0.5f);


        b1 = new Bruit(i3.nbElements());
        b1.setIemeElement(0, 1f);
        b1.setIemeElement(1, -2.5f);
        b1.setIemeElement(2, -1f);
        b1.setIemeElement(3, 1.5f);
        b1.setIemeElement(4, 0f);

    }

    @Test
    public void nbElements() {
        collector.checkThat("Information vide", i1.nbElements(), is(0));
        collector.checkThat("Information true", i2.nbElements(), is(1));
        collector.checkThat("Information true, false, false,false, true", i3.nbElements(), is(5));
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void iemeElementKO() {
        i1.iemeElement(1);
    }

    @Test
    public void iemeElement() {
        collector.checkThat("Test ieme element", i2.iemeElement(0), is(true));
        collector.checkThat("Test ieme element", i3.iemeElement(3), is(false));
        collector.checkThat("Test ieme element", i3.iemeElement(4), is(true));
    }

    @Test
    public void setIemeElement() {
        collector.checkThat("Test setIemeElement", i2.iemeElement(0), is(true));
        i2.setIemeElement(0, false);
        collector.checkThat("Test setIemeElement", i2.iemeElement(0), is(false));
        collector.checkThat("Test setIemeElement", i3.iemeElement(4), is(true));
        i3.setIemeElement(4, false);
        collector.checkThat("Test setIemeElement", i3.iemeElement(4), is(false));
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void setIemeElementKO() {
        i2.setIemeElement(5, false);
    }

    @Test
    public void add() {
        i1.add(false);
        collector.checkThat("Test add", i1.nbElements(), is(1));
        collector.checkThat("Test add", i1.iemeElement(0), is(false));
        i3.add(false);
        collector.checkThat("Test add", i3.nbElements(), is(6));
        collector.checkThat("Test add", i3.iemeElement(5), is(false));
    }

    @Test
    public void testEquals() {
        collector.checkThat("Test equals", i2.equals(i5), is(false));
        collector.checkThat("Test equals", i3.equals(i4), is(true));
        collector.checkThat("Test equals", i3.equals(i5), is(false));
        collector.checkThat("Test equals", i3.equals(i2), is(false));
        collector.checkThat("Test equals", i3.equals(new LinkedList<Boolean>()), is(false));
    }

    @Test
    public void testToString() {
        collector.checkThat("Test to String", i3.toString(), containsString("true"));
        collector.checkThat("Test to String", i3.toString(), containsString("false"));
        collector.checkThat("Test to String", i2.toString(), containsString("true"));
    }

    @Test
    public void ajouterBruit() {
        collector.checkThat("Test ajouterBruit", i6.ajouterBruit(b1), is(new Information<>(new Float[] {6f, 2.5f, -6f, -3.5f ,0f})));
    }

    @Test (expected = UnsupportedOperationException.class)
    public void ajouterBruitKO1() {
        i3.ajouterBruit(new Bruit(i3.nbElements()));
    }

    @Test (expected = SizeInformationException.class)
    public void ajouterBruitKO2() {
        i6.ajouterBruit(new Bruit(4));
    }
    @Test
    public void somme() {
        Information<Float> I7 = new Information<>(i7);
        I7.somme(i8);
        collector.checkThat("Test somme même taille", I7, is(new Information<>(new Float[]{-1f, 4f, 7f})));
        Information<Float> copieI7 = new Information<>(i7);
        copieI7.somme(i9);
        collector.checkThat("Test somme taille différente", copieI7, is(new Information<>(new Float[]{1f, 3f, 3f})));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sommeTypeIncorrect() {
        i2.somme(i3);
    }

    @Test
    public void genererInformationRetardee(){
        Information<Float> infoRetardee = i7.genererInformationRetardee(parametreTrajet);
        collector.checkThat("Test retard et atténuation", infoRetardee, is(new Information<>(new Float[]{0f, 0f, 0.5f, 1f, 1.5f})));
        Information<Float> infoRetardeeTauZero = i7.genererInformationRetardee(parametreTrajet2);
        collector.checkThat("Test tau=0", infoRetardeeTauZero, is(new Information<>(new Float[]{0.5f, 1f, 1.5f})));

    }

    @Test(expected = UnsupportedOperationException.class)
    public void genererInformationRetardeeKO() {
        i3.genererInformationRetardee(new ParametreTrajet(2, 0.5f));
    }

    @Test(expected = IllegalArgumentException.class)
    public void genererInformationRetardeeParametreKO2() {
        i7.genererInformationRetardee(null);
    }

    @Test
    public void calculerPuissance(){
        double puissanceI7 = i7.calculerPuissance();
        assertEquals("Test calcul puissance i7", (1f + 4f + 9f) / 3f, puissanceI7, 0.0001);
        double puissanceI8 = i8.calculerPuissance();
        assertEquals("Test calcul puissance i8", (4f + 4f + 16f) / 3f, puissanceI8, 0.0001);
        double puissanceVide = i1.calculerPuissance();
        collector.checkThat("Test calcul puissance information vide", puissanceVide, is(0.0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void calculerPuissanceKO() {
        i3.calculerPuissance();
    }

}