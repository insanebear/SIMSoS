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

    public PatientFactory(int totalCasualty) {
        this.patientsList = new ArrayList<>();
        this.totalCasualty = totalCasualty;
        this.injuryList = Patient.InjuryType.values();


    }

    public void generatePatient(ArrayList<Integer>[][] patientMap, ArrayList<Patient> patientsList){
        Random random = new Random();
        int radius = patientMap.length;

        for(int i=0; i<totalCasualty; i++){
            int strength = random.nextInt(90)+40;
            Patient.InjuryType injuryType = injuryList[random.nextInt(injuryList.length)];

//            int x = ThreadLocalRandom.current().nextInt(5, radius);
//            int y = ThreadLocalRandom.current().nextInt(5, radius);
            int x = random.nextInt(radius);
            int y = random.nextInt(radius);
            Location location = new Location(x, y);

            Patient p = new Patient(i, strength, injuryType, location);
            patientsList.add(p);

            patientMap[x][y].add(i);

        }

        // patient information
        for(Patient p : patientsList){
            System.out.println("Patient "+p.getPatientId()+" is at ("
                    +p.getLocation().getX()+", "+p.getLocation().getY()+")");
            System.out.println("Injury type: "+p.getInjuryType());
            System.out.println("Strength: "+p.getStrength()+" Severity: "+p.getSeverity());
            System.out.println("Status: "+ p.getStatus());
            System.out.println();
        }
    }

}
