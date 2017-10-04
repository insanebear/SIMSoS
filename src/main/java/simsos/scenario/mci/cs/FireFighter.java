package simsos.scenario.mci.cs;

import org.apache.commons.lang3.ArrayUtils;
import simsos.scenario.mci.*;
import simsos.scenario.mci.policy.*;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.*;

import static simsos.scenario.mci.Environment.*;
import static simsos.scenario.mci.cs.SoSManager.numWaitPTS;

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
    private ArrayList<String> selectMethodList;
    private ArrayList<String> stageMethodList;

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

        selectMethodList = new ArrayList<>();
        stageMethodList = new ArrayList<>();
        makeActionMethodList();

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
                                //NOTE RESUE _select policy
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

                            //Note RESCUE_ stage policy
                            currPolicy = checkPolicy(currAction.toString());
                            int stageSpot = stagePatient(currPolicy);

                            stageZone[stageSpot].add(rescuedPatientId);
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
        return new Random().nextFloat() < conformRate;
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

    private void makeActionMethodList(){
        selectMethodList.add("Random");
        selectMethodList.add("Distance");
        selectMethodList.add("Severity");
        selectMethodList.add("InjuryType");

        stageMethodList.add("Random");
        stageMethodList.add("MeanRandom");
        stageMethodList.add("MCSlot");
    }

    /**--------Destination--------**/

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

    /**--------Action--------**/
    /*----Select Patient (Select)----*/
    private int selectPatient(ArrayList<Integer> patientList, Policy policy){
        //NOTE 한 위치에서 여러가지 방법으로(알맞는 방법으로 구해야할 환자의 index 를 골라서 return
        //TODO select action by probability(uncertainty)

        ArrayList<Integer> candPatients = (ArrayList<Integer>) patientList.clone();
        Random rd = new Random();
        boolean decision = makeDecision();
        int resIdx = -1;

        if(policy != null && decision){
            ArrayList<String> policyActionMethod = policy.getAction().getActionMethod();
            if(policyActionMethod.size()>0) {
                for (String s : policyActionMethod) {
                    switch (s) {
                        case "Random":
                            candPatients = randomSelect(candPatients);
                            break;
                        case "Distance":
                            candPatients = distanceSelect(candPatients);
                            break;
                        case "Severity":
                            candPatients = severitySelect(candPatients);
                            break;
                        case "InjuryType":
                            candPatients = injuryTypeSelect(candPatients);
                            break;
                    }
                    if (candPatients.size() == 1)
                        break;
                }
                if (candPatients.size() != 1)
                    resIdx = candPatients.get(rd.nextInt(candPatients.size()));
                else
                    resIdx = candPatients.get(0);
            }
        } else {
            if(!decision)
                System.out.println("Decide not to follow the policy.");
            else
                System.out.println("Not fitted condition.");
            System.out.println("Do the randomly select method.");

            Collections.shuffle(selectMethodList);
            for(String s: selectMethodList){
                switch (s){
                    case "Random":
                        candPatients = randomSelect(candPatients);
                        break;
                    case "Distance":
                        candPatients = distanceSelect(candPatients);
                        break;
                    case "Severity":
                        candPatients = severitySelect(candPatients);
                        break;
                    case "InjuryType":
                        candPatients = injuryTypeSelect(candPatients);
                        break;
                }
                if(candPatients.size() == 1)
                    break;
            }
            if(candPatients.size()!=1)
                resIdx = candPatients.get(rd.nextInt(candPatients.size()));
            else
                resIdx = candPatients.get(0);
        }
        removePatient(resIdx, patientList);
        return resIdx;
    }

    private ArrayList<Integer>  randomSelect(ArrayList<Integer> candPatients){
        System.out.println("Random policy applied.");
        ArrayList<Integer> tempList = new ArrayList<>();
        tempList.add(new Random().nextInt(candPatients.size()));
        return tempList;
    }

    private ArrayList<Integer>  distanceSelect(ArrayList<Integer> candPatients){
        System.out.println("Distance policy applied.");
        ArrayList<Integer> tempList = new ArrayList<>();
        tempList.add(candPatients.get(0));
        return tempList;
    }

    private ArrayList<Integer>  severitySelect(ArrayList<Integer> candPatients){
        System.out.println("Severity policy applied.");
        ArrayList<Integer> tempList = new ArrayList<>();

        Collections.sort(candPatients, new Comparator<Integer>() {
            @Override
            public int compare(Integer idx1, Integer idx2) { // sort descending order
                return patientsList.get(idx2).getSeverity()-patientsList.get(idx1).getSeverity();
            }
        });

        int stdSeverity = patientsList.get(candPatients.get(0)).getSeverity(); // highest severity

        for(Integer idx : candPatients)
            if(patientsList.get(idx).getSeverity() == stdSeverity)
                tempList.add(idx);

        return tempList;
    }

    private ArrayList<Integer>  injuryTypeSelect(ArrayList<Integer> candPatients){
        //NOTE InjuryType Sorting means sorting by strength decreasing rate
        System.out.println("InjurtyType first policy applied.");
        ArrayList<Integer> tempList = new ArrayList<>();
        int stdRate = 0;

        Collections.sort(candPatients, new Comparator<Integer>() {
            @Override
            public int compare(Integer idx1, Integer idx2) { // sort descending order
                return patientsList.get(idx2).strengthDecreasingRate() - patientsList.get(idx1).strengthDecreasingRate();
            }
        });

        stdRate = patientsList.get(candPatients.get(0)).strengthDecreasingRate(); // highest decreasing rate

        for(Integer idx : candPatients){
            if(patientsList.get(idx).strengthDecreasingRate() == stdRate)
                tempList.add(idx);
        }

        return tempList;
    }

    /*----Select Slot (Stage)----*/
    private int stagePatient(Policy policy){
        int resIdx=0;
        boolean decision = makeDecision();
        Random rd = new Random();
        String policyActionMethod;

        if(policy != null && decision){
            policyActionMethod = policy.getAction().getActionMethod().get(0);
        } else {
            if(!decision)
                System.out.println("Decide not to follow the policy.");
            else
                System.out.println("Not fitted condition.");
            System.out.println("Do the randomly select method.");

            Collections.shuffle(stageMethodList);
            policyActionMethod = stageMethodList.get(0);
        }

        if(policyActionMethod.equals("Random")){
            resIdx = rd.nextInt(stageZone.length);
        }else if(policyActionMethod.equals("MeanRandom")){
            int totalSlot = stageZone.length;
            int[] numWaitPatient = new int[totalSlot];
            int mean = calcMeanWaitPatient(numWaitPatient, totalSlot);

            if(mean!=0){
                ArrayList<Integer> candidIdx = new ArrayList<>();
                for(int i=0; i<totalSlot; i++)
                    if(numWaitPatient[i] <= mean)
                        candidIdx.add(i);
                resIdx = candidIdx.get(rd.nextInt(candidIdx.size()));
            }else
                resIdx = rd.nextInt(stageZone.length);
        }else if(policyActionMethod.equals("MCSlot")){ // most crowded slot
            int maximum = Collections.max(Arrays.asList(ArrayUtils.toObject(numWaitPTS)));
            ArrayList<Integer> maxPTSSlot = new ArrayList<>();
            for(int i=0; i<numWaitPTS.length; i++)
                if(numWaitPTS[i]==maximum)
                    maxPTSSlot.add(i);
            resIdx = maxPTSSlot.get(rd.nextInt(maxPTSSlot.size()));
        }
        return resIdx;
    }

    private int calcMeanWaitPatient(int[] numWaitPatient, int totalSlot){
        int sum=0;
        for(int i=0; i<totalSlot; i++)
            numWaitPatient[i] = stageZone[i].size();
        for(int number : numWaitPatient)
            sum += number;
        return Math.round(sum/totalSlot);
    }

    private void removePatient(int idx, ArrayList<Integer> patientList){
        for(int i=0; i<patientList.size(); i++)
            if(patientList.get(i) == idx)
                patientList.remove(i);
    }

    private void handleDeadPatient(){

    }

}
