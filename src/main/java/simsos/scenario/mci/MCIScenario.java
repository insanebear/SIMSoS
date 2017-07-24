package simsos.scenario.mci;

import simsos.scenario.mci.cs.*;
import simsos.simulation.component.Scenario;

import java.util.ArrayList;

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
        this.world = new MCIWorld(100, 300, 200, 19);

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

        this.world.addAgent(new SoSManager(this.world, "SoSManager"));

        for(int i=0; i<3; i++){
            FireDepartment fd = new FireDepartment(this.world, i, "Fire Department", fdPolicies);

            for(int j=0; j<fd.getAllocFighters(); j++){
                FireFighter ff = new FireFighter(null, j, "Fire Fighter", "FD"+i ,2);
                this.world.addAgent(ff);
                fd.setWorkFighterList(j, ff);
            }
        }

        for(int i=0; i<3; i++){
//            this.world.addAgent(new PTSCenter(this.world, i, "PTSCenter", ptsPolicies));
        }

        for(int i=0; i<3; i++){
//            this.world.addAgent(new Hospital(this.world, i,"Hospital", hPolicies));
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
