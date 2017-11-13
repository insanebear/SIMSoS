package simsos.scenario.mci;

import simsos.scenario.SoSInfrastructure;
import simsos.scenario.mci.cs.*;
import simsos.simulation.component.Scenario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by mgjin on 2017-06-28.
 *
 * Edited by Youlim Jung on 2017-07-18.
 */
public class MCIScenario extends Scenario {

    // indicates how much CS will follow policies
    private String typeSoS;
    private double minCompliance;
    private double maxCompliance;
    private double mResCompliance;
    private double mTransCompliance;
    private double mTreatCompliance;
    private double enforceRate;

    private int totFireFighters;
    private int totAmbulance;
    private int totalH;

    private int crew;
    private int general;
    private int intensive;
    private int operating;
    private ArrayList<Location> hospitalLocations;

    private SoSInfrastructure infrastructure;

    public MCIScenario(SoSInfrastructure infrastructure) throws IOException {
//        Random rd = new Random();
        this.world = new MCIWorld();
        this.infrastructure = infrastructure;
        setInfrastructure();

        // SoS Manager
        SoSManager manager = new SoSManager(this.world, "SoSManager");
        this.world.addAgent(manager);

        // Fire Fighter
        for(int i = 0; i<this.totFireFighters; i++){
            FireFighter ff = new FireFighter(null, i, "Fire Fighter", "FD", makeCSCompliance(mResCompliance), isEnforced(getEnforceRate()));
            this.world.addAgent(ff);
        }

        // PTS Ambulance
        for(int i = 0; i<this.totAmbulance; i++){
            GndAmbulance gndAmbul = new GndAmbulance(null, i, "Gnd Ambulance", "PTS", makeCSCompliance(mTransCompliance), isEnforced(getEnforceRate()));
            this.world.addAgent(gndAmbul);
        }

        // Hospital
        for(int i=0; i<this.totalH; i++){
            int generalRoom = getGeneral();
            int intensiveRoom = getIntensive();
            int operatingRoom = getOperating();
            int medicalCrew = getCrew();

            Hospital hospital = new Hospital(this.world, i, "Hospital", generalRoom,
                    intensiveRoom, operatingRoom, getHospitalLocations().get(i), medicalCrew, makeCSCompliance(mTreatCompliance), isEnforced(getEnforceRate()));

            // information setting for reset
            Information info = new Information();
            info.setName(hospital.getName());
            info.setId(hospital.getId());
            info.setLocation(hospital.getLocation());
            info.setProperties("GR", generalRoom);
            info.setProperties("IR", intensiveRoom);
            info.setProperties("OR", operatingRoom);

            infrastructure.putCsInformation(info);

            this.world.addAgent(hospital); // hospital acts by itself
            manager.setHospitals(hospital);
        }
//        this.checker = null;
    }

    public void setInfrastructure() {
        this.typeSoS = infrastructure.getTypeSoS();
        this.minCompliance = infrastructure.getMinCompliance();
        this.maxCompliance = infrastructure.getMaxCompliance();
        this.mResCompliance = infrastructure.getRescueCompliance();
        this.mTransCompliance = infrastructure.getTransportCompliance();
        this.mTreatCompliance = infrastructure.getTreatmentCompliance();
        this.enforceRate = infrastructure.getEnforceRate();

        this.totFireFighters = infrastructure.getTotFireFighters();
        this.totAmbulance = infrastructure.getNumPTSCenter();
        this.totalH = infrastructure.getTotHospital();

        this.crew = infrastructure.getCrew();
        this.general = infrastructure.getGeneral();
        this.intensive = infrastructure.getIntensive();
        this.operating = infrastructure.getOperating();
        this.hospitalLocations = infrastructure.getHospitalLocations();
    }

    public double getEnforceRate() {
        return enforceRate;
    }

    public double makeCSCompliance(double mCompliance) {
        Random rd = new Random();
        double min = getMinCompliance();
        double max = getMaxCompliance();
        double avg = mCompliance;
        double result;

        switch (getTypeSoS()){
            case "D":   // directed
                return 1;
            case "D+A": // directed + acknowledge
                max = 1;
                break;
        }
        do{
            Double randomNum = rd.nextGaussian()*0.5+avg;
            result = (double)Math.round(randomNum*10)/10;
        }while(result < min || result >= max);

        return result;
    }

    private boolean isEnforced(double enforceRate){
        Random random = new Random();
        if(typeSoS.equals("D"))
            return true;
        else if(typeSoS.equals("D+A")){
            Double rate = (double) random.nextFloat();  // not include 1.0
            return  rate < enforceRate;
        }
        return false;
    }

    public int getCrew() {
        return crew;
    }

    public int getGeneral() {
        return general;
    }

    public int getIntensive() {
        return intensive;
    }

    public int getOperating() {
        return operating;
    }

    public String getTypeSoS() {
        return typeSoS;
    }

    public double getMinCompliance() {
        return minCompliance;
    }

    public double getMaxCompliance() {
        return maxCompliance;
    }

    public ArrayList<Location> getHospitalLocations() {
        return hospitalLocations;
    }
}
