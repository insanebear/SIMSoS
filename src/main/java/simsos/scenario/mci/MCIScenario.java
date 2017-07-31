package simsos.scenario.mci;

import simsos.scenario.mci.cs.*;
import simsos.simulation.component.Scenario;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static simsos.scenario.mci.Environment.hospitalMapSize;

/**
 * Created by mgjin on 2017-06-28.
 *
 * Edited by Youlim Jung on 2017-07-18.
 */
public class MCIScenario extends Scenario {
    private ArrayList<Policy> wholePolicies;
    private ArrayList<Policy> fdPolicies;
    private ArrayList<Policy> ptsPolicies;
    private ArrayList<Policy> hPolicies;

    private FireDepartment[] fireDepartments;
    private PTSCenter[] ptsCenters;
    private Hospital[] hospitals;


    public MCIScenario(ArrayList<Policy> mciPolicies) {
        Random rd = new Random();
        this.world = new MCIWorld(100, 300, 200, 10);

        // categorize policies
        this.wholePolicies = mciPolicies;
        this.fdPolicies = new ArrayList<>();
        this.ptsPolicies = new ArrayList<>();
        this.hPolicies = new ArrayList<>();
        categorizePolicy();

        /*
        * SoS Construction
        * SoS Manager: 1, Fire Department: 3, PTS Center: 3, Hospital: 3
        * */

        fireDepartments = new FireDepartment[3];
        ptsCenters = new PTSCenter[3];
        hospitals = new Hospital[3];

        // SoS Manager
        SoSManager manager = new SoSManager(this.world, "SoSManager");
        this.world.addAgent(manager);

        // Fire Department
        for(int i=0; i<3; i++){
            FireDepartment fd = new FireDepartment(this.world, i, "Fire Department", fdPolicies);
            manager.setFireDepartments(fd);
            for(int j=0; j<fd.getAllocFighters(); j++){
                FireFighter ff = new FireFighter(null, j, "Fire Fighter", "FD"+i ,2);
                this.world.addAgent(ff);
                fd.setWorkFighterList(j, ff);
            }
        }

        // PTS Center
        for(int i=0; i<3; i++){
            PTSCenter pts = new PTSCenter(this.world, i, "PTS Center", ptsPolicies);
            manager.setPtsCenters(pts);
            for(int j=0; j<pts.getAllocGndAmbul(); j++){
                GndAmbulance gndAmbul = new GndAmbulance(null, j, "Gnd Ambulance", "PTS"+i ,2);
                this.world.addAgent(gndAmbul);
                pts.setWorkGndAmbuls(j, gndAmbul);
            }
        }

        for(int i=0; i<3; i++){
            int generalRoom = ThreadLocalRandom.current().nextInt(7, 15);
            int intensiveRoom = ThreadLocalRandom.current().nextInt(3, 8);
            int operatingRoom = ThreadLocalRandom.current().nextInt(8, 20);
            int locX = ThreadLocalRandom.current().nextInt(4, hospitalMapSize.getRight());
            int locY = rd.nextInt(hospitalMapSize.getRight());

            Hospital hospital = new Hospital(this.world, i, "Hospital", hPolicies,
                    generalRoom, intensiveRoom, operatingRoom, new Location(locX, locY));
            this.world.addAgent(hospital); // hospital acts by itself
            manager.setHospitals(hospital);
        }

        this.checker = null;
    }
    private void categorizePolicy(){
        for(Policy policy: wholePolicies){
            if(policy.getPolicyTag().equals("fd")) {
                fdPolicies.add(policy);
            } else if(policy.getPolicyTag().equals("pts")){
                ptsPolicies.add(policy);
            } else if (policy.getPolicyTag().equals("h")){
                hPolicies.add(policy);
            }
        }
    }
}
