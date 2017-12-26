package simsos.scenario.mci.cs;

import org.apache.commons.lang3.ArrayUtils;
import simsos.scenario.mci.Location;
import simsos.scenario.mci.Patient;
import simsos.scenario.mci.policy.CallBack;
import simsos.scenario.mci.policy.Policy;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.*;
import java.util.stream.Collectors;

import static simsos.scenario.mci.Environment.*;
import static simsos.scenario.mci.cs.SoSManager.numWaitPTS;

/**
 *
 * Created by Youlim Jung on 22/07/2017.
 *
 */
public class GndAmbulance extends Agent{
    private enum Status {WAITING, LOADING, READY_TO_TRANSPORT, TRANSPORTING, DELIVERING, BACK_TO_SCENE, INACTIVE}

    private enum Actions{ WAIT, LOAD, READY_TRANSPORT, TRANSPORT, DELIVER, RETURN, NONE }

    private int gAmbId;
    private String affiliation;
    private String name;
    private String role;
    private String csType;

    private Location location;
    private Location originLocation;
    private Status status;
    private Actions currAction;
    private Policy currPolicy;

    private int loadPatientId;
    private ArrayList<Integer> spotPatientList;

    private final double compliance; // indicates how much CS will follow policies
    private final double loadCompliance;
    private final double deliverCompliance;
    private final double returnCompliance;
    private final double waitCompliance;
    private double policyGivenCompliance;
    private boolean newCompFlag;
    private final boolean enforced;

    private final ArrayList<String> loadMethodList;
    private final ArrayList<String> deliverMethodList;
    private final ArrayList<String> returnMethodList;

    private Hospital destHospital;
    private Location destination; // for transporting
    private String pRoomType;
    //
    private int reachTime;
    private int waitTime;
    private int defaultWait;

    private CallBack callBack = new CallBack() {
        @Override
        public boolean checkCSStat(String condString, int condValue) {
            if(condString.equals("Time") && waitTime==condValue)
                return true;
            return false;
        }
    };

    public GndAmbulance(World world, int gAmbId, String name, String affiliation, double compliance, boolean enforced) {
        super(world);
        this.name = name;
        this.affiliation = affiliation;
        this.role = "TRANSPORT";
        this.gAmbId = gAmbId;
        this.compliance = compliance;
        this.enforced = enforced;
        this.loadCompliance = randomCompliance();
        this.deliverCompliance = randomCompliance();
        this.returnCompliance = randomCompliance();
        this.waitCompliance = randomCompliance();

        if(this.enforced)
            this.csType = "D";
        else
            this.csType = "A";

        loadMethodList = new ArrayList<>();
        deliverMethodList = new ArrayList<>();
        returnMethodList = new ArrayList<>();
        makeActionMethodList();

        this.reset();
    }

