package simsos.scenario.mci.cs;

import simsos.scenario.mci.Location;
import simsos.scenario.mci.Patient;
import simsos.scenario.mci.Policy;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static simsos.scenario.mci.Environment.patientMapSize;
import static simsos.scenario.mci.Environment.patientsList;
import static simsos.scenario.mci.Environment.stageZone;

/**
 *
 * Created by Youlim Jung on 22/07/2017.
 *
 */
public class GndAmbulance extends Agent{
    private enum Status {WAITING, LOADING, BACK_TO_SCENE, TRANSFERRING}

    private Patient.InjuryType patientType;

    private String affiliation;
    private String name;
    private Location location;
    private int gAmbId;
    private int loadPatientId;
    private ArrayList<Integer> spotPatientList;
    private Status status;
    private int moveLimit;

    public GndAmbulance(World world, int gAmbId, String name, String affiliation, int moveLimit) {
        super(world);
        Random rd = new Random();
        this.name = name;
        this.affiliation = affiliation;
        this.gAmbId = gAmbId;
        this.location = new Location(rd.nextInt(patientMapSize.getLeft()),0);
        this.loadPatientId = -1;
        this.status = Status.WAITING;

    }

    @Override
    public Action step() {
        switch(status){
            case WAITING:
                return new Action(0) {
                    @Override
                    public void execute() {
                        if(checkPatient(location.getX()) && loadPatientId == -1){
                            spotPatientList = stageZone[location.getX()];
                            status = Status.LOADING;
                            System.out.println(getAffiliation()+" "+getName()+" "+getId()+"is "+getStatus()+". Ready to loading");
                        }
                    }

                    @Override
                    public String getName() {
                        return "PTS Waiting";
                    }
                };
            case LOADING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        loadPatientId = spotPatientList.get(0);
                        spotPatientList.remove(0);
                        System.out.println("Patient "+loadPatientId+" is ready to be transferred.");
                        patientsList.get(loadPatientId).changeStat(); // TRANSFERRING

                        status = Status.TRANSFERRING;
                        System.out.println(getAffiliation()+" "+getName()+" "+getId()+"is "+getStatus()+". Start transferring.");
                    }

                    @Override
                    public String getName() {
                        return "PTS Loading";
                    }
                };
            case TRANSFERRING:
                return new Action(1) {
                    @Override
                    public void execute() {

                    }

                    @Override
                    public String getName() {
                        return "PTS Transferring";
                    }
                };
            case BACK_TO_SCENE:
                return new Action(1) {
                    @Override
                    public void execute() {

                    }

                    @Override
                    public String getName() {
                        return null;
                    }
                };
        }
        return Action.getNullAction(0, this.getName()+": do nothing.");
    }

    @Override
    public void reset() {

    }

    @Override
    public int getId() {
        return this.gAmbId;
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

    public boolean checkPatient(int idx){
        if(stageZone[idx].size() > 0)
            return true;
        else
            return false;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public int getgAmbId() {
        return gAmbId;
    }

    public Status getStatus() {
        return status;
    }
}
