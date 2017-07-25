package simsos.scenario.mci.cs;

import simsos.scenario.mci.Policy;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.*;

/**
 * Created by mgjin on 2017-06-29.
 */
public class SoSManager extends Agent {
    private String name;
    private int id;

    ArrayList<FireDepartment> fireDepartments;
    ArrayList<PTSCenter> ptsCenters;
    ArrayList<Hospital> hospitals;

    public SoSManager(World world, String name) {
        super(world);

        this.name = name;

        fireDepartments = new ArrayList<>();
        ptsCenters = new ArrayList<>();
        hospitals = new ArrayList<>();

        this.reset();
    }

    @Override
    public Action step() {

        return Action.getNullAction(1, this.getName() + ": null action");
    }

    @Override
    public void reset() {
//        this.rescueRequestQueue = new HashMap<String, RescueProcess>();
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
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

    public void setFireDepartments(FireDepartment fd){
        fireDepartments.add(fd);
    }

    public void setPtsCenters(PTSCenter pts){
        ptsCenters.add(pts);
    }

    public void setHospitals(Hospital hospital){
        hospitals.add(hospital);
    }
}
