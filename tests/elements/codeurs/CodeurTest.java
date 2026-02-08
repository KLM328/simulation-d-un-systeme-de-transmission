package elements.codeurs;

import information.Information;
import information.InformationNonConformeException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import simulateur.Mode;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.*;


public class CodeurTest {
    private Information<Boolean> informationVide;
    private Information<Boolean> informationRecu;
    private Information<Float> informationEmiseNRZT;
    private Information<Float> informationEmiseNRZ;
    private Information<Float> informationEmiseRZ;
    private Information<Boolean> informationAttenduCodage;
    private Information<Boolean> informationAttenduCodage2;
    private Information<Boolean> informationCodage;

    @Rule
    public ErrorCollector collector = new ErrorCollector();
    private Codeur codeurRZ;
    private Codeur codeurNRZT;
    private Codeur codeurNRZ;
    private Codeur codeur;

    @Before
    public void setUp() throws InformationNonConformeException {
        informationRecu = new Information<>();
        informationVide = new Information<>();
        informationEmiseNRZT = new Information<>();
        informationEmiseRZ = new Information<>();
        informationEmiseNRZ = new Information<>();
        informationAttenduCodage = new Information<>();
        informationAttenduCodage2 = new Information<>();
        informationCodage = new Information<>();
        codeurRZ = new Codeur(Mode.RZ, 30, new Float[] {-5f, 5f},false);
        codeurNRZ = new Codeur(Mode.NRZ, 30, new Float[] {-5f, 5f},false);
        codeurNRZT = new Codeur(Mode.NRZT, 30, new Float[] {-5f, 5f},false);
        codeur = new Codeur(Mode.NRZ,30, new Float[] {-5f, 5f},true);

        //information avec codage canal
        informationCodage.add(true);
        informationCodage.add(false);
        informationCodage.add(true);
        informationAttenduCodage.add(true);
        informationAttenduCodage.add(false);
        informationAttenduCodage.add(true);
        informationAttenduCodage.add(false);
        informationAttenduCodage.add(true);
        informationAttenduCodage.add(false);
        informationAttenduCodage.add(true);
        informationAttenduCodage.add(false);
        informationAttenduCodage.add(true);

        informationAttenduCodage2.add(true);
        informationAttenduCodage2.add(false);
        informationAttenduCodage2.add(true);
        informationAttenduCodage2.add(false);
        informationAttenduCodage2.add(true);


        //information sans codage canal
        informationRecu.add(true);
        informationRecu.add(false);
        informationRecu.add(true);
        informationRecu.add(false);
        informationRecu.add(true);
        informationRecu.add(true);
        informationRecu.add(false);
        informationRecu.add(false);
        informationRecu.add(true);
        informationRecu.add(true);
        informationRecu.add(true);
        informationRecu.add(true);
        informationEmiseNRZT = convertir(informationRecu, Mode.NRZT, -5, 5, 30);
        informationEmiseNRZ = convertir(informationRecu, Mode.NRZ, -5, 5, 30);
        informationEmiseRZ = convertir(informationRecu, Mode.RZ, 0, 5, 30);
    }

    private Information<Float> convertir(Information<Boolean> information, Mode mode, float min, float max, int nbEch) {
        Information<Float> informationConvertie = new Information<>();
        Boolean suivant;
        Boolean precedent;
        Boolean valeur;
        for(int i=0; i<information.nbElements(); i++) {
            valeur = information.iemeElement(i);
            if (i == 0){
                precedent = !valeur;
            }
            else {
                precedent = information.iemeElement(i-1);
            }
            if (i == information.nbElements()-1){
                suivant = !valeur;
            }else {
                suivant = information.iemeElement(i+1);
            }
            if(mode == Mode.RZ){
                informationConvertie.add(genererSymboleRZ(valeur, max, nbEch));
            }
            else if(mode == Mode.NRZ){
                informationConvertie.add(genererSymboleNRZ(valeur, min, max, nbEch));
            }
            else if(mode == Mode.NRZT){
                informationConvertie.add(genererSymboleNRZT(valeur, precedent, suivant, min, max, nbEch ));
            }
        }
        return informationConvertie;
    }

