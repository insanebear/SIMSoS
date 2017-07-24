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
//    private int bedCapacity = 0;
//
//    private static class RescueProcess {
//        private enum Stage {Listed, BedSecured, UtilityCollected, PTSSecured, Complete};
//
//        private String patient;
//        private Patient_old.Severity patientSeverity;
//        private Location patientLocation;
//        private Stage stage = Stage.Listed;
//
//        private String hospital;
//        private Location hospitalLocation;
//        private String pts;
//
//        private ArrayList<Pair<String, Integer>> utilities = new ArrayList<Pair<String, Integer>>();
//
//        public RescueProcess(String patient) {
//            this.patient = patient;
//        }
//    }
//
//    private HashMap<String, RescueProcess> rescueRequestQueue;

    public SoSManager(World world, String name) {
        super(world);

        this.name = name;
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
}
