package simsos.scenario.mci.cs;

import simsos.scenario.mci.Location;
import simsos.scenario.mci.Policy;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by mgjin on 2017-06-28.
 */
public class Hospital extends Agent {
    private String name;
    private int hospitalId;
    private Location location;

    private int capacity;
//    private ArrayList<Patient_old> inpatients;

    private Action treatment;

    public Hospital(World world, int hospitalId, String name, ArrayList<Policy> mciPolicies) {
        super(world);
        this.name = name;
        this.hospitalId = hospitalId;
        this.reset();
    }

    @Override
    public Action step() {
//        if (this.inpatients.size() > 0)
//            return this.treatment;
//        else
//            return Action.getNullAction(1, this.getId() + ": No treatment");
        return Action.getNullAction(1, this.getName() + ": No treatment");
    }

    @Override
    public void reset() {
//        Random rd = new Random();
//
//        this.location = new Location(MCIWorld.MAP_SIZE.getLeft() / 2, MCIWorld.MAP_SIZE.getRight() / 2);
//
//        this.capacity = 30 + (rd.nextInt(20) - 10); // 30 +- 10
//        this.inpatients = new ArrayList<Patient_old>();
//
//        this.treatment = new Action(1) {
//
//            @Override
//            public void execute() {
//                for (Patient_old patientOld : inpatients) {
//                    //give a treatment message to patientOld
//                }
//            }
//
//            @Override
//            public String getId() {
//                return Hospital.this.getId() + ": Treatment";
//            }
//        };
    }

    @Override
    public int getId() {
        return this.hospitalId;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void messageIn(Message msg) {

    }

    @Override
    public HashMap<String, Object> getProperties() {
        return new HashMap<String, Object>();
    }

    @Override
    public void injectPolicies(ArrayList<Policy> policies) {

    }
}
