package simsos.scenario.mci.cs;

import simsos.scenario.mci.*;
import simsos.scenario.mci.policy.*;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.*;

import static simsos.scenario.mci.Environment.*;

/**
 *
 * Created by Youlim Jung on 18/07/2017.
 *
 */
public class FireFighter extends Agent{
    private enum Status{
        SEARCHING, RESCUING, TRANSPORTING, DONE
    }
    private enum Directions{ //TODO Refactor this (not using moving direction)
        WEST, SOUTH, EAST, NORTH
    }
    public enum Actions{
        SEARCH, RESCUE, TRANSPORT, NONE
    }
    private int fighterId;
    private String affiliation;
    private String name;

    // current properties
    private Status status;
    private Location location;
    private int story;
    private Actions currAction;
    private Policy currPolicy;

    private int rescuedPatientId;
    private ArrayList<Integer> spotPatientList;

    private double conformRate; // indicates how much CS will follow policies

    //
    private Location destCoordinate;
    private int destStory;
    private int reachTime;

    public FireFighter(World world, int fighterId, String name, String affiliation, double conformRate) {
        super(world);
        this.name = name;
        this.affiliation = affiliation;
        this.fighterId = fighterId;
        this.location = new Location(0, 0);
        this.story = 0;
        this.rescuedPatientId = -1;     // no patient rescued
        this.status = Status.SEARCHING;
        this.currAction = Actions.SEARCH;
        this.conformRate = conformRate;

        this.destCoordinate = new Location(0, 0);
        setDestination();
        this.reachTime = setReachTime();

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
                            currAction = Actions.RESCUE;
                            location.setX(destCoordinate.getX());
                            location.setY(destCoordinate.getY());
                            story = destStory;
                        } else
                            reachTime--;
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
                        spotPatientList = building.get(story).getSpotPatientList(location.getX(), location.getY());
                        if (checkPatient(spotPatientList)) {
                            if(spotPatientList.size()>1){
                                //NOTE Policy applied if there are 2 or more patients
                                currPolicy = checkPolicy(currAction.toString());
                                rescuedPatientId = selectPatient(spotPatientList, currPolicy);
//                                rescuedPatientId = selectPatient(spotPatientList, null);
                            }else{
                                rescuedPatientId = spotPatientList.get(0);
                                spotPatientList.remove(0);
                            }

                            patientsList.get(rescuedPatientId).changeStat(); // patient RESCUED
                            status = Status.TRANSPORTING;
                            currAction = Actions.TRANSPORT;

                            //TODO location scheduling?
                            setDestination();
                            reachTime = setReachTime();

                            System.out.println("Patient \'" + rescuedPatientId + "\' rescued. Ready to transport.");
                        } else {
                            setDestination();
                            reachTime = setReachTime();
                            System.out.println("Patient not found");
                            if(destCoordinate ==null){
                                status = Status.DONE;
                                currAction = Actions.NONE;
                            }
                            else {
                                status = Status.SEARCHING;
                                currAction = Actions.SEARCH;
                            }
                        }
                    }
                    @Override
                    public String getName() {
                        return "Trying to rescue";
                    }
                };

            case TRANSPORTING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if (reachTime == 0) {
                            location.setX(destCoordinate.getX());
                            location.setY(destCoordinate.getY());
                            story = destStory;

                            stageZone[location.getX()].add(rescuedPatientId);
                            Environment.patientsList.get(rescuedPatientId).changeStat(); // TRANSPORT_WAIT
                            Environment.updateCasualty();

                            System.out.println(getAffiliation() + " " + getName() + " " + getId() + " is at " + location.getX() + ", " + location.getY());
                            System.out.println("Patient \'" + rescuedPatientId + "\' is staged on "
                                    + location.getX() + "th area. Patient is " + Environment.patientsList.get(rescuedPatientId).getStatus());

                            // initialize status
                            rescuedPatientId = -1;
                            setDestination();
                            reachTime = setReachTime();

                            if(destCoordinate ==null){
                                status = Status.DONE;
                                currAction = Actions.NONE;
                            }
                            else {
                                status = Status.SEARCHING;
                                currAction = Actions.SEARCH;
                            }
                        } else {
                            reachTime--;
                        }
                    }

                    @Override
                    public String getName() {
                        return "Transporting";
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
    public boolean makeDecision() {
        return false;
    }

    @Override
    public HashMap<String, Object> getProperties() {
        return new HashMap<String, Object>();
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

    public void setDestination(){
        Random rd = new Random();
        int idx = rd.nextInt(patientsList.size());
        int endCond = 0;

        if(rescuedPatientId == -1){
            while(endCond <= patientsList.size()){
                Patient p = patientsList.get(idx);
                if (p.getStatus() == Patient.Status.RESCUE_WAIT){
                    Location pLocation = p.getLocation();
                    destCoordinate.setX(pLocation.getX());
                    destCoordinate.setY(pLocation.getY());
                    destStory = p.getStory();
                }
                idx = rd.nextInt(patientsList.size());
                endCond++;
            }
        }else{
            destStory = 0;
            destCoordinate = new Location(rd.nextInt(patientMapSize.getLeft()),0);
        }

    }

    public int setReachTime(){
        // distance-relative time
        int weight;
        int diffLocation = location.distanceTo(destCoordinate); // always > 0, min:0, max:2*radius
        int diffStory = destStory - story; // diff>0: upward, diff<0: downward, diff=0: same floor

        // weight by distance
        weight = calDistWeight(diffLocation);

        // weight by height
        if(diffStory > 0)
            weight += 2;

        // weight by rescued patient
        if(rescuedPatientId > 0)
            weight += 1;

        return calReachTime(weight);
    }

    public int calDistWeight(int distance){
        int radius = patientMapSize.getLeft();

        if(distance == 0)
            return 0;
        else if(distance>0 && distance<(radius/4))
            return 2;
        else if(distance>=(radius/4) && distance<(radius/2))
            return 4;
        else
            return 6;
    }

    public int calReachTime(int weight){
        Random rd = new Random();

        int mean = 2 + weight;
        double stdDev = 2;
        int result = 0;
        boolean isValid = false;

        while(!isValid){
            result = (int)Math.round(rd.nextGaussian() * stdDev + mean);
            if(result>0)
                isValid = true;
        }
        return result;
    }


    private int selectPatient(ArrayList<Integer> patientList, Policy policy){
        //NOTE 한 위치에서 여러가지 방법으로(알맞는 방법으로 구해야할 환자의 index 를 골라서 return
        //TODO select action by probability(uncertainty)
        int resIdx;
        ArrayList<Integer> candPatients = (ArrayList<Integer>) patientList.clone();
        Random rd = new Random();
        int prob;

        //CHECK
//        for(Integer i : candPatients)
//            System.out.println("index: "+i+" Severity: "+patientsList.get(i).getSeverity()+
//                    " InjuryType: "+patientsList.get(i).getInjuryType());

        if(policy != null){
            String criteria = policy.getAction().getValue();
            if(criteria.equals("Severity")){
                System.out.println("Severity first policy applied.");
                candPatients = sortBySeverity(candPatients); // 이 action은 할 수도 있고 안 할 수도 있게 해야할듯.
//                //CHECK
//                System.out.println("----------------------------------------------");
//                for(Integer i : candPatients)
//                    System.out.println("index: "+i+" Severity: "+patientsList.get(i).getSeverity()+
//                            " InjuryType: "+patientsList.get(i).getInjuryType());
//                System.out.println("----------------------------------------------");
                while(candPatients.size()>1){
                    prob = rd.nextInt(10);
                    if(prob>=0 && prob<6){
                        System.out.println("Rest of the action is processed by InjuryType first.");
                        candPatients = sortByInjuryType(candPatients);
                    }

                    int temp = candPatients.get(rd.nextInt(candPatients.size()));
                    System.out.println("Rest of the action is processed by random");
                    candPatients.clear();
                    candPatients.add(temp);
                }
            }else if(criteria.equals("InjuryType")){
                System.out.println("InjurtyType first policy applied.");
                candPatients = sortByInjuryType(candPatients);
                while(candPatients.size()>1){
                    prob = rd.nextInt(10);
                    if(prob>=0 && prob<6){
                        System.out.println("Rest of the action is processed by Severity first.");
                        candPatients = sortBySeverity(candPatients);
                    }else if(prob<=6 && prob<10) {
                        int temp = candPatients.get(rd.nextInt(candPatients.size()));
                        System.out.println("Rest of the action is processed by random");
                        candPatients.clear();
                        candPatients.add(temp);
                    }
                }
            }
            resIdx = candPatients.get(0);
//            //CHECK
//            System.out.println("Selected patient index: "+resIdx+" Severity: "+patientsList.get(resIdx).getSeverity()+
//                    " InjuryType: "+patientsList.get(resIdx).getInjuryType());
//            removePatient(resIdx, patientList);
        } else {
            // TODO randomly select action? or set an action as default?
            System.out.println("Policy condition does not fit on current situation. Take first patient.");
            resIdx = patientList.get(0);
            patientList.remove(0);
        }

        return resIdx;
    }

    private ArrayList<Integer> sortBySeverity(ArrayList<Integer> patientList){
        ArrayList<Integer> tempList = new ArrayList<>();
        int stdSeverity = 0;

        Collections.sort(patientList, new Comparator<Integer>() {
            @Override
            public int compare(Integer idx1, Integer idx2) { // sort descending order
                return patientsList.get(idx2).getSeverity()-patientsList.get(idx1).getSeverity();
            }
        });

        stdSeverity = patientsList.get(patientList.get(0)).getSeverity(); // highest severity

        for(Integer idx : patientList)
            if(patientsList.get(idx).getSeverity() == stdSeverity)
                tempList.add(idx);

        return tempList;
    }

    private ArrayList<Integer> sortByInjuryType(ArrayList<Integer> patientList){
        //NOTE InjuryType Sorting means sorting by strength decreasing rate
        ArrayList<Integer> tempList = new ArrayList<>();
        int stdRate = 0;

        Collections.sort(patientList, new Comparator<Integer>() {
            @Override
            public int compare(Integer idx1, Integer idx2) { // sort descending order
                return patientsList.get(idx2).strengthDecreasingRate() - patientsList.get(idx1).strengthDecreasingRate();
            }
        });

        stdRate = patientsList.get(patientList.get(0)).strengthDecreasingRate();

        for(Integer idx : patientList){
            if(patientsList.get(idx).strengthDecreasingRate() == stdRate)
                tempList.add(idx);
        }
        return tempList;
    }


//    private int checkPolicy(String actName){
//        int idx = 0;
//
//        for(Policy p : Environment.rescuePolicies){
//            if(this.name.equals(p.getActor()) && p.isActive() && actName.equals(p.getAction().getName())){
//                return idx;
//            }
//            idx++;
//        }
//        return -1;
//    }

    // 이거 왜 만든거지?
    private void removePatient(int idx, ArrayList<Integer> patientList){
        for(int i=0; i<patientList.size(); i++)
            if(patientList.get(i) == idx)
                patientList.remove(i);
    }

    private void handleDeadPatient(){

    }

}
