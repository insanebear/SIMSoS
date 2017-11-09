package simsos.scenario.mci.cs;

import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.*;

import static simsos.scenario.mci.Environment.stageZone;

/**
 * Created by mgjin on 2017-06-29.
 */
public class SoSManager extends Agent {
    private String name;
    private int id;

    public static ArrayList<Hospital> hospitals;
    public static int[] numWaitPTS = new int[stageZone.length];

    public SoSManager(World world, String name) {
        super(world);

        this.name = name;

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
        numWaitPTS = new int[stageZone.length];
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
    public boolean makeDecision() {
        return false;
    }

    @Override
    public HashMap<String, Object> getProperties() {
        return new HashMap<String, Object>();
    }

    public void setHospitals(Hospital hospital){
        hospitals.add(hospital);
    }
}