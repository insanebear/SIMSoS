package simsos.scenario.mci;

import simsos.scenario.SoSInfrastructure;
import simsos.scenario.mci.cs.*;
import simsos.simulation.component.Scenario;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static simsos.scenario.mci.Environment.hospitalMapSize;

/**
 * Created by mgjin on 2017-06-28.
 *
 * Edited by Youlim Jung on 2017-07-18.
 */
public class MCIScenario extends Scenario {

    // indicates how much CS will follow policies
    private double mResCompliance;
    private double mTransCompliance;
    private double mTreatCompliance;
    private double enforceRate;

    private int totalFD;
    private int totalPTS;
    private int totalH;

    private int crew;
    private int general;
    private int intensive;
    private int operating;

    private SoSInfrastructure infrastructure;

    public MCIScenario(SoSInfrastructure infrastructure) throws IOException {
        Random rd = new Random();
        this.world = new MCIWorld();
        this.infrastructure = infrastructure;
        setInfrastructure();

        // SoS Manager
        SoSManager manager = new SoSManager(this.world, "SoSManager");
        this.world.addAgent(manager);

        // Fire Department
        for(int i=0; i<this.totalFD; i++){
            FireDepartment fd = new FireDepartment(this.world, i, "Fire Department", this.mResCompliance, isEnforced(getEnforceRate()));
            manager.setFireDepartments(fd);

            for(int j=0; j<fd.getAllocFighters(); j++){
                FireFighter ff = new FireFighter(null, j, "Fire Fighter", "FD"+i, getRandomCompliance(mResCompliance), isEnforced(getEnforceRate()));
                this.world.addAgent(ff);
                fd.setWorkFighterList(j, ff);
            }
        }

        // PTS Center
        for(int i=0; i<this.totalPTS; i++){
            PTSCenter pts = new PTSCenter(this.world, i, "PTS Center", this.mTransCompliance, isEnforced(getEnforceRate()));
            manager.setPtsCenters(pts);
            for(int j=0; j<pts.getAllocGndAmbul(); j++){
                GndAmbulance gndAmbul = new GndAmbulance(null, j, "Gnd Ambulance", "PTS"+i , getRandomCompliance(mTransCompliance), isEnforced(getEnforceRate()));
                this.world.addAgent(gndAmbul);
                pts.setWorkGndAmbuls(j, gndAmbul);
            }
        }

        // Hospital
        for(int i=0; i<this.totalH; i++){
            int generalRoom = getGeneral();
            int intensiveRoom = getIntensive();
            int operatingRoom = getOperating();
            int medicalCrew = getCrew();

            int locX = ThreadLocalRandom.current().nextInt(4, hospitalMapSize.getRight());
            int locY = rd.nextInt(hospitalMapSize.getRight());

            Hospital hospital = new Hospital(this.world, i, "Hospital", generalRoom,
                    intensiveRoom, operatingRoom, new Location(locX, locY), medicalCrew, getRandomCompliance(mTreatCompliance), isEnforced(getEnforceRate()));

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

        this.checker = null;
    }

    public void setInfrastructure() {
        this.mResCompliance = infrastructure.getRescueCompliance();
        this.mTransCompliance = infrastructure.getTransportCompliance();
        this.mTreatCompliance = infrastructure.getTreatmentCompliance();
        this.enforceRate = infrastructure.getEnforceRate();

        this.totalFD = infrastructure.getNumFireDepartment();
        this.totalPTS = infrastructure.getNumPTSCenter();
        this.totalH = infrastructure.getNumHospital();

        this.crew = infrastructure.getCrew();
        this.general = infrastructure.getGeneral();
        this.intensive = infrastructure.getIntensive();
        this.operating = infrastructure.getOperating();
    }

    public double getEnforceRate() {
        return enforceRate;
    }

    private int getRandomValue(int variance, int mean){
        Random rd = new Random();
        int min = (int)Math.round(mean*0.3);
        int max = (int)Math.round(mean*0.7);
        int result = (int)Math.round(rd.nextGaussian()* variance + mean);
        while(result < min || result > max)
            result = (int)Math.round(rd.nextGaussian()* variance + mean);
        return result;
    }

    public double getRandomCompliance(double mCompliance) {
        Random rd = new Random();
        double min = mCompliance;
        double max = 0.9;
        double result;

        do{
            result = Math.round(rd.nextDouble()*10d)/10d;
        }while(result < min || result > max);

        return result;
    }

    private boolean isEnforced(double enforceRate){
        Random random = new Random();
        Double rate = (double) random.nextFloat();
        return  rate < enforceRate;
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
}
