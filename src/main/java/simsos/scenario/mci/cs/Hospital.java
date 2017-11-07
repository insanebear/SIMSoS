package simsos.scenario.mci.cs;

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

import static simsos.scenario.mci.Environment.checkActionPolicy;
import static simsos.scenario.mci.Environment.checkCompliancePolicy;
import static simsos.scenario.mci.Environment.patientsList;

/**
 * Created by mgjin on 2017-06-28.
 *
 * Edited by Youlim Jung on 2017-07-25.
 */
public class Hospital extends Agent {
    private enum Status {WAITING, TREATING, INACTIVE}
    private enum Actions {WAIT, TREAT, OPERATE, RELEASE}
    private String name;
    private String role;
    private int hospitalId;
    private Location location;
    private int medicalCrew;
    private boolean active;

    private double compliance; // indicates how much CS will follow policies
    private double treatCompliance;
    private double operateCompliance;
    private double releaseCompliance;
    private boolean enforced;

    private int totGeneral;
    private int totIntensive;
    private int totOperating;

    private ArrayList<Integer> patientsInHospital;
    private ArrayList<Integer> generalRoom;
    private ArrayList<Integer> intensiveRoom;
    private ArrayList<Integer> operatingRoom;

    private int needSurgeryGeneral;
    private int needSurgeryIntensive;

    private int availGeneral;
    private int availIntensive;
    private int availOperating;

    private Policy currPolicy;
    private Actions currAction;

    // for release
    private int severityLimit;
    private int timeLimit;



    private Status status;

    private CallBack callBack = new CallBack() {
        @Override
        public boolean checkCSStat(String condString, int condValue) {
//            if(condString.equals("Story") && story==condValue)
//                return true;
            return false;
        }
    };


    public Hospital(World world, int hospitalId, String name,
                    int general, int intensive, int operating, Location location, int medicalCrew, double compliance, boolean enforced) {
        super(world);
        this.name = name;
        this.role = "TREATMENT";
        this.hospitalId = hospitalId;
        this.totGeneral = general;
        this.totIntensive = intensive;
        this.totOperating = operating;

        this.availGeneral = this.totGeneral;
        this.availIntensive = this.totIntensive;
        this.availOperating = this.totOperating;

        this.location = location;
        this.medicalCrew = medicalCrew;

        this.compliance = compliance;
        this.treatCompliance = randomCompliance();
        this.operateCompliance = randomCompliance();
        this.releaseCompliance = randomCompliance();
        this.enforced = enforced;

        patientsInHospital = new ArrayList<>();
        generalRoom = new ArrayList<>();
        intensiveRoom = new ArrayList<>();
        operatingRoom = new ArrayList<>();

        this.status = Status.WAITING;
        this.currAction = Actions.WAIT;
        this.currPolicy = null;

//        System.out.println(getName()+" "+getId()+" is at "+location.getX()+", "+location.getY());
//        System.out.println("General: "+totGeneral+", Intensive: "+totIntensive+", Operating: "+totOperating+", Crew: "+medicalCrew);

        this.reset();
    }

    @Override
    public Action step() {
        // check if it is active or not every time it starts its action
        // If true, accept patients
        // If false, not accept patients


        switch(status){
            case WAITING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        active = checkActive();
//                        System.out.println("[Hospital "+hospitalId+"]");
                        if(generalRoom.size()>0 || intensiveRoom.size()>0 || operatingRoom.size()>0){
                            status = Status.TREATING;
                        }
//                        System.out.println("Waiting...");
                    }
                    @Override
                    public String getName() {
                        return "Hospital waiting";
                    }
                };

            case TREATING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        active = checkActive();
//                        System.out.println("[Hospital "+hospitalId+"]");

                        // release patients
                        releasePatients();
                        // rearrange rooms
                        rearrangePatients();

                        // treat and operate
                        treatPatients();
                        operatePatients();

                        // update information
//                        System.out.println("General");
                        increaseStayTime(generalRoom);
//                        System.out.println("Intensive");
                        increaseStayTime(intensiveRoom);
                        calcNeedSurgeryPatients();
                        updatePatientPeriod();