    @Override
    public Action step() {
        switch(status){
            case WAITING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if(waitTime > 0){
                            if(stageZone[location.getX()].size()>0 && loadPatientId == -1) {
                                status = Status.LOADING;
                                currAction = Actions.LOAD;
                                numWaitPTS[location.getX()] -= 1;
                            }
                            else
                                waitTime--;
                        }else if(waitTime == 0){
                            checkComplianceValue("Wait");
                            currPolicy = checkActionPolicy(role, csType,"Wait", callBack);

                            boolean decision;

                            if (newCompFlag)
                                decision = makeDecision(policyGivenCompliance);
                            else
                                decision = makeDecision();


                            if(currPolicy!=null && decision){
                                int stayAmount = currPolicy.getAction().getMethodValue();
                                setWaitTime(stayAmount);
                            }else
                                setWaitTime(defaultWait);

                            if(location.getX()+1 <= hospitalMapSize.getLeft())
                                location.moveX(1);
                            else
                                location.setX(0);
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
                        spotPatientList = stageZone[location.getX()];
                        // exclude dead patients
                        excludeDeadPatient(spotPatientList);
                        if(spotPatientList.size()>0){ // double check if there is a patient
                            checkComplianceValue("Load");
                            currPolicy = checkActionPolicy(role, csType,"Load", callBack);
                            loadPatientId = loadPatient(spotPatientList, currPolicy);
                            Patient patient = patientsList.get(loadPatientId);
                            patient.changeStat(); // to LOADED

                            status = Status.READY_TO_TRANSPORT;
                            currAction = Actions.READY_TRANSPORT;
                        }else{ // Another waiting ambulance took a patient! No patients on the spot!
                            status = Status.WAITING;
                            currAction = Actions.WAIT;

                            if(location.getX()+1 < hospitalMapSize.getLeft())
                                location.moveX(1);
                            else
                                location.moveX(-hospitalMapSize.getLeft()+1);
                        }
                    }
                    @Override
                    public String getName() {
                        return "PTS Loading";
                    }
                };
            case READY_TO_TRANSPORT:
                return new Action(1) {
                    @Override
                    public void execute() {
                        Patient patient = patientsList.get(loadPatientId);

                        if(!patient.isDead()){  // Loaded patient is not dead
                            if(patient.getSeverity()>6 && !patient.getInjuryType().equals(Patient.InjuryType.FRACTURED))
                                pRoomType = "Intensive";
                            else
                                pRoomType = "General";
                            checkComplianceValue("DeliverTo");
                            currPolicy = checkActionPolicy(role, csType, "DeliverTo", callBack);
                            destHospital = selectHospital(pRoomType, currPolicy);
                            if(destHospital != null){
                                status = Status.TRANSPORTING;
                                currAction = Actions.TRANSPORT;
                                destHospital.reserveRoom(pRoomType);
                                destination = destHospital.getLocation();
                                reachTime = setReachTime();
                                patient.changeStat(); // TRANSPORTING
                            }
                            else{
                                // First aid
                                patient.recoverStrength(new Random().nextInt(20));
                            }
                        } else {    // LoadedPatient is Dead.
                            status = Status.LOADING;
                            currAction = Actions.LOAD;
                        }
                    }

                    @Override
                    public String getName() {
                        return "PTS Searching Hospital";
                    }
                };
            case TRANSPORTING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if(reachTime == 0){
                            location = destination;
                            status = Status.DELIVERING;
                            currAction = Actions.DELIVER;
                        }else
                            reachTime--;
                    }

