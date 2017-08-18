package simsos.scenario.mci.cs;

import simsos.scenario.mci.*;
import simsos.scenario.mci.policy.*;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
    private Patient.InjuryType patientType;

    private String affiliation;
    private String name;
    private int fighterId;
    private Location location;
    private int rescuedPatientId;
    private ArrayList<Integer> spotPatientList;
    private Status status;

    private Actions currAction;
    private Policy currPolicy;

    private double conformRate; // indicates how much CS will follow policies

    //
    private Location destination;
    private int reachTime;

    public FireFighter(World world, int fighterId, String name, String affiliation, double conformRate) {
        super(world);
        this.name = name;
        this.affiliation = affiliation;
        this.fighterId = fighterId;
        this.location = new Location(0, 0);
        this.rescuedPatientId = -1;     // no patient rescued
        this.status = Status.SEARCHING;
        this.currAction = Actions.SEARCH;
        this.conformRate = conformRate;
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
                            currAction = Actions.RESCUE;
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
                        spotPatientList = patientMap[destination.getX()][destination.getY()];
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
                            destination = new Location(new Random().nextInt(patientMapSize.getLeft()), patientMapSize.getRight());
                            reachTime = setReachTime();

                            System.out.println("Patient \'" + rescuedPatientId + "\' rescued. Ready to transport.");
                        } else {
                            destination = setDestination();
                            reachTime = setReachTime();
                            System.out.println("Patient not found");
                            if(destination==null){
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
                            stageZone[destination.getX()].add(rescuedPatientId);
                            Environment.patientsList.get(rescuedPatientId).changeStat(); // TRANSPORT_WAIT
                            Environment.updateCasualty();

                            System.out.println(getAffiliation() + " " + getName() + " " + getId() + " is at " + location.getX() + ", " + location.getY());
                            System.out.println("Patient \'" + rescuedPatientId + "\' is staged on "
                                    + location.getX() + "th area. Patient is " + Environment.patientsList.get(rescuedPatientId).getStatus());

                            rescuedPatientId = -1;  // initialize rescuePatient
                            destination = setDestination();
                            reachTime = setReachTime();

                            if(destination==null){
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

    private void removePatient(int idx, ArrayList<Integer> patientList){
        for(int i=0; i<patientList.size(); i++)
            if(patientList.get(i) == idx)
                patientList.remove(i);
    }
}
