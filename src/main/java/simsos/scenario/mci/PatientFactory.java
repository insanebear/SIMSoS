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

    private int totalCasualty;
    private Patient.InjuryType[] injuryList;

    public PatientFactory(int totalCasualty) {
        this.totalCasualty = totalCasualty;
        this.injuryList = Patient.InjuryType.values();
    }

//    public void generatePatient(ArrayList<Integer>[][] patientMap, ArrayList<Patient> patientsList, ArrayList<Floor> building){
    public void generatePatient(ArrayList<Patient> patientsList, ArrayList<Floor> building, int radius){
        Random rd = new Random();

        for(int i=0; i<totalCasualty; i++){
            Patient.InjuryType injuryType = injuryList[rd.nextInt(injuryList.length)];
            int strength = setStrengthByType(injuryType);

            int story = rd.nextInt(building.size());

            int x = (int)Math.round(rd.nextGaussian()*1.5 + radius/2);
            int y = (int)Math.round(rd.nextGaussian()*1.5 + radius/2);
            Location location = new Location(x, y);

            Patient p = new Patient(i, strength, injuryType, story, location);
            patientsList.add(p);
            building.get(story).setPatientOnFloor(i, x, y);
        }

//        // CHECK patient information
//        for(Patient p : patientsList){
//            System.out.println("Patient "+p.getPatientId()+" is at ("
//                    +p.getLocation().getX()+", "+p.getLocation().getY()+")");
//            System.out.println("Injury type: "+p.getInjuryType());
//            System.out.println("Strength: "+p.getStrength());
//            System.out.println("Severity: "+p.getSeverity());
//            System.out.println();
//        }
    }
    public int setStrengthByType(Patient.InjuryType injuryType){
        // total strength: 200 (0~199)
        // Fractured: relatively slight injury
        // Bleeding, Burn: burn is much more serious than bleeding in terms of mean strength

        Random rd = new Random();
        int strength=0;

        switch (injuryType){
            case FRACTURED:
                strength = (int)Math.round(rd.nextGaussian()*18 + 120);
                break;
            case BLEEDING:
                strength = (int)Math.round(rd.nextGaussian()*20 + 100);
                break;
            case BURN:
                strength = (int)Math.round(rd.nextGaussian()*15 + 60);
        }
        return strength;
    }

}
