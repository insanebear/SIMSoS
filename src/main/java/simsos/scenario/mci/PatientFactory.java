package simsos.scenario.mci;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * Created by Youlim Jung on 17/07/2017.
 *
 */
public class PatientFactory {
    private ArrayList<Patient> patientsList;

    private ArrayList<Integer> [][] patientMap;
    private int totalCasualty;

    private Patient.InjuryType[] injuryList;

    public PatientFactory(int totalCasualty, int radius) {
        this.patientsList = new ArrayList<>();
        this.totalCasualty = totalCasualty;
        this.injuryList = Patient.InjuryType.values();

        patientMap = new ArrayList[radius+1][radius+1];
        initalizeMap();
    }

    public ArrayList<Integer>[][] generatePatient(int radius, ArrayList<Patient> patientsList){
        Random random = new Random();

        for(int i=0; i<totalCasualty; i++){
            int strength = random.nextInt(170)+20;
            Patient.InjuryType injuryType = injuryList[random.nextInt(injuryList.length)];

            int x = ThreadLocalRandom.current().nextInt(5, radius);
            int y = ThreadLocalRandom.current().nextInt(5, radius);
//          int x = random.nextInt(radius);
//          int y = random.nextInt(radius);
            Location location = new Location(x, y);

            Patient p = new Patient(i, strength, injuryType, location);
            patientsList.add(p);

            patientMap[x][y].add(i);

        }

        // patient generation test
        for(Patient p : patientsList){
            System.out.println("patient name: "+p.getPatientId());
            System.out.println("location: "+p.getLocation().getX()+", "+p.getLocation().getY());
        }
        return patientMap;
    }

    public void initalizeMap(){
        for(int i=0; i<patientMap.length; i++)
            for(int j=0 ; j<patientMap.length; j++)
                patientMap[i][j] = new ArrayList<>();
    }
}