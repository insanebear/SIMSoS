package simsos.scenario.mci;

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

    private double conformRate = 0.8; // indicates how much CS will follow policies

    public MCIScenario() throws IOException {
        Random rd = new Random();
        this.world = new MCIWorld(5000, 300, 200, 10);

        /*
        * SoS Construction
        * SoS Manager: 1, Fire Department: 3, PTS Center: 3, Hospital: 3
        * CSs follow policies based on a given probability
        * */

        // SoS Manager
        SoSManager manager = new SoSManager(this.world, "SoSManager");
        this.world.addAgent(manager);

        // Fire Department
        for(int i=0; i<3; i++){
            FireDepartment fd = new FireDepartment(this.world, i, "Fire Department", conformRate);
            manager.setFireDepartments(fd);
            for(int j=0; j<fd.getAllocFighters(); j++){
                FireFighter ff = new FireFighter(null, j, "Fire Fighter", "FD"+i, conformRate);
                this.world.addAgent(ff);
                fd.setWorkFighterList(j, ff);
            }
        }

        // PTS Center
        for(int i=0; i<3; i++){
            PTSCenter pts = new PTSCenter(this.world, i, "PTS Center", conformRate);
            manager.setPtsCenters(pts);
            for(int j=0; j<pts.getAllocGndAmbul(); j++){
                GndAmbulance gndAmbul = new GndAmbulance(null, j, "Gnd Ambulance", "PTS"+i , conformRate);
                this.world.addAgent(gndAmbul);
                pts.setWorkGndAmbuls(j, gndAmbul);
            }
        }

        for(int i=0; i<3; i++){
            int generalRoom = ThreadLocalRandom.current().nextInt(90, 100);
            int intensiveRoom = ThreadLocalRandom.current().nextInt(30, 100);
            int operatingRoom = ThreadLocalRandom.current().nextInt(60, 80);
            int locX = ThreadLocalRandom.current().nextInt(4, hospitalMapSize.getRight());
            int locY = rd.nextInt(hospitalMapSize.getRight());

            Hospital hospital = new Hospital(this.world, i, "Hospital", generalRoom,
                    intensiveRoom, operatingRoom, new Location(locX, locY), conformRate);
            this.world.addAgent(hospital); // hospital acts by itself
            manager.setHospitals(hospital);
        }

        this.checker = null;
//        this.checker = new GoalChecker();
    }

    public double getConformRate() {
        return conformRate;
    }

    public void setConformRate(double conformRate) {
        this.conformRate = conformRate;
    }
}
