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
    private double rescueCompliance;
    private double transportCompliance;
    private double treatmentCompliance;
    private boolean enforced;

    private int totalFD;
    private int totalPTS;
    private int totalH;

    private int meanCrew;
    private int varCrew;
    private int meanGeneral;
    private int varGeneral;
    private int meanIntensive;
    private int varIntensive;
    private int meanOperating;
    private int varOperating;

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
            FireDepartment fd = new FireDepartment(this.world, i, "Fire Department", getRescueCompliance(), this.isEnforced());
            manager.setFireDepartments(fd);

            for(int j=0; j<fd.getAllocFighters(); j++){
                FireFighter ff = new FireFighter(null, j, "Fire Fighter", "FD"+i, getRescueCompliance(), this.isEnforced());
                this.world.addAgent(ff);
                fd.setWorkFighterList(j, ff);
            }
        }

        // PTS Center
        for(int i=0; i<this.totalPTS; i++){
            PTSCenter pts = new PTSCenter(this.world, i, "PTS Center", getTransportCompliance(), this.isEnforced());
            manager.setPtsCenters(pts);
            for(int j=0; j<pts.getAllocGndAmbul(); j++){
                GndAmbulance gndAmbul = new GndAmbulance(null, j, "Gnd Ambulance", "PTS"+i , getTransportCompliance(), this.isEnforced());
                this.world.addAgent(gndAmbul);
                pts.setWorkGndAmbuls(j, gndAmbul);
            }
        }

        for(int i=0; i<this.totalH; i++){
            int generalRoom = getRandomValue(varGeneral, meanGeneral);
            int intensiveRoom = getRandomValue(varIntensive, meanIntensive);
            int operatingRoom = getRandomValue(varOperating, meanOperating);
            int medicalCrew = getRandomValue(varCrew, meanCrew);

            int locX = ThreadLocalRandom.current().nextInt(4, hospitalMapSize.getRight());
            int locY = rd.nextInt(hospitalMapSize.getRight());

            Hospital hospital = new Hospital(this.world, i, "Hospital", generalRoom,
                    intensiveRoom, operatingRoom, new Location(locX, locY), medicalCrew, getTreatmentCompliance(), this.isEnforced());

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
//        this.checker = new GoalChecker();
    }

    public void setInfrastructure() {
        this.rescueCompliance = infrastructure.getRescueCompliance();
        this.transportCompliance = infrastructure.getTransportCompliance();
        this.treatmentCompliance = infrastructure.getTreatmentCompliance();
        this.enforced = infrastructure.isEnforced();

        this.totalFD = infrastructure.getNumFireDepartment();
        this.totalPTS = infrastructure.getNumPTSCenter();
        this.totalH = infrastructure.getNumHospital();

        this.meanCrew = infrastructure.getMeanCrew();
        this.varCrew = infrastructure.getVarCrew();
        this.meanGeneral = infrastructure.getMeanGeneral();
        this.varGeneral = infrastructure.getVarGeneral();
        this.meanIntensive = infrastructure.getMeanIntensive();
        this.varIntensive = infrastructure.getVarIntensive();
        this.meanOperating = infrastructure.getMeanOperating();
        this.varOperating = infrastructure.getVarOperating();
    }

    public boolean isEnforced() {
        return enforced;
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

    public double getRescueCompliance() {
        return rescueCompliance;
    }

    public double getTransportCompliance() {
        return transportCompliance;
    }

    public double getTreatmentCompliance() {
        return treatmentCompliance;
    }
}
