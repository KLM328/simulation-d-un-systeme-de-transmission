package bruits;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;


public class BruitBlancGaussienTest {

    BruitBlancGaussien bruitBlancGaussien1;
    BruitBlancGaussien bruitBlancGaussien2;
    BruitBlancGaussien bruitBlancGaussien3;
    BruitBlancGaussien bruitBlancGaussien4;

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Before
    public void setUp() throws Exception {
        bruitBlancGaussien1 = new BruitBlancGaussien(100000, 4f, 4, 30);
        bruitBlancGaussien2 = new BruitBlancGaussien(1000);
        bruitBlancGaussien3 = new BruitBlancGaussien(100, 1f, 4, 50, 1234);
        bruitBlancGaussien4 = new BruitBlancGaussien(100, 1f, 4, 50, 1234);
    }

    @Test
    public void moyenneNulle() {
        collector.checkThat("Error moyenne", bruitBlancGaussien1.mean() < 0.01f, is(true));
    }

    @Test
    public void distributionGausienne() {
        Map<Double, Integer> distribution = bruitBlancGaussien1.distribution(0.001f);
        double moyenne = bruitBlancGaussien1.mean();
        double ecartType = bruitBlancGaussien1.ecartType();
        double[] proportionType = {0.68, 0.95, 0.997};

        for (int i : new Integer[] {1,2,3}){
            double sum = 0;
            for(double d : distribution.keySet()){
                if(Math.abs(d - moyenne) <= i * ecartType){
                    sum += distribution.get(d);
                }
            }
            double value = ((sum/bruitBlancGaussien1.nbElements()) - proportionType[i-1]);
            collector.checkThat("Error distributionGausienne", value < 0.01, is(true));
            collector.checkThat("Error distributionGausienne", bruitBlancGaussien2, is(new Bruit(1000)));
        }


    }

    @Test
    public void seed(){
        collector.checkThat("Seed fixé bruit different", bruitBlancGaussien4, is(bruitBlancGaussien3));
    }
}