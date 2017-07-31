package simsos.scenario.mci.cs;

import simsos.scenario.mci.*;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static simsos.scenario.mci.Environment.*;

/**
 *
 * Created by Youlim Jung on 18/07/2017.
 *
 */
public class FireFighter extends Agent{
    private enum Status{
        SEARCHING, RESCUING, TRANSFERRING, DONE
    }
    private enum Directions{
        WEST, SOUTH, EAST, NORTH
    }
    private Patient.InjuryType patientType;

    private String affiliation;
    private String name;
    private int fighterId;
    private Location location;
    private int rescuedPatientId;
    private ArrayList<Integer> spotPatientList;
    private Status status;
    private int moveLimit;

    //
    private Location destination;
    private int reachTime;


    public FireFighter(World world, int fighterId, String name, String affiliation, int moveLimit) {
        super(world);
        this.name = name;
        this.affiliation = affiliation;
        this.fighterId = fighterId;
        this.location = new Location(0, 0);
        this.rescuedPatientId = -1;     // no patient rescued
        this.status = Status.SEARCHING;
        this.moveLimit = moveLimit;
        //
        this.destination = setDestination();
        this.reachTime = setReachTime(); // distribution 으로 랜덤설정할수도있겠구나
    }

    @Override
    public Action step() {
        switch (status) {
            case SEARCHING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if (reachTime == 0) {
                            status = Status.RESCUING;
                        } else {
                            reachTime--;
                        }
                    }
                    @Override
                    public String getName() {
                        return "Searching";
                    }
                };

            case RESCUING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        spotPatientList = patientMap[destination.getX()][destination.getY()];
                        if (checkPatient(spotPatientList)) {
                            rescuedPatientId = spotPatientList.get(0); //TODO rescue priority
                            spotPatientList.remove(0);

                            patientsList.get(rescuedPatientId).changeStat(); // RESCUED
                            status = Status.TRANSFERRING;

                            //TODO location scheduling?
                            destination = new Location(new Random().nextInt(patientMapSize.getLeft()), patientMapSize.getRight());
                            reachTime = setReachTime();

                            System.out.println("Patient \'" + rescuedPatientId + "\' rescued. Ready to transfer.");
                        } else {
                            destination = setDestination();
                            reachTime = setReachTime();
                            System.out.println("Patient not found");
                            if(destination==null)
                                status = Status.DONE;
                            else
                                status = Status.SEARCHING;
                        }
                    }
                    @Override
                    public String getName() {
                        return "Trying to rescue";
                    }
                };

            case TRANSFERRING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if (reachTime == 0) {
                            stageZone[destination.getX()].add(rescuedPatientId);
                            Environment.patientsList.get(rescuedPatientId).changeStat(); // TRANSFER_WAIT

                            System.out.println(getAffiliation() + " " + getName() + " " + getId() + " is at " + location.getX() + ", " + location.getY());
                            System.out.println("Patient \'" + rescuedPatientId + "\' is staged on "
                                    + location.getX() + "th area. Patient is " + Environment.patientsList.get(rescuedPatientId).getStatus());

                            rescuedPatientId = -1;  // initialize rescuePatient
                            destination = setDestination();
                            reachTime = setReachTime();

                            if(destination==null)
                                status = Status.DONE;
                            else
                                status = Status.SEARCHING;
                        } else {
                            reachTime--;
                        }
                    }

                    @Override
                    public String getName() {
                        return "Transferring";
                    }
                };
            case DONE:
                String s = this.getAffiliation()+" "+this.getName()+" "+this.getId()+" finished its work.";
                return Action.getNullAction(1, s);
        }
        return Action.getNullAction(1, this.getName() + ": ??");
    }

    @Override
    public void reset() {

    }

    @Override
    public int getId() {
        return this.fighterId;
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

    public String getAffiliation() {
        return affiliation;
    }


    public boolean checkPatient(ArrayList<Integer> spotPatientList){
        if(!spotPatientList.isEmpty())
            return true;
        else
            return false;
    }

    public Location setDestination(){ //TODO revise
        Random rd = new Random();
        int idx = rd.nextInt(patientsList.size());
        int endCond = 0;


        while(endCond <= patientsList.size()){
            Patient p = patientsList.get(idx);
            if (p.getStatus()== Patient.Status.RESCUE_WAIT){
                return p.getLocation();
            }
            idx = rd.nextInt(patientsList.size());
            endCond++;
        }
        return null;
    }

    public int setReachTime(){
        //TODO review
        // This can be revised to get a certain distribution from outside if policy is applied.
        return ThreadLocalRandom.current().nextInt(3, 10);
    }

}
