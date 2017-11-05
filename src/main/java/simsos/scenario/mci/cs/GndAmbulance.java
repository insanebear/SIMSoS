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
import java.util.concurrent.ThreadLocalRandom;
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
    private boolean active;

    private Location location;
    private Location originLocation;
    private Status status;
    private Actions currAction;
    private Policy currPolicy;
    private int findingTrial;

    private int loadPatientId;
    private ArrayList<Integer> spotPatientList;

    private double compliance; // indicates how much CS will follow policies
    private double loadCompliance;
    private double deliverCompliance;
    private double returnCompliance;
    private double waitCompliance;
    private boolean enforced;

    private ArrayList<String> loadMethodList;
    private ArrayList<String> deliverMethodList;
    private ArrayList<String> returnMethodList;

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
        Random rd = new Random();
        this.name = name;
        this.affiliation = affiliation;
        this.role = "TRANSPORT";

        if(checkActive()){
            this.status = Status.WAITING;
            this.currAction = Actions.WAIT;
        }else{
            this.status = Status.INACTIVE;
            this.currAction = Actions.NONE;
        }
        this.findingTrial = 0;
        this.gAmbId = gAmbId;
        this.location = new Location(rd.nextInt(patientMapSize.getLeft()), 0);
        this.originLocation = new Location(this.location.getX(), this.location.getY());
        this.loadPatientId = -1;
        this.status = Status.WAITING;
        this.currAction = Actions.WAIT;

        this.compliance = compliance;
        this.loadCompliance = randomCompliance();
        this.deliverCompliance = randomCompliance();
        this.returnCompliance = randomCompliance();
        this.waitCompliance = randomCompliance();
        this.enforced = enforced;

        loadMethodList = new ArrayList<>();
        deliverMethodList = new ArrayList<>();
        returnMethodList = new ArrayList<>();
        makeActionMethodList();

        this.reachTime = setReachTime();
        this.defaultWait = 1; //TODO review the policy which can manage waiting time.
        setWaitTime(defaultWait);
    }

    @Override
    public Action step() {
        switch(status){
            case INACTIVE:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if(checkActive()){
                            Random rd = new Random();
                            location = new Location(rd.nextInt(patientMapSize.getLeft()), 0);
                            numWaitPTS[location.getX()] += 1;
                            status = Status.WAITING;
                            currAction = Actions.WAIT;
                        }
                    }

                    @Override
                    public String getName() {
                        return "GndAmulance Inactive";
                    }
                };
            case WAITING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if(checkActive()){
                            if(waitTime>0){
                                if(stageZone[location.getX()].size()>0 && loadPatientId == -1) {
                                    status = Status.LOADING;
                                    currAction = Actions.LOAD;
                                    numWaitPTS[location.getX()] -= 1;
                                }
                                else
                                    waitTime--;
                            }else{ //TODO review the policy which can manage waiting time.
                                if(location.getX()+1 <= hospitalMapSize.getLeft())
                                    location.moveX(1);
                                else
                                    location.setX(0);
                                setWaitTime(defaultWait);
                            }
                        } // else, this CS does not join SoS now.
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
//                        System.out.println("Ambulance location: "+location.getX()+", "+location.getY());
                        if(spotPatientList.size()>0){ // double check if there is a patient
                            currPolicy = checkActionPolicy(role, currAction.toString(), callBack);
                            loadPatientId = loadPatient(spotPatientList, currPolicy);

//                            System.out.println("Patient "+loadPatientId+" is ready to be transported.");
                            Patient patient = patientsList.get(loadPatientId);
//                            System.out.println("GND ambulance LOADED");
//                            System.out.println("Original Status: "+patient.getStatus());
                            patientsList.get(loadPatientId).changeStat(); // to LOADED
//                            System.out.println("Changed Status: "+patient.getStatus());
//                            System.out.println("Loaded patient: "+loadPatientId);
//                            System.out.println();
                            status = Status.READY_TO_TRANSPORT;
                            currAction = Actions.READY_TRANSPORT;
//                            System.out.println(getAffiliation()+" "+getName()+" "+getId()+"is "+getStatus()+". Start transporting.");
                        }else{ // Another waiting ambulance took a patient! No patients on the spot!
                            status = Status.WAITING;
                            currAction = Actions.WAIT;

                            if(location.getX()+1 < hospitalMapSize.getLeft())
                                location.moveX(1);
                            else
                                location.moveX(-hospitalMapSize.getLeft()+1);
//                            System.out.println("Ambulance waits patients on changed place.");
//                            System.out.println("Changed location: "+location.getX()+", "+location.getY());
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
//                        System.out.println("<<<Ambulance "+getId()+">>>");
                        //TODO review policy in severity
                        Patient patient = patientsList.get(loadPatientId);

                        if(!patient.isDead()){  // Loaded patient is not dead
                            if(patient.getSeverity()>6 && !patient.getInjuryType().equals(Patient.InjuryType.FRACTURED))
                                pRoomType = "Intensive";
                            else
                                pRoomType = "General";

//                            System.out.println("Finding Hospital -- "+findingTrial+" th trial.");
                            destHospital = selectHospital(pRoomType, currPolicy);
//                            System.out.println("Patient ID: "+loadPatientId);
                            if(destHospital != null){
//                                System.out.println("Destination Hospital: "+destHospital.getId());
                                status = Status.TRANSPORTING;
                                currAction = Actions.TRANSPORT;
                                destHospital.reserveRoom(pRoomType);
                                destination = destHospital.getLocation();
                                reachTime = setReachTime();
//                                System.out.println("GND ambulance TRANSPORTING");
//                                System.out.println("Original Status: "+patient.getStatus());
                                patient.changeStat(); // TRANSPORTING
//                                System.out.println("Changed Status: "+patient.getStatus());
//                                System.out.println();
                            }
                            else{
//                                System.out.println(loadPatientId+" did not get a destination hospital.");
//                                System.out.println();
                                findingTrial++;
                                // First aid
                                patient.recoverStrength(new Random().nextInt(15));
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
//                        System.out.println("GND ambulance SURGERY_WAIT");
//                        System.out.println("Original Status: "+patient.getStatus());
                        patient.changeStat(); // to SURGERY_WAIT
//                        System.out.println("Changed Status: "+patient.getStatus());

                        destHospital.enterHospital(pRoomType, loadPatientId);

                        status = Status.BACK_TO_SCENE;
                        currAction = Actions.RETURN;

                        loadPatientId = -1;
                        destHospital = null;
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
                            if(checkActive()){
                                location = destination;
                                numWaitPTS[location.getX()] += 1;
                                status = Status.WAITING;
                                currAction = Actions.WAIT;
                            }else{
                                location = null;
                                status = Status.INACTIVE;
                                currAction = Actions.NONE;
                            }

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
        return new Random().nextFloat() < compliance;
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

    /**----Select Hospital (DeliverTo)----**/
    @SuppressWarnings("unchecked")
    private Hospital selectHospital(String pRoomType, Policy policy){
        ArrayList<Hospital> hospitals = (ArrayList<Hospital>) SoSManager.hospitals.clone();
        ArrayList<Integer> availHospitals = checkAvailHospitals(pRoomType, hospitals);
        Random rd = new Random();
        int resIdx = -1;
        boolean decision = makeDecision();
        if(availHospitals.size()==0){
//            System.out.println("No available hospital now.");
            return null;    // no available hospital now.
        }
        if(policy != null && decision){
            String actionMethod = policy.getAction().getActionMethod();
            switch (actionMethod){
                case "Random":
                    availHospitals = randomSelect(availHospitals);
                    break;
                case "Distance":
                    availHospitals = distanceSelect(availHospitals);
                    break;
                case "Vacancy":
                    availHospitals = vacancySelect(availHospitals);
                    break;
                case "Rate":
                    availHospitals = rateSelect(availHospitals);
                    break;
            }
            if(availHospitals.size() > 1)
                resIdx = availHospitals.get(rd.nextInt(availHospitals.size()));
            else if (availHospitals.size() == 1)
                resIdx = availHospitals.get(0);
//            ArrayList<String> policyActionMethod = policy.getAction().getActionMethod();
//            if(policyActionMethod.size()>0){
//                for(String s : policyActionMethod){
//                    switch (s){
//                        case "Random":
//                            availHospitals = randomSelect(availHospitals);
//                            break;
//                        case "Distance":
//                            availHospitals = distanceSelect(availHospitals);
//                            break;
//                        case "Vacancy":
//                            availHospitals = vacancySelect(availHospitals);
//                            break;
//                        case "Rate":
//                            availHospitals = rateSelect(availHospitals);
//                            break;
//                    }
//                    if(availHospitals.size() == 1){
////                        System.out.println(s + "Select used. (Hospital)");
//                        break;
//                    }
//                }
//                if(availHospitals.size() > 1)
//                    resIdx = availHospitals.get(rd.nextInt(availHospitals.size()));
//                else if (availHospitals.size() == 1)
//                    resIdx = availHospitals.get(0);
//            }
        }else{
//            if(!decision)
//                System.out.println("Decide not to follow the policy.");
//            else
//                System.out.println("Not fitted condition or No policy");
//            System.out.println("Do the randomly select method.");

            Collections.shuffle(deliverMethodList);
            for(String s : deliverMethodList){
                switch (s){
                    case "Random":
                        availHospitals = randomSelect(availHospitals);
                        break;
                    case "Distance":
                        availHospitals = distanceSelect(availHospitals);
                        break;
                    case "Vacancy":
                        availHospitals = vacancySelect(availHospitals);
                        break;
                    case "Rate":
                        availHospitals = rateSelect(availHospitals);
                        break;
                }
                if(availHospitals.size() == 1){
//                    System.out.println(s + "Select used. (Hospital)");
                    break;
                }
            }
            if(availHospitals.size() > 1)
                resIdx = availHospitals.get(rd.nextInt(availHospitals.size()));
            else if (availHospitals.size() == 1)
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
        // rate = # of operating room / # of existing patients who did not get a surgery.
        int i=0;
        int maxRate=0;
        ArrayList<Integer> tempList = new ArrayList<>();
        HashMap<Integer, Integer> hRateInfo = new HashMap<>();

        for(Integer hospitalId : availHospitals) {
            Hospital hospital = null;
            for(Hospital h : SoSManager.hospitals)
                if (hospitalId == h.getId())
                    hospital = h;

            int rate = 0;
            if(pRoomType.equals("General"))
                rate = hospital.getTotOperating()
                        / (hospital.getGeneralRoom().size()-hospital.getNeedSurgeryGeneral());
            else if(pRoomType.equals("Intensive"))
                rate = hospital.getTotOperating()
                        / (hospital.getIntensiveRoom().size()-hospital.getNeedSurgeryIntensive());

            hRateInfo.put(hospitalId, rate);
        }
        hRateInfo = sortByValueReverse(hRateInfo);

        for(Integer id : hRateInfo.keySet()){
            if(i==0)
                maxRate = hRateInfo.get(id);
            if(hRateInfo.get(id) == maxRate)
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
//                    avail = h.getTotGeneral() - h.getGeneralRoom().size();
                    avail = h.getAvailGeneral();
                    if (avail > 0 && h.isActive()){
                        availHospitals.add(h.getId());
                    }
                }
                break;
            case "Intensive":
                for(Hospital h : hospitals) {
//                    avail = h.getTotIntensive() - h.getIntensiveRoom().size();
                    avail = h.getAvailIntensive();
                    if (avail > 0 && h.isActive()){
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

    /**----Select Patient (Load)----**/
    @SuppressWarnings("unchecked")
    private int loadPatient(ArrayList<Integer> patientList, Policy policy) {
        ArrayList<Integer> candPatients = (ArrayList<Integer>) patientList.clone();
        Random rd = new Random();
        boolean decision = makeDecision();
        int resIdx = -1;

        if(policy != null && decision){
            String actionMethod = policy.getAction().getActionMethod();
            switch (actionMethod) {
                case "Random":
                    candPatients = randomLoad(candPatients);
                    break;
                case "Distance":
                    candPatients = distanceLoad(candPatients);
                    break;
                case "Severity":
                    candPatients = severityLoad(candPatients);
                    break;
                case "InjuryType":
                    candPatients = injuryTypeLoad(candPatients);
                    break;
            }
            if (candPatients.size() != 1)
                resIdx = candPatients.get(rd.nextInt(candPatients.size()));
            else
                resIdx = candPatients.get(0);

//            ArrayList<String> policyActionMethod = policy.getAction().getActionMethod();
//            if(policyActionMethod.size()>0) {
//                for (String s : policyActionMethod) {
//                    switch (s) {
//                        case "Random":
//                            candPatients = randomLoad(candPatients);
//                            break;
//                        case "Distance":
//                            candPatients = distanceLoad(candPatients);
//                            break;
//                        case "Severity":
//                            candPatients = severityLoad(candPatients);
//                            break;
//                        case "InjuryType":
//                            candPatients = injuryTypeLoad(candPatients);
//                            break;
//                    }
//                    if (candPatients.size() == 1){
////                        System.out.println(s + "Load used. (Patient)");
//                        break;
//                    }
//                }
//                if (candPatients.size() != 1)
//                    resIdx = candPatients.get(rd.nextInt(candPatients.size()));
//                else
//                    resIdx = candPatients.get(0);
//            }
        } else {
//            if(!decision)
//                System.out.println("Decide not to follow the policy.");
//            else
//                System.out.println("Not fitted condition or No policy");
//            System.out.println("Do the randomly select method.");

            Collections.shuffle(loadMethodList);
            for(String s: loadMethodList){
                switch (s){
                    case "Random":
                        candPatients = randomLoad(candPatients);
                        break;
                    case "Distance":
                        candPatients = distanceLoad(candPatients);
                        break;
                    case "Severity":
                        candPatients = severityLoad(candPatients);
                        break;
                    case "InjuryType":
                        candPatients = injuryTypeLoad(candPatients);
                        break;
                }
                if(candPatients.size() == 1){
//                    System.out.println(s + "Load used. (Patient)");
                    break;
                }
            }
            if(candPatients.size()!=1)
                resIdx = candPatients.get(rd.nextInt(candPatients.size()));
            else
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

    /**----Select Slot (ReturnTo)----**/
    private Location selectSlot(Policy policy){
        int resIdx=0;
        boolean decision = makeDecision();
        Random rd = new Random();
        String policyActionMethod;

        if(policy != null && decision){
            policyActionMethod = policy.getAction().getActionMethod();
//            policyActionMethod = policy.getAction().getActionMethod().get(0);
        } else {
//            if(!decision)
//                System.out.println("Decide not to follow the policy.");
//            else
//                System.out.println("Not fitted condition or No policy");
//            System.out.println("Do the randomly select method.");

            Collections.shuffle(returnMethodList);
            policyActionMethod = returnMethodList.get(0);
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
        //TODO review
        // This can be revised to get a certain distribution from outside if policy is applied.
        return ThreadLocalRandom.current().nextInt(2, 5);
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
        double min = this.compliance*0.3;
        double max = 1-min;
        double tempCompliance = 0;
        while(tempCompliance < min || tempCompliance > max){
            tempCompliance = Math.round(rd.nextGaussian()*3 + this.compliance*10);
            tempCompliance = tempCompliance/10;
        }
        return tempCompliance;
    }

    private boolean checkActive(){
        ArrayList<Policy> compliancePolicies = checkCompliancePolicy(role);
        this.active = true;

        if(compliancePolicies.size() != 0){ // policy exists
            for(Policy policy : compliancePolicies){
                String actionName = policy.getAction().getActionName();
                switch (actionName){
                    case "Load":
                        if(loadCompliance < policy.getMinCompliance())
                            this.active = false;
                        break;
                    case "DeliverTo":
                        if(deliverCompliance < policy.getMinCompliance())
                            this.active = false;
                        break;
                    case "ReturnTo":
                        if(returnCompliance < policy.getMinCompliance())
                            this.active = false;
                        break;
                    case "Wait":
                        if(waitCompliance < policy.getMinCompliance())
                            this.active = false;
                        break;
                }
                if (!this.active)
                    break;
            }
        }
        return this.active;
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
//        System.out.println("StageDEAD: "+deadList.size());
        for (Integer patientId : deadList){
            spotPatientList.remove(spotPatientList.indexOf(patientId));
        }
    }

    //TODO policy구현 (시간 관련)
    private void setWaitTime(int time){
        this.waitTime = time;
    }

    private void removePatient(int idx, ArrayList<Integer> patientList){
        for(int i=0; i<patientList.size(); i++)
            if(patientList.get(i) == idx)
                patientList.remove(i);
    }

}