                    @Override
                    public String getName() {
                        return "PTS Transporting";
                    }
                };
            case DELIVERING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        Patient patient = patientsList.get(loadPatientId);
                        patient.changeStat(); // to SURGERY_WAIT

                        destHospital.enterHospital(pRoomType, loadPatientId);

                        status = Status.BACK_TO_SCENE;
                        currAction = Actions.RETURN;
                        loadPatientId = -1;
                        destHospital = null;
                        checkComplianceValue("ReturnTo");
                        currPolicy = checkActionPolicy(role, csType, "ReturnTo", callBack);
                        destination = selectSlot(currPolicy);
                    }

                    @Override
                    public String getName() {
                        return "PTS Delivering";
                    }
                };
            case BACK_TO_SCENE:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if(reachTime == 0){
                            location = destination;
                            numWaitPTS[location.getX()] += 1;
                            status = Status.WAITING;
                            currAction = Actions.WAIT;
                        }else
                            reachTime--;
                    }

                    @Override
                    public String getName() {
                        return "PTS Back to scene";
                    }
                };
        }
        return Action.getNullAction(0, this.getName()+": do nothing.");
    }

    @Override
    public void reset() {
        Random rd = new Random();
        this.location = new Location(rd.nextInt(patientMapSize.getLeft()), 0);
        this.originLocation = new Location(this.location.getX(), this.location.getY());

        this.status = Status.WAITING;
        this.currAction = Actions.WAIT;
        numWaitPTS[location.getX()] += 1;

        this.loadPatientId = -1;
//        this.reachTime = setReachTime();
        this.defaultWait = 1; //TODO review the policy which can manage waiting time.
        setWaitTime(defaultWait);
        this.newCompFlag = false;
        this.policyGivenCompliance = 0.0;
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
    public boolean makeDecision() {
        return enforced || new Random().nextFloat() < compliance;
    }

    public boolean makeDecision(double compValue) {
        return enforced || new Random().nextFloat() < compValue;
    }

    @Override
    public HashMap<String, Object> getProperties() {
        return new HashMap<String, Object>();
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

    /**----Select Patient (Load)----**/
    @SuppressWarnings("unchecked")
    private int loadPatient(ArrayList<Integer> patientList, Policy policy) {
        ArrayList<Integer> candPatients = (ArrayList<Integer>) patientList.clone();
        Random rd = new Random();
        boolean decision;
        int resIdx = -1;

        if (newCompFlag)
            decision = makeDecision(policyGivenCompliance);
        else
            decision = makeDecision();

        if(policy != null && decision){
            String actionMethod = policy.getAction().getActionMethod();
            switch (actionMethod) {
                case "Distance":
                    candPatients = distanceLoad(candPatients);
                    break;
                case "Severity":
                    candPatients = severityLoad(candPatients);
                    break;
                case "InjuryType":
                    candPatients = injuryTypeLoad(candPatients);
                    break;
                case "Random":
                    candPatients = randomLoad(candPatients);
                    break;
            }
            if (candPatients.size() != 1)
                resIdx = candPatients.get(rd.nextInt(candPatients.size()));
            else
                resIdx = candPatients.get(0);
        } else {
            candPatients = randomLoad(candPatients);
            resIdx = candPatients.get(0);
        }
        removePatient(resIdx, patientList);
        return resIdx;
    }

    private ArrayList<Integer> randomLoad(ArrayList<Integer> candPatients){
        ArrayList<Integer> tempList = new ArrayList<>();
        int candidIdx = new Random().nextInt(candPatients.size());
        tempList.add(candPatients.get(candidIdx));
        return tempList;
    }

    private ArrayList<Integer> distanceLoad(ArrayList<Integer> candPatients){
        ArrayList<Integer> tempList = new ArrayList<>();
        tempList.add(candPatients.get(0));
        return tempList;
    }

    private ArrayList<Integer> severityLoad(ArrayList<Integer> candPatients){
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

    private ArrayList<Integer> injuryTypeLoad(ArrayList<Integer> candPatients){
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

        return tempList;
    }

    /**----Select Hospital (DeliverTo)----**/
    @SuppressWarnings("unchecked")
    private Hospital selectHospital(String pRoomType, Policy policy){
        ArrayList<Hospital> hospitals = (ArrayList<Hospital>) SoSManager.hospitals.clone();
        ArrayList<Integer> availHospitals = checkAvailHospitals(pRoomType, hospitals);
        Random rd = new Random();
        int resIdx = -1;
        boolean decision;
        if(availHospitals.size()==0){
            return null;    // no available hospital now.
        }

        if (newCompFlag)
            decision = makeDecision(policyGivenCompliance);
        else
            decision = makeDecision();

        if(policy != null && decision){
            String actionMethod = policy.getAction().getActionMethod();
            switch (actionMethod){
                case "Distance":
                    availHospitals = distanceSelect(availHospitals);
                    break;
                case "Vacancy":
                    availHospitals = vacancySelect(availHospitals);
                    break;
                case "Rate":
                    availHospitals = rateSelect(availHospitals);
                    break;
                case "Random":
                    availHospitals = randomSelect(availHospitals);
                    break;
            }
            if(availHospitals.size() > 1)
                resIdx = availHospitals.get(rd.nextInt(availHospitals.size()));
            else if (availHospitals.size() == 1)
                resIdx = availHospitals.get(0);
        }else{
            availHospitals = randomSelect(availHospitals);
            resIdx = availHospitals.get(0);
        }

        for(Hospital hospital : SoSManager.hospitals)
            if(resIdx == hospital.getId())
                return hospital;

        return null;
    }

    private ArrayList<Integer> randomSelect(ArrayList<Integer> availHospitals){
        ArrayList<Integer> tempList = new ArrayList<>();
        int candidIdx = new Random().nextInt(availHospitals.size());
        tempList.add(availHospitals.get(candidIdx));
        return tempList;
    }

    private ArrayList<Integer> distanceSelect(ArrayList<Integer> availHospitals){
        int i=0;
        int minDistance=0;
        ArrayList<Integer> tempList = new ArrayList<>();
        HashMap<Integer, Integer> hDistanceInfo = new HashMap<>();

        for(Integer hospitalId : availHospitals) {
            Hospital hospital = null;
            for(Hospital h : SoSManager.hospitals) {
                if (hospitalId == h.getId())
                    hospital = h;
            }
            int distance = hospital.getLocation().distanceTo(location);
            hDistanceInfo.put(hospitalId, distance);
        }
        hDistanceInfo = sortByValue(hDistanceInfo);

        for(Integer id : hDistanceInfo.keySet()){
            if(i==0)
                minDistance = hDistanceInfo.get(id);
            if(hDistanceInfo.get(id) == minDistance)
                tempList.add(id);
            i++;
        }
        return tempList;
    }

    private ArrayList<Integer> vacancySelect(ArrayList<Integer> availHospitals){
        int i=0;
        int maxVacancy=0;
        ArrayList<Integer> tempList = new ArrayList<>();
        HashMap<Integer, Integer> hVacancyInfo = new HashMap<>();

        for(Integer hospitalId : availHospitals) {
            Hospital hospital = null;
            for(Hospital h : SoSManager.hospitals) {
                if (hospitalId == h.getId())
                    hospital = h;
            }

            int vacancy = 0;
            if(pRoomType.equals("General"))
                vacancy = hospital.getTotGeneral()-hospital.getGeneralRoom().size();
            else if(pRoomType.equals("Intensive"))
                vacancy = hospital.getTotIntensive()-hospital.getIntensiveRoom().size();

            hVacancyInfo.put(hospitalId, vacancy);
        }
        hVacancyInfo = sortByValueReverse(hVacancyInfo);

        for(Integer id : hVacancyInfo.keySet()){
            if(i==0)
                maxVacancy = hVacancyInfo.get(id);
            if(hVacancyInfo.get(id) == maxVacancy)
                tempList.add(id);
            i++;
        }
        return tempList;
    }

    private ArrayList<Integer> rateSelect(ArrayList<Integer> availHospitals){
        // rate = # of existing patients who did not get a surgery / # of operating room.
        int i=0;
        int minRate=0;
        HashMap<Integer, Integer> hRateInfo = new HashMap<>();

        for(Integer hospitalId : availHospitals) {
            Hospital hospital = null;
            for(Hospital h : SoSManager.hospitals)
                if (hospitalId == h.getId())
                    hospital = h;
                if(hospital != null){
                    int rate = 0;
                    if(pRoomType.equals("General"))
                        rate = hospital.getNeedSurgeryGeneral() / hospital.getTotOperating();
                    else if(pRoomType.equals("Intensive"))
                        rate = hospital.getNeedSurgeryIntensive() / hospital.getTotOperating();
                    hRateInfo.put(hospitalId, rate);
                }
        }
        hRateInfo = sortByValue(hRateInfo);

        ArrayList<Integer> tempList = new ArrayList<>();
        for(Integer id : hRateInfo.keySet()){
            if(i==0)
                minRate = hRateInfo.get(id);
            if(hRateInfo.get(id) == minRate)
                tempList.add(id);
            i++;
        }
        return tempList;
    }

    private ArrayList<Integer> checkAvailHospitals(String roomType, ArrayList<Hospital> hospitals){
        int avail;
        ArrayList<Integer> availHospitals = new ArrayList<>();
        switch (roomType){
            case "General":
                for(Hospital h : hospitals) {
                    avail = h.getAvailGeneral();
                    if (avail > 0){
                        availHospitals.add(h.getId());
                    }
                }
                break;
            case "Intensive":
                for(Hospital h : hospitals) {
                    avail = h.getAvailIntensive();
                    if (avail > 0){
                        availHospitals.add(h.getId());
                    }
                }
                break;
        }
        return availHospitals;
    }

    private <K, V extends Comparable<? super V>> HashMap<K, V> sortByValue(HashMap<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private <K, V extends Comparable<? super V>> HashMap<K, V> sortByValueReverse(HashMap<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**----Select Slot (ReturnTo)----**/
    private Location selectSlot(Policy policy){
        int resIdx = 0;
        boolean decision;
        Random rd = new Random();
        String policyActionMethod;

        if (newCompFlag)
            decision = makeDecision(policyGivenCompliance);
        else
            decision = makeDecision();

        if(policy!=null && decision){
            policyActionMethod = policy.getAction().getActionMethod();
        } else {
            policyActionMethod = "Random";
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
                    if(numWaitPatient[i] >= mean)
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
        }else if(policyActionMethod.equals("Original")){
            resIdx = originLocation.getX();
        }
        return new Location(resIdx,0);
    }

    private int calcMeanWaitPatient(int[] numWaitPatient, int totalSlot){
        int sum=0;
        for(int i=0; i<totalSlot; i++)
            numWaitPatient[i] = stageZone[i].size();
        for(int number : numWaitPatient)
            sum += number;
        return Math.round(sum/totalSlot);
    }

    private int setReachTime(){
        // distance-relative time
        int weight;
        int diffLocation = location.distanceTo(destination); // always > 0, min:0, max:2*radius

        // weight by distance
        weight = calDistWeight(diffLocation);
        return calReachTime(weight);
    }

    private int calDistWeight(int distance){
        int radius = hospitalMapSize.getLeft();
        if(distance == 0)
            return 0;
        else if(distance>0 && distance<(radius/4))
            return 1;
        else if(distance>=(radius/4) && distance<(radius/2))
            return 2;
        else
            return 3;
    }

    private int calReachTime(int weight){
        Random rd = new Random();
        int mean = 1 + weight;
        double stdDev = 1;
        int result = 0;
        boolean isValid = false;

        while(!isValid){
            result = (int)Math.round(rd.nextGaussian()*stdDev + mean);
            if(result>0)
                isValid = true;
        }
        return result;
    }

    private void makeActionMethodList(){
        loadMethodList.add("Random");
        loadMethodList.add("Distance");
        loadMethodList.add("Severity");
        loadMethodList.add("InjuryType");
        deliverMethodList.add("Random");
        deliverMethodList.add("Distance");
        deliverMethodList.add("Severity");
        deliverMethodList.add("InjuryType");
        returnMethodList.add("Random");
        returnMethodList.add("MeanRandom");
        returnMethodList.add("MCSlot");
        returnMethodList.add("Original");
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

    private void checkComplianceValue(String currAction){
        ArrayList<Policy> compliancePolicies = checkCompliancePolicy(role);
        ArrayList<Policy> currCompliancePolicies = new ArrayList<>();

        if(compliancePolicies.size() != 0) { // policy exists
            for (Policy policy : compliancePolicies) {
                String actionName = policy.getAction().getActionName();
                if (currAction.equals(actionName))
                    currCompliancePolicies.add(policy);
            }
        }   // gather policies related to current action name

        for (Policy policy : currCompliancePolicies) {
            if (currAction.equals("Load"))
                if (loadCompliance < policy.getMinCompliance() && makeDecision(loadCompliance)){
                    this.policyGivenCompliance = policy.getMinCompliance();
                    this.newCompFlag = true;
                }else
                    this.newCompFlag = false;
            else if (currAction.equals("DeliverTo"))
                if (deliverCompliance < policy.getMinCompliance() && makeDecision(deliverCompliance)){
                    this.policyGivenCompliance = policy.getMinCompliance();
                    this.newCompFlag = true;
                }else
                    this.newCompFlag = false;
            else if (currAction.equals("ReturnTo"))
                if (returnCompliance < policy.getMinCompliance() && makeDecision(returnCompliance)){                    this.newCompFlag = false;
                    this.policyGivenCompliance = policy.getMinCompliance();
                    this.newCompFlag = true;
                }else
                    this.newCompFlag = false;
            else if (currAction.equals("Wait"))
                if (waitCompliance < policy.getMinCompliance() && makeDecision(waitCompliance)){
                    this.policyGivenCompliance = policy.getMinCompliance();
                    this.newCompFlag = true;
                }else
                    this.newCompFlag = false;

            if (!newCompFlag)
                break;
        }
    }

    private boolean checkDeadPatient(int patientId){
        Patient patient = patientsList.get(patientId);
        return patient.isDead();
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

    private void setWaitTime(int time){
        this.waitTime = time;
    }

    private void removePatient(int idx, ArrayList<Integer> patientList){
        for(int i=0; i<patientList.size(); i++)
            if(patientList.get(i) == idx)
                patientList.remove(i);
    }

}