    private Float[] genererSymboleNRZ(Boolean valeur, float min, float max, int nbEch) {
        Float[] symbole = new Float[nbEch];
        for (int i = 0; i < nbEch; i++) {
            if (valeur) {
                symbole[i] = max;
            }
            else {
                symbole[i] = min;
            }
        }
        return symbole;
    }

    private Float[] genererSymboleRZ(Boolean valeur, float max, int nbEch) {
        Float[] symbole = new Float[nbEch];
        for (int i = 0; i < 3*nbEch; i+=3) {
            if (valeur && i >= nbEch && i < 2*nbEch) {
                symbole[i/3] = max;
            }
            else {
                symbole[i/3] = 0f;
            }
        }
        return symbole;
    }

    private Float[] genererSymboleNRZT(Boolean valeur, Boolean precedent, Boolean suivant, float min, float max, int nbEch) {
        Float[] symbole = new Float[nbEch];
        Map<Boolean, Float> convertion = new HashMap<>();
        convertion.put(false, min);
        convertion.put(true, max);
        for (int i = 0; i < nbEch/3; i++) {
            float x = (((float) i /(nbEch /3f))/2f) + 0.5f;
            symbole[i] = convertion.get(precedent) - (convertion.get(precedent) - convertion.get(valeur))*x;
        }
        for (int i = nbEch/3; i < 2*nbEch/3; i++) {
            symbole[i] = convertion.get(valeur);
        }
        for (int i = 2*nbEch/3; i < nbEch; i++) {
            float x = ((i - 2f*nbEch/3f) / (nbEch /3f))/2f;
            symbole[i] = convertion.get(valeur) - (convertion.get(valeur) - convertion.get(suivant))*x;
        }
        return symbole;
    }

    @Test
    public void recevoir() throws InformationNonConformeException {
        collector.checkThat("Error recevoir()", codeurNRZ.recevoir(informationRecu), is(informationRecu));
        collector.checkThat("Error recevoir()", codeurRZ.recevoir(informationRecu), is(informationRecu));
        collector.checkThat("Error recevoir()", codeurNRZT.recevoir(informationRecu), is(informationRecu));
        collector.checkThat("Error modification informationEmise", codeurNRZ.getInformationRecue(), is(informationRecu));
    }

    @Test(expected = InformationNonConformeException.class)
    public void recevoirKO1() throws InformationNonConformeException {
        codeurNRZ.recevoir(informationVide);
    }

    @Test(expected = InformationNonConformeException.class)
    public void recevoirKO3() throws InformationNonConformeException {
        codeurNRZ.recevoir(null);
    }

    @Test
    public void transformer() throws InformationNonConformeException {
        codeurNRZT.recevoir(informationRecu);
        codeurRZ.recevoir(informationRecu);
        codeurNRZ.recevoir(informationRecu);
        collector.checkThat("Error transformer()", codeurNRZ.transformer(), is(informationEmiseNRZ));
        collector.checkThat("Error transformer()", codeurRZ.transformer(), is(informationEmiseRZ));
        collector.checkThat("Error transformer()", codeurNRZT.transformer(), is(informationEmiseNRZT));
    }

    @Test
    public void emettre() throws InformationNonConformeException {
        codeurRZ.recevoir(informationRecu);
        collector.checkThat("Error emettre()", codeurRZ.emettre(), is(informationEmiseRZ));
    }

    @Test
    public void codageCanal() throws InformationNonConformeException {
        codeur.recevoir(informationCodage);
        collector.checkThat("Erreur codageCanal()", codeur.codageCanal(), is(informationAttenduCodage));
    }

    @Test(expected = InformationNonConformeException.class)
    public void codageCanal2() throws InformationNonConformeException {
        collector.checkThat("Erreur codageCanal", codeur.codageCanal(), is(informationAttenduCodage2));
    }
}
