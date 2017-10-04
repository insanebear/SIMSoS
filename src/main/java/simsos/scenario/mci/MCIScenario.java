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

    private double conformRate; // indicates how much CS will follow policies
    private int totalFD;
    private int totalPTS;
    private int totalH;

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
            FireDepartment fd = new FireDepartment(this.world, i, "Fire Department", conformRate);
            manager.setFireDepartments(fd);

            for(int j=0; j<fd.getAllocFighters(); j++){
                FireFighter ff = new FireFighter(null, j, "Fire Fighter", "FD"+i, conformRate);
                this.world.addAgent(ff);
                fd.setWorkFighterList(j, ff);
            }
        }

        // PTS Center
        for(int i=0; i<this.totalPTS; i++){
            PTSCenter pts = new PTSCenter(this.world, i, "PTS Center", conformRate);
            manager.setPtsCenters(pts);
            for(int j=0; j<pts.getAllocGndAmbul(); j++){
                GndAmbulance gndAmbul = new GndAmbulance(null, j, "Gnd Ambulance", "PTS"+i , conformRate);
                this.world.addAgent(gndAmbul);
                pts.setWorkGndAmbuls(j, gndAmbul);
            }
        }

        for(int i=0; i<this.totalH; i++){
            int generalRoom = (int)Math.round(rd.nextGaussian()* varGeneral + meanGeneral);
            int intensiveRoom = (int)Math.round(rd.nextGaussian()* varIntensive + meanIntensive);
            int operatingRoom = (int)Math.round(rd.nextGaussian()* varOperating + meanOperating);

            int locX = ThreadLocalRandom.current().nextInt(4, hospitalMapSize.getRight());
            int locY = rd.nextInt(hospitalMapSize.getRight());

            Hospital hospital = new Hospital(this.world, i, "Hospital", generalRoom,
                    intensiveRoom, operatingRoom, new Location(locX, locY), conformRate);

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
        conformRate = infrastructure.getConformRate();

        totalFD = infrastructure.getNumFireDepartment();
        totalPTS = infrastructure.getNumPTSCenter();
        totalH = infrastructure.getNumHospital();

        meanGeneral = infrastructure.getMeanGeneral();
        varGeneral = infrastructure.getVarGeneral();
        meanIntensive = infrastructure.getMeanIntensive();
        varIntensive = infrastructure.getVarIntensive();
        meanOperating = infrastructure.getMeanOperating();
        varOperating = infrastructure.getVarOperating();
    }

    public double getConformRate() {
        return conformRate;
    }

    public void setConformRate(double conformRate) {
        this.conformRate = conformRate;
    }
}
