package simsos.scenario.mci;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static simsos.scenario.mci.Environment.patientMapSize;

/**
 *
 * Created by Youlim Jung on 17/07/2017.
 *
 */
public class PatientFactory {

    private int totalCasualty;
    private Patient.InjuryType[] injuryList;

    public PatientFactory(int totalCasualty) {
        this.totalCasualty = totalCasualty;
        this.injuryList = Patient.InjuryType.values();
    }

    public void generatePatient(ArrayList<Patient> patientsList, ArrayList<Floor> building, int radius){
        Random rd = new Random();

        for(int i=0; i<totalCasualty; i++){
            Patient.InjuryType injuryType = injuryList[rd.nextInt(injuryList.length)];
            int strength = 399;
            while(!checkValidStrength(strength)){
                strength = setStrengthByType(injuryType);
            }
            int story = rd.nextInt(building.size());
            int x = -1;
            int y = -1;

            while(!checkValidLocation(x, y)){
                x = (int)Math.round(rd.nextGaussian()*1.5 + radius/2);
                y = (int)Math.round(rd.nextGaussian()*1.5 + radius/2);
            }

            Location location = new Location(x, y);

            // Patient Instantiation
            Patient p = new Patient(i, strength, injuryType, story, location);
            patientsList.add(p);
            building.get(story).setPatientOnFloor(i, x, y);
        }
    }
    public int setStrengthByType(Patient.InjuryType injuryType){
        // total strength: 300 (0~299)
        // Fractured: relatively slight injury
        // Bleeding, Burn: burn is much more serious than bleeding in terms of mean strength

        Random rd = new Random();
        int strength=0;

        switch (injuryType){
            case FRACTURED:
                strength = (int)Math.round(rd.nextGaussian()*18 + 220);
                break;
            case BLEEDING:
                strength = (int)Math.round(rd.nextGaussian()*20 + 200);
                break;
            case BURN:
                strength = (int)Math.round(rd.nextGaussian()*15 + 160);
        }
        return strength;
    }

    private boolean checkValidLocation(int x, int y){
        if(y>=0 && y<patientMapSize.getRight() && x>=0 && x<patientMapSize.getLeft())
            return true;
        else
            return false;
    }

    private boolean checkValidStrength(int strength){
        if(strength<160 && strength>=0)
            return true;
        else
            return false;
    }

}
