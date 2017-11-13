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
        INACTIVE, SEARCHING, RESCUING, TRANSPORTING, DONE
    }
    public enum Actions{
        SEARCH, RESCUE, TRANSPORT, NONE
    }
    private int fighterId;
    private String affiliation;
    private String name;
    private String role;
    private boolean active;
    private String csType;

    // current properties
    private Status status;
    private Location location;
    private int story;
    private Actions currAction;
    private Policy currPolicy;

    private int rescuedPatientId;
    private ArrayList<Integer> spotPatientList;

    private double compliance; // indicates how much CS will follow policies
    private double selectCompliance;
    private double stageCompliance;
    private boolean enforced; // directed SoS or acknowledged SoS
    private ArrayList<String> selectMethodList;
    private ArrayList<String> stageMethodList;

    //
    private Location destCoordinate;
    private int destStory;
    private int reachTime;

    private CallBack callBack = new CallBack() {
        @Override
        public boolean checkCSStat(String condString, int condValue) {
            return condString.equals("Story") && story==condValue;
        }
    };

    public FireFighter(World world, int fighterId, String name, String affiliation, double compliance, boolean enforced) {
        super(world);
        this.name = name;
        this.affiliation = affiliation;
        this.role = "RESCUE";
        this.fighterId = fighterId;
        this.enforced = enforced;
        this.compliance = compliance;
        this.selectCompliance = randomCompliance();
        this.stageCompliance = randomCompliance();

        if(this.enforced)
            this.csType = "D";
        else
            this.csType = "A";
        selectMethodList = new ArrayList<>();
        stageMethodList = new ArrayList<>();
        makeActionMethodList();

        this.reset();
    }

    @Override
    public Action step() {
        switch (status) {
            case INACTIVE:
                return new Action(1){
                    @Override
                    public void execute() {
                        if(checkActive()){
                            status = Status.SEARCHING;
                            currAction = Actions.SEARCH;
                        }
                    }

                    @Override
                    public String getName() {
                        return "FF inactive";
                    }
                };

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
                        // exclude dead patient (ignore dead patients)
                        excludeDeadPatient(spotPatientList);
                        if (checkPatientExistence(spotPatientList)) {
                            if(spotPatientList.size()>1){
                                //NOTE RESCUE_ select Policy
                                currPolicy = checkActionPolicy(role, csType, "Select", callBack);
                                rescuedPatientId = selectPatient(spotPatientList, currPolicy);
                                removePatient(rescuedPatientId, spotPatientList);
                            }else{
                                // rescue patient but do not need to check policy (no selectable situation)
                                rescuedPatientId = spotPatientList.get(0);
                                spotPatientList.remove(0);
                            }
                            Patient patient = patientsList.get(rescuedPatientId);
                            patient.changeStat(); // to RESCUED
                            status = Status.TRANSPORTING;
                            currAction = Actions.TRANSPORT;

                            setDestination();
                            reachTime = setReachTime();
                        } else {
                            // Patients do not exist. Search again.
                            setDestination();
                            if(destCoordinate ==null){
                                status = Status.DONE;
                                currAction = Actions.NONE;
                            }
                            else {
                                reachTime = setReachTime();
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
                            // Update final (arrived currently) location.
                            location.setX(destCoordinate.getX());
                            location.setY(destCoordinate.getY());
                            story = destStory;
                            // Check patient status
                            Patient patient = patientsList.get(rescuedPatientId);
                            if(!patient.isDead()){
                                //Note RESCUE_ stage policy
                                currPolicy = checkActionPolicy(role, csType,"Stage", callBack);
                                int stageSpot = stagePatient(currPolicy);

                                // Stage a patient at a spot.
                                stageZone[stageSpot].add(rescuedPatientId);
                                patientsList.get(rescuedPatientId).changeStat(); // to TRANSPORT_WAIT
                                updateCasualty();
                            }
                            // initialize information
                            rescuedPatientId = -1;
                            if(checkActive()){
                                setDestination();
                                if(destCoordinate == null){
                                    status = Status.DONE;
                                    currAction = Actions.NONE;
                                }
                                else {
                                    reachTime = setReachTime();
                                    status = Status.SEARCHING;
                                    currAction = Actions.SEARCH;
                                }
                            } else {
                                location = new Location(0, 0);
                                status = Status.INACTIVE;
                                currAction = Actions.NONE;
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
                return new Action(1) {
                    @Override
                    public void execute() {
                        String s = getAffiliation()+" "+this.getName()+" "+getId()+" finished its job.";
                    }

                    @Override
                    public String getName() {
                        return "FF finished job.";
                    }
                };
        }
        return Action.getNullAction(1, this.getName() + ": ??");
    }

    @Override
    public void reset() {
        if(checkActive()){
            this.status = Status.SEARCHING;
            this.currAction = Actions.SEARCH;
        }else{
            this.status = Status.INACTIVE;
            this.currAction = Actions.NONE;
        }
        this.currPolicy = null;
        this.story = 0;
        this.location = new Location(0, 0);
        this.rescuedPatientId = -1;
        this.destCoordinate = new Location(0, 0);
        setDestination();
        this.reachTime = setReachTime();
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
        return enforced || new Random().nextFloat() < compliance;
    }

    @Override
    public HashMap<String, Object> getProperties() {
        return new HashMap<String, Object>();
    }

    public String getAffiliation() {
        return affiliation;
    }

    public boolean checkPatientExistence(ArrayList<Integer> spotPatientList){
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
        if(rescuedPatientId == -1) { // 이제부터 환자 구해야 함
            ArrayList<Integer> candidList = new ArrayList<>();
            for(Patient patient : patientsList){
                if(patient.getStatus() == Patient.Status.RESCUE_WAIT)
                    candidList.add(patient.getPatientId());
            }
            if(candidList.size()>0){
                Collections.shuffle(candidList);
                Patient patient = patientsList.get(candidList.get(0));
                Location pLocation = patient.getLocation();
                destStory = patient.getStory();
                destCoordinate = new Location(pLocation.getX(), pLocation.getY());
            }else
                destCoordinate = null;
        } else {
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
            weight += 1;

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
            return 1;
        else if(distance>=(radius/4) && distance<(radius/2))
            return 2;
        else
            return 3;
    }

    public int calReachTime(int weight){
        Random rd = new Random();

        int mean = 1 + weight;
        double stdDev = 1;
        int result = 0;
        boolean isValid = false;

        while(!isValid){
            result = (int)Math.round(rd.nextGaussian() * stdDev + mean);
            if(result>0)
                isValid = true;
        }
        return result;
    }

    /**--------ActiveCheck--------**/
    private boolean checkActive(){
        ArrayList<Policy> compliancePolicies = checkCompliancePolicy(role);
        this.active = true;
        if(compliancePolicies.size() != 0){ // policy exists
            for(Policy policy : compliancePolicies){
                String actionName = policy.getAction().getActionName();
                switch (actionName){
                    case "Select":
                        if(selectCompliance < policy.getMinCompliance())
                            this.active = false;
                        break;
                    case "Stage":
                        if(stageCompliance < policy.getMinCompliance())
                            this.active = false;
                        break;
                }
                if (!this.active)
                    break;
            }
        }
        return this.active;
    }

    /**--------Action--------**/
    /*----Select Patient (Select)----*/
    @SuppressWarnings("unchecked")
    private int selectPatient(ArrayList<Integer> patientList, Policy policy){
        ArrayList<Integer> candPatients = (ArrayList<Integer>) patientList.clone();
        Random rd = new Random();
        boolean decision = makeDecision();
        int resIdx = -1;

        if(policy != null && decision){
            String actionMethod = policy.getAction().getActionMethod();
            switch (actionMethod) {
//                case "Random":
//                    candPatients = randomSelect(candPatients);
//                    break;
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
                if (candPatients.size() != 1)
                    resIdx = candPatients.get(rd.nextInt(candPatients.size()));
                else
                    resIdx = candPatients.get(0);
        } else {
            candPatients = randomSelect(candPatients);
//            Collections.shuffle(selectMethodList);
//            for(String s: selectMethodList){
//                switch (s){
//                    case "Random":
//                        candPatients = randomSelect(candPatients);
//                        break;
//                    case "Distance":
//                        candPatients = distanceSelect(candPatients);
//                        break;
//                    case "Severity":
//                        candPatients = severitySelect(candPatients);
//                        break;
//                    case "InjuryType":
//                        candPatients = injuryTypeSelect(candPatients);
//                        break;
//                }
//                if(candPatients.size() == 1){
//                    break;
//                }
//            }
//            if(candPatients.size()!=1)
//                resIdx = candPatients.get(rd.nextInt(candPatients.size()));
//            else
//                resIdx = candPatients.get(0);
            resIdx = candPatients.get(0);
        }
        return resIdx;
    }

    private ArrayList<Integer>  randomSelect(ArrayList<Integer> candPatients){
        ArrayList<Integer> tempList = new ArrayList<>();
        int candidIdx = new Random().nextInt(candPatients.size());
        tempList.add(candPatients.get(candidIdx));
        return tempList;
    }

    private ArrayList<Integer>  distanceSelect(ArrayList<Integer> candPatients){
        ArrayList<Integer> tempList = new ArrayList<>();
        tempList.add(candPatients.get(0));
        return tempList;
    }

    private ArrayList<Integer>  severitySelect(ArrayList<Integer> candPatients){
        ArrayList<Integer> tempList = new ArrayList<>();
        Collections.sort(candPatients, new Comparator<Integer>() {
            @Override
            public int compare(Integer idx1, Integer idx2) { // sort descending order
                return patientsList.get(idx2).getStrength()-patientsList.get(idx1).getStrength();
            }
        });

        int selectedId = candPatients.get(candPatients.size()-1);
        tempList.add(selectedId);
        return tempList;
    }

    private ArrayList<Integer>  injuryTypeSelect(ArrayList<Integer> candPatients){
        //NOTE InjuryType Sorting means sorting by strength decreasing rate
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
        Collections.sort(tempList, new Comparator<Integer>() {
            @Override
            public int compare(Integer idx1, Integer idx2) { // sort descending order
                return patientsList.get(idx1).getStrength() - patientsList.get(idx2).getStrength();
            }
        });
        int returnIndex = tempList.get(0);
        tempList.clear();
        tempList.add(returnIndex);
        return tempList;
    }

    /*----Select Slot (Stage)----*/
    private int stagePatient(Policy policy){
        int destIdx=0;
        boolean decision = makeDecision();
        Random rd = new Random();
        String actionMethod;

        if(policy != null && decision){
//            policyActionMethod = policy.getAction().getActionMethod().get(0);
            actionMethod = policy.getAction().getActionMethod();
        } else {
            Collections.shuffle(stageMethodList);
//            actionMethod = stageMethodList.get(0);
            actionMethod = "Random";
        }

        if(actionMethod.equals("Random")){
            destIdx = rd.nextInt(stageZone.length);
        }else if(actionMethod.equals("MeanRandom")){
            int totalSlot = stageZone.length;
            int[] numWaitPatient = new int[totalSlot];
            int mean = calcMeanWaitPatient(numWaitPatient, totalSlot);

            if(mean!=0){
                ArrayList<Integer> candidIdx = new ArrayList<>();
                for(int i=0; i<totalSlot; i++)
                    if(numWaitPatient[i] <= mean)
                        candidIdx.add(i);
                destIdx = candidIdx.get(rd.nextInt(candidIdx.size()));
            }else
                destIdx = rd.nextInt(stageZone.length);
        }else if(actionMethod.equals("MCSlot")){ // most crowded slot
            int maximum = Collections.max(Arrays.asList(ArrayUtils.toObject(numWaitPTS)));
            ArrayList<Integer> maxPTSSlot = new ArrayList<>();
            for(int i=0; i<numWaitPTS.length; i++)
                if(numWaitPTS[i]==maximum)
                    maxPTSSlot.add(i);
            destIdx = maxPTSSlot.get(rd.nextInt(maxPTSSlot.size()));
        }
        return destIdx;
    }

    /**------------Other manipulation-------------**/
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

    private double randomCompliance(){
        Random rd = new Random();
        double min = 0.1;
        double max = 1.0;
        double result;

        if(enforced)
            return 1;
        do{
            Double randomNum = rd.nextGaussian()*0.1+this.compliance;
            result = (double)Math.round(randomNum*10)/10;
        }while(result < min || result >= max);

        return result;
    }

    private void excludeDeadPatient(ArrayList<Integer> spotPatientList){
        ArrayList<Integer> deadList = new ArrayList<>();
        for(Integer patientId : spotPatientList){
            if(checkDeadPatient(patientId))
                deadList.add(patientId);
        }
        for (Integer patientId : deadList){
            spotPatientList.remove(spotPatientList.indexOf(patientId));
        }
    }

    private boolean checkDeadPatient(int patientId){
        Patient patient = patientsList.get(patientId);
        return patient.isDead();
    }

}