                        if(generalRoom.isEmpty() && intensiveRoom.isEmpty() && operatingRoom.isEmpty())
                            status = Status.WAITING;
                    }
                    @Override
                    public String getName() {
                        return "Hospital working.";
                    }
                };
        }
        return Action.getNullAction(1, this.getName() + ": No patient");
    }

    @Override
    public void reset() {

    }

    @Override
    public int getId() {
        return this.hospitalId;
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

    public void reserveRoom(String roomType){
        switch (roomType){
            case "General":
                if(availGeneral > 0)
                    availGeneral--;
                break;
            case "Intensive":
                if(availIntensive > 0)
                    availIntensive--;
                break;
        }
    }

    public void enterHospital(String roomType, int patientId){
        // only called by ambulances to hospitalize patient.
        Patient patient = patientsList.get(patientId);
        patient.setHospital(hospitalId);
        patient.setPrevRoomName("");
        patient.setRoomName(roomType);

        switch (roomType){
            case "General":
                generalRoom.add(patientId);
                break;
            case "Intensive":
                intensiveRoom.add(patientId);
                break;
        }
        patientsInHospital.add(patientId);
        // categorize patient by setting treatPeriod;
        categorizePatient(patientId);
    }

    private void categorizePatient(int patientId){
        Patient patient = patientsList.get(patientId);
        Random rd = new Random();
        if(patient.getSeverity() >= 7)
            patient.setTreatPeriod(rd.nextInt(2)+1); // 1, 2
        else if(patient.getSeverity() >= 4)
            patient.setTreatPeriod(rd.nextInt(3)+1); // 1, 2, 3
        else
            patient.setTreatPeriod(rd.nextInt(3)+2); // 2, 3, 4
        patient.resetWaitPeriod();
    }

    private void increaseStayTime(ArrayList<Integer> room){
        Patient patient;
        for (int i=0; i<room.size(); i++){
            int patientId = room.get(i);
            patient = patientsList.get(patientId);
            patient.increaseStayTime();
        }
    }

    /**--- Treat ---**/
    private void treatPatients(){
        // related to only general room
        currAction = Actions.TREAT;

        ArrayList<Integer> candPatients = new ArrayList<>();
        ArrayList<Integer> treatPatients;
        Random rd = new Random();
        int member = rd.nextInt(3)+1;
        int availCrewGroup = medicalCrew / member;

        // select patient to be treated
        for(Integer patientId : generalRoom){
            Patient patient = patientsList.get(patientId);
            if (!patient.isTreated())
                candPatients.add(patientId);
        }

        if(candPatients.size() >= availCrewGroup){
            currPolicy = checkActionPolicy(role, currAction.toString(), callBack);
            treatPatients = selectTreatPatient(candPatients, availCrewGroup, currPolicy);
        }
        else
            treatPatients = candPatients;

        // treat patient (General Room)
        for (Integer patientId : treatPatients) {
            Patient patient = patientsList.get(patientId);
            if(patient.getInjuryType() == Patient.InjuryType.BURN)
                patient.recoverStrength(rd.nextInt(3)+10);
            else if(patient.getInjuryType() == Patient.InjuryType.BLEEDING)
                patient.recoverStrength(rd.nextInt(4)+10);
            else if(patient.getInjuryType() == Patient.InjuryType.FRACTURED)
                patient.recoverStrength(rd.nextInt(4)+15);
            patient.setTreated(true);
            // re-categorize before wait period starts.
            categorizePatient(patientId);
            patient.resetWaitPeriod();
        }

        // treat patient (Intensive Room)
        for(Integer patientId : intensiveRoom){
            Patient patient = patientsList.get(patientId);
            if(patient.getInjuryType() == Patient.InjuryType.BURN)
                patient.recoverStrength(rd.nextInt(3)+10);
            else if(patient.getInjuryType() == Patient.InjuryType.BLEEDING)
                patient.recoverStrength(rd.nextInt(4)+10);
            else if(patient.getInjuryType() == Patient.InjuryType.FRACTURED)
                patient.recoverStrength(rd.nextInt(4)+10);
            patient.setTreated(true);
            patient.resetWaitPeriod();
        }
    }

    private void updatePatientPeriod(){
        // to fair treatment
        for(Integer patientId : generalRoom){
            Patient patient = patientsList.get(patientId);
            if(patient.getWaitPeriod()>0 && patient.isTreated())
                patient.decreaseWaitPeriod();
            if(patient.getWaitPeriod() == 0)
                patient.setTreated(false);
        }
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Integer> selectTreatPatient(ArrayList<Integer> candidList, int selectNumber, Policy policy){
        ArrayList<Integer> tempList = (ArrayList<Integer>) candidList.clone();
        ArrayList<Integer> resultList = new ArrayList<>();
        boolean decision = makeDecision();
        if(policy!=null && decision){
            String actionMethod = policy.getAction().getActionMethod();
            switch (actionMethod){
                case "Random":
                    Collections.shuffle(tempList);
                    break;
                case "Severity":
                    tempList = severitySelect(tempList);
                    break;
            }
//            ArrayList<String> policyActionMethod = policy.getAction().getActionMethod();
//            if(policyActionMethod.size()>0){
//                for(String s : policyActionMethod){
//                    switch (s){
//                        case "Random":
//                            Collections.shuffle(tempList);
//                            break;
//                        case "Severity":
//                            tempList = severitySelect(tempList);
//                            break;
//                    }
//                }
//            }
        }else{
//            if(!decision)
//                System.out.println("Decide not to follow the policy.");
//            else
//                System.out.println("Not fitted condition or No policy");
//            System.out.println("Do the randomly select method.");
            Collections.shuffle(tempList);
        }
        for(int i=0; i<selectNumber; i++)
            resultList.add(tempList.get(i));
        return resultList;
    }

    private ArrayList<Integer> severitySelect(ArrayList<Integer> candidList){
        ArrayList<Integer> tempList = new ArrayList<>();
        HashMap<Integer, Integer> severityInfo = new HashMap<>();

        for(Integer patientId : candidList){
            int severity = patientsList.get(patientId).getSeverity();
            severityInfo.put(patientId, severity);
        }
        severityInfo = sortByValue(severityInfo);
        tempList.addAll(severityInfo.keySet());
        return tempList;
    }

    private ArrayList<Integer> injuryTypeSelect(ArrayList<Integer> candidList){
        ArrayList<Integer> tempList = new ArrayList<>();
        HashMap<Integer, Integer> injuryTypeInfo = new HashMap<>();

        for(Integer patientId : candidList){
            int decreasingRate = patientsList.get(patientId).strengthDecreasingRate();
            injuryTypeInfo.put(patientId, decreasingRate);
        }
        injuryTypeInfo = sortByValue(injuryTypeInfo);
        tempList.addAll(injuryTypeInfo.keySet());
        return tempList;
    }

    /**--- Operate ---**/
    private void operatePatients(){
        currAction = Actions.OPERATE;

        boolean successSurgery = new Random().nextBoolean();
        ArrayList<Integer> toBeOperated;
        int recoverStrength;

        // select patients to be operated
        currPolicy = checkActionPolicy(role, currAction.toString(), callBack);
        if(availOperating>0){
            toBeOperated = selectOperatePatient(availOperating, currPolicy);
            setOperateTime(toBeOperated);
            // move them to Operating room but reserved their bed(Not reducing availability)
            for(Integer patientId : toBeOperated){
                Patient patient = patientsList.get(patientId);
                movePatientRoom("Operating", patientId);
                if(patient.getPrevRoomName().equals("General"))         // maintain the bed number
                    removePatient(patientId, generalRoom);
                else if(patient.getPrevRoomName().equals("Intensive"))  // maintain the bed number
                    removePatient(patientId, intensiveRoom);
            }
        }

        // operate patients in operating room
        for(Integer patientId : operatingRoom){
            Patient patient = patientsList.get(patientId);

            // move patients who are finished operation
            if(patient.getOperateTime()==0 || patient.getStrength()>=270)
                patient.setOperated(true);

            if(patient.isOperated()){
                patient.changeStat(); // to RECOVERY
                // After surgery,
                if(patient.getPrevRoomName().equals("Intensive")){
                    if(patient.getSeverity()<7 && availGeneral>0){
                        movePatientRoom("General", patientId);
                        availIntensive++;
                        removePatient(patientId, intensiveRoom);
                    } else { // else, go back to original bed.
                        patient.setPrevRoomName(patient.getRoomName());
                        patient.setRoomName("Intensive");
                    }
                }else if(patient.getPrevRoomName().equals("General")){
                    if(patient.getSeverity()>6 && availIntensive>0){
                        movePatientRoom("Intensive", patientId);
                        availGeneral++;
                        removePatient(patientId, generalRoom);
                    } else {// else, go back to original bed.
                        patient.setPrevRoomName(patient.getRoomName());
                        patient.setRoomName("General");
                    }
                }
                removePatient(patientId, operatingRoom);
                availOperating++;
                break;
            }

            // new operation
            if(successSurgery){
                if(patient.getSeverity() >= 7)
//                    strength = ThreadLocalRandom.current().nextInt(20, 40);
                    recoverStrength = ThreadLocalRandom.current().nextInt(60, 80);
                else if(patient.getSeverity() >= 4)
//                    strength = ThreadLocalRandom.current().nextInt(30, 50);
                    recoverStrength = ThreadLocalRandom.current().nextInt(70, 100);
                else
//                    strength = ThreadLocalRandom.current().nextInt(50, 70);
                    recoverStrength = ThreadLocalRandom.current().nextInt(100, 120);
            } else {
                recoverStrength = ThreadLocalRandom.current().nextInt(-20, -1);
                if(new Random().nextBoolean())
                    patient.increaseOperateTime();
            }
            patient.recoverStrength(recoverStrength);
            patient.decreaseOperateTime();
        }
    }

    private ArrayList<Integer> selectOperatePatient(int selectNumber, Policy policy){
        ArrayList<Integer> candidPatient = new ArrayList<>();
        ArrayList<Integer> resultList = new ArrayList<>();;
        boolean decision = makeDecision();
        // sort out candidate patients for operation
        for(Integer patientId : generalRoom){
            Patient patient = patientsList.get(patientId);
            if(!patient.isOperated() && patient.getSeverity()>2)    // exclude severity 0, 1, 2
                candidPatient.add(patientId);
        }

        for(Integer patientId : intensiveRoom){
            Patient patient = patientsList.get(patientId);
            if(!patient.isOperated())
                candidPatient.add(patientId);
        };

        // select patients to be operated
        if(policy!=null && decision){
            String actionMethod = policy.getAction().getActionMethod();
            switch (actionMethod){
                case "Random":
                    Collections.shuffle(candidPatient);
                    break;
                case "ArriveTime":
                    Collections.sort(candidPatient, Comparator.comparing(item -> patientsInHospital.indexOf(item)));
                    break;
                case "Severity":
                    candidPatient = severitySelect(candidPatient);
                    break;
                case "InjuryType":
                    candidPatient = injuryTypeSelect(candidPatient);
                    break;
            }
//            ArrayList<String> policyActionMethod = policy.getAction().getActionMethod();
//            if(policyActionMethod.size()>0){
//                for(String s : policyActionMethod){
//                    switch (s){
//                        case "Random":
//                            Collections.shuffle(candidPatient);
//                            break;
//                        case "ArriveTime":
//                            Collections.sort(candidPatient, Comparator.comparing(item -> patientsInHospital.indexOf(item)));
//                            break;
//                        case "Severity":
//                            candidPatient = severitySelect(candidPatient);
//                            break;
//                        case "InjuryType":
//                            candidPatient = injuryTypeSelect(candidPatient);
//                            break;
//                    }
//                }
        }else{
            Collections.shuffle(candidPatient);
        }

        // take patients as many as selectable
        if(candidPatient.size() > selectNumber){
            for(int i=0; i<selectNumber; i++){
                int patientId = candidPatient.get(i);
                Patient patient = patientsList.get(patientId);
                patient.changeStat(); // to SURGERY
                resultList.add(patientId);
            }
        }else
            resultList.addAll(candidPatient);
        return resultList;
    }

    private void setOperateTime(ArrayList<Integer> toBeOperated){
        Random rd = new Random();
        for (Integer patientId : toBeOperated){
            Patient patient = patientsList.get(patientId);
            int time=1;
            if(patient.getSeverity() >= 7)
                time = rd.nextInt(3)+2; // 2, 3, 4
            else if(patient.getSeverity() >= 4)
                time = rd.nextInt(2)+1; // 1, 2
            else if(patient.getSeverity() == 3)
                time = 1;
            patient.setOperateTime(time);
        }
    }

    /**----RELEASE----**/
    private void releasePatients(){
        currAction = Actions.RELEASE;

        ArrayList<Integer> releaseList;

        // check policy
        currPolicy = checkActionPolicy(role, currAction.toString(), callBack);
        releaseList = findReleasePatient(generalRoom, currPolicy);

        for(int i=0; i<releaseList.size(); i++){
            availGeneral++;
            removePatient(i, generalRoom);
        }
    }

    private ArrayList<Integer> findReleasePatient(ArrayList<Integer> candidList, Policy policy)
    {
        ArrayList<Integer> resultList = new ArrayList<>();
        boolean decision = makeDecision();
        if(policy!=null && decision){
            String actionMethod = policy.getAction().getActionMethod();
            switch (actionMethod) {
                case "Severity":
                    this.severityLimit = policy.getAction().getMethodValue();
                    this.timeLimit = 2;
                    break;
                case "Time":
                    this.severityLimit = 3;
                    this.timeLimit = policy.getAction().getMethodValue();
                    break;
            }
//            ArrayList<String> policyActionMethod = policy.getAction().getActionMethod();
//            if(policyActionMethod.size()>0){
//                for(String s : policyActionMethod){
//                    switch (s){
//                        case "Severity":
//                            this.severityLimit = policy.getAction().getMethodValue();
//                            this.timeLimit = 2;
//                            break;
//                        case "Time":
//                            this.severityLimit = 3;
//                            this.timeLimit = policy.getAction().getMethodValue();
//                            break;
//                    }
//                }
//            }
        }else{
            this.severityLimit = 3;
            this.timeLimit = 2;
        }

        for(Integer patientId : candidList){
            Patient patient = patientsList.get(patientId);
            if(patient.getSeverity() <= this.severityLimit){
                if(patient.getStayTime() >= this.timeLimit){
                    resultList.add(patientId);
                    patient.setReleased(true);
                }
            }
        }
        return resultList;
    }

    private void rearrangePatients(){
        // NOTE remove dead patients and move patients according to their status
        ArrayList<Integer> movedPatients = new ArrayList<>();

        // handle dead patients
        int vacancy;

        // General Room
        vacancy = removeDeadPatient(generalRoom);
        if(vacancy>0)
            if(availGeneral+vacancy <= totGeneral)
                availGeneral += vacancy;
//            System.out.println("Something is wrong in rearrangePatient for generaRoom");

        // Intensive Room
        vacancy = removeDeadPatient(intensiveRoom);
        if(vacancy>0)
            if(availIntensive+vacancy <= totIntensive)
                availIntensive += vacancy;
//            System.out.println("Something is wrong in rearrangePatient for intensiveRoom");

        // Operating Room
        vacancy = removeDeadPatient(operatingRoom);
        if(vacancy>0)
            if(availOperating+vacancy <= totOperating)
                availOperating += vacancy;
//            System.out.println("Something is wrong in rearrangePatient for operatingRoom");

        // General Room rearrangement
        for(int i=0; i<generalRoom.size(); i++){
            int patientId = generalRoom.get(i);
            Patient patient = patientsList.get(patientId);

            // general room to intensive room
            if(patient.getSeverity()>6 && availIntensive>0 && availIntensive<=totIntensive)
                movedPatients.add(patientId);

            if(availIntensive==0)
                break;
        }
        for(Integer patientId : movedPatients){
            movePatientRoom("Intensive", patientId);    // decrease intensive availability
            availGeneral++;
            removePatient(patientId, generalRoom);
        }
        movedPatients.clear();

        // Intensive Room rearrangement
        for(int i=0; i<intensiveRoom.size(); i++){
            int patientId = intensiveRoom.get(i);
            Patient patient = patientsList.get(patientId);

            // intensive room to general room
            if(patient.getSeverity()<7 && availGeneral>0 && availGeneral<=totGeneral)
                movedPatients.add(patientId);

            if(availGeneral==0)
                break;
        }
        for(Integer patientId : movedPatients){
            movePatientRoom("General", patientId);   // decrease general availability
            availIntensive++;
            removePatient(patientId, generalRoom);
        }
        movedPatients.clear();
    }

    private void movePatientRoom(String roomTo, int patientId){
        Patient patient = patientsList.get(patientId);
        String prevRoom = patient.getRoomName();

        patient.setPrevRoomName(prevRoom);
        patient.setRoomName(roomTo);
        switch (roomTo){
            case "General":
                if(availGeneral > 0){
                    generalRoom.add(patientId);
                    availGeneral--;
                }
//                else
//                    System.out.println("Something is wrong in movePatientRoom (General)");
                break;
            case "Intensive":
                if(availIntensive > 0){
                    intensiveRoom.add(patientId);
                    availIntensive--;
                }
//                else
//                    System.out.println("Something is wrong in movePatientRoom (Intensive)");
                break;
            case "Operating":
                if(availOperating > 0){
                    availOperating--;
                    operatingRoom.add(patientId);
                }
//                else
//                    System.out.println("Something is wrong in movePatientRoom (Operating)");
                break;
        }
    }

    private int removeDeadPatient(ArrayList<Integer> room){
        ArrayList<Integer> removeList = new ArrayList<>();
        int vacancy = 0;

        if(!room.isEmpty())
            for(Integer patientId : room){
                Patient patient = patientsList.get(patientId);
                if(patient.getStatus() == Patient.Status.DEAD){
                    removeList.add(patientId); // list up on removeList
                    vacancy++;
                }
            }
        for(Integer patientId : removeList)
            removePatient(patientId, room);
        return vacancy;
    }

    // Other manipulations
    public int getNeedSurgeryGeneral() {
        return needSurgeryGeneral;
    }

    public int getNeedSurgeryIntensive() {
        return needSurgeryIntensive;
    }

    private void calcNeedSurgeryPatients(){
        int tempGeneral=0, tempIntensive=0;

        for(Integer pId : generalRoom){
            Patient p = patientsList.get(pId);
            if(!p.isOperated())
                tempGeneral++;
        }
        for(Integer pId : intensiveRoom){
            Patient p = patientsList.get(pId);
            if(!p.isOperated())
                tempIntensive++;
        }

        needSurgeryGeneral = tempGeneral;
        needSurgeryIntensive = tempIntensive;
    }

    private void removePatient(int idx, ArrayList<Integer> patientList){
        for(int i=0; i<patientList.size(); i++){
            if(patientList.get(i) == idx)
                patientList.remove(i);
        }
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
                    case "Treat":
                        if(treatCompliance < policy.getMinCompliance())
                            this.active = false;
                        break;
                    case "Operate":
                        if(operateCompliance < policy.getMinCompliance())
                            this.active = false;
                        break;
                    case "Release":
                        if(releaseCompliance < policy.getMinCompliance())
                            this.active = false;
                        break;
                }
                if (!this.active)
                    break;
            }
        }
        return this.active;
    }

    // Getters and Setters
    public boolean isActive() {
        return active;
    }

    public Location getLocation() {
        return location;
    }

    public int getAvailGeneral() {
        return availGeneral;
    }

    public int getAvailIntensive() {
        return availIntensive;
    }

    public int getTotGeneral() {
        return totGeneral;
    }

    public int getTotIntensive() {
        return totIntensive;
    }

    public int getTotOperating() {
        return totOperating;
    }

    public ArrayList<Integer> getGeneralRoom() {
        return generalRoom;
    }

    public ArrayList<Integer> getIntensiveRoom() {
        return intensiveRoom;
    }
}