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

        System.out.println(getName()+" "+getId()+" is at "+location.getX()+", "+location.getY());
        System.out.println("General: "+totGeneral+", Intensive: "+totIntensive+", Operating: "+totOperating+", Crew: "+medicalCrew);

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
                        System.out.println("[Hospital "+hospitalId+"]");
                        if(generalRoom.size()>0 || intensiveRoom.size()>0 || operatingRoom.size()>0){
                            status = Status.TREATING;
                        }
                        System.out.println("Waiting...");
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
                        System.out.println("[Hospital "+hospitalId+"]");

                        // release patients
                        releasePatients();
                        // rearrange rooms
                        rearrangePatients();

                        // treat and operate
                        treatPatients();
                        operatePatients();

                        // update information
                        System.out.println("General");
                        increaseStayTime(generalRoom);
                        System.out.println("Intensive");
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
        return new Random().nextFloat() < compliance;
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

    public void enterHospital(String roomType, int patientId){
        // only called by ambulances to hospitalize patient.
        Patient patient = patientsList.get(patientId);
        patient.setHospital(hospitalId);
        patient.setPrevRoomName("");
        patient.setRoomName(roomType);

        switch (roomType){
            case "General":
                generalRoom.add(patientId);
                availGeneral--;
                break;
            case "Intensive":
                intensiveRoom.add(patientId);
                availIntensive--;
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
            System.out.println("Patient "+patientId+", StayTime: "+patient.getStayTime());
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
                System.out.println("무엇인가 잘못됐다");
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
            ArrayList<String> policyActionMethod = policy.getAction().getActionMethod();
            //TODO 여기 바꿔야함
            if(policyActionMethod.size()>0){
                for(String s : policyActionMethod){
                    switch (s){
                        case "Random":
                            Collections.shuffle(tempList);
                            break;
                        case "Severity":
                            tempList = severitySelect(tempList);
                            break;
                    }
                }
            }
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
        int strength;

        // select patients to be operated
        currPolicy = checkActionPolicy(role, currAction.toString(), callBack);
        toBeOperated = selectOperatePatient(availOperating, currPolicy);
        setOperateTime(toBeOperated);

        // move them to Operating room (but reserved their bed)
        for(Integer patientId : toBeOperated){
            Patient patient = patientsList.get(patientId);
            movePatientRoom("Operating", patientId);
            System.out.println("MOVE TO OPERATING ROOM");
            if(patient.getPrevRoomName().equals("General"))
                removePatient(patientId, generalRoom);
            else if(patient.getPrevRoomName().equals("Intensive"))
                removePatient(patientId, intensiveRoom);
        }


        // operate patients in operating room
        for(Integer patientId : operatingRoom){
            Patient patient = patientsList.get(patientId);

            // move patients who are finished operation
            if(patient.getOperateTime()==0 || patient.getStrength()>=270)
                patient.setOperated(true);

            if(patient.isOperated()){
                System.out.println("Hospital RECOVERY");
                System.out.println("Original Status: "+patient.getStatus());
                patient.changeStat(); // to RECOVERY
                System.out.println("Changed Status: "+patient.getStatus());
                System.out.println("PrevRoom "+patient.getPrevRoomName()+" Severity: "+patient.getSeverity());
                System.out.println("AvailGeneral: "+availGeneral+" AvailIntensive: "+availIntensive);

                // After surgery,
                if(patient.getRoomName().equals("Intensive")){
                    if(patient.getSeverity()<7 && availGeneral>0){
                        availIntensive++;
                        movePatientRoom("General", patientId);
                        System.out.println("Before remove Intensive");
                        for(Integer i : intensiveRoom)
                            System.out.print(i+", ");
                        removePatient(patientId, intensiveRoom);
                        System.out.println("After remove Intensive");
                        for(Integer i : intensiveRoom)
                            System.out.print(i+", ");
                    } // else, go back to original bed.
                }else if(patient.getRoomName().equals("General")){
                    if(patient.getSeverity()>6 && availIntensive>0){
                        availGeneral++;
                        System.out.println("Before remove General");
                        for(Integer i : generalRoom)
                            System.out.print(i+", ");
                        movePatientRoom("Intensive", patientId);
                        removePatient(patientId, generalRoom);
                        System.out.println("After remove General");
                        for(Integer i : generalRoom)
                            System.out.print(i+", ");
                    } // else, go back to original bed.
                }
                removePatient(patientId, operatingRoom);
                availOperating++;
                break;
            }

            // new operation
            if(successSurgery){
                if(patient.getSeverity() >= 7)
//                    strength = ThreadLocalRandom.current().nextInt(20, 40);
                    strength = ThreadLocalRandom.current().nextInt(60, 80);
                else if(patient.getSeverity() >= 4)
//                    strength = ThreadLocalRandom.current().nextInt(30, 50);
                    strength = ThreadLocalRandom.current().nextInt(70, 100);
                else
//                    strength = ThreadLocalRandom.current().nextInt(50, 70);
                    strength = ThreadLocalRandom.current().nextInt(100, 120);
            } else {
                strength = ThreadLocalRandom.current().nextInt(-30, -10);
                if(new Random().nextBoolean())
                    patient.increaseOperateTime();
            }
            patient.recoverStrength(strength);
            patient.decreaseOperateTime();
        }
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

    private ArrayList<Integer> selectOperatePatient(int selectNumber, Policy policy){
        ArrayList<Integer> candidPatient = new ArrayList<>();
        ArrayList<Integer> resultList = new ArrayList<>();
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
            ArrayList<String> policyActionMethod = policy.getAction().getActionMethod();
            //TODO 여기 바꿔야함
            if(policyActionMethod.size()>0){
                for(String s : policyActionMethod){
                    switch (s){
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
                }
            }
        }else{
//            if(!decision)
//                System.out.println("Decide not to follow the policy.");
//            else
//                System.out.println("Not fitted condition or No policy");
//            System.out.println("Do the randomly select method.");
            Collections.shuffle(candidPatient);
        }

        // take patients as many as selectable
        if(candidPatient.size() > selectNumber)
            for(int i=0; i<selectNumber; i++){
                int patientId = candidPatient.get(i);
                Patient patient = patientsList.get(patientId);
                System.out.println("Hospital SURGERY");
                System.out.println("Original Status: "+patient.getStatus());
                patient.changeStat(); // to SURGERY
                System.out.println("Changed Status: "+patient.getStatus());
                resultList.add(patientId);
            }
        else
            resultList.addAll(candidPatient);

        // re-calculate operating room availability
        availOperating -= resultList.size();

        return resultList;
    }

    public void reserveRoom(String roomType){
        switch (roomType){
            case "General":
                availGeneral--;
            case "Intensive":
                availIntensive--;
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
            removePatient(i, generalRoom);
        }
    }

    private ArrayList<Integer> findReleasePatient(ArrayList<Integer> candidList, Policy policy)
    {
        ArrayList<Integer> resultList = new ArrayList<>();
        boolean decision = makeDecision();
        if(policy!=null && decision){
            ArrayList<String> policyActionMethod = policy.getAction().getActionMethod();
            if(policyActionMethod.size()>0){
                for(String s : policyActionMethod){
                    switch (s){
                        case "Severity":
                            this.severityLimit = policy.getAction().getMethodValue();
                            this.timeLimit = 2;
                            break;
                        case "Time":
                            this.severityLimit = 3;
                            this.timeLimit = policy.getAction().getMethodValue();
                            break;
                    }
                }
            }
        }else{
//            if(!decision)
//                System.out.println("Decide not to follow the policy.");
//            else
//                System.out.println("Not fitted condition or No policy");
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
        availIntensive += removeDeadPatient(intensiveRoom);
        availGeneral += removeDeadPatient(generalRoom);
        availOperating += removeDeadPatient(operatingRoom);

        // General Room
        for(int i=0; i<generalRoom.size(); i++){
            int patientId = generalRoom.get(i);
            Patient patient = patientsList.get(patientId);

            // general room to intensive room
            if(patient.getSeverity()>6 && availIntensive>0){
                availGeneral++;
                movePatientRoom("Intensive", patientId);
                movedPatients.add(patientId);
            }

            if(availIntensive==0)
                break;
        }
        for(Integer patientId : movedPatients)
            removePatient(patientId, generalRoom);
        movedPatients.clear();

        // Intensive Room
        for(int i=0; i<intensiveRoom.size(); i++){
            int patientId = intensiveRoom.get(i);
            Patient patient = patientsList.get(patientId);

            // intensive room to general room
            if(patient.getSeverity()<7 && availGeneral>0){
                movePatientRoom("General", patientId);
                movedPatients.add(patientId);
                availIntensive++;
            }

            if(availGeneral==0)
                break;
        }
        for(Integer patientId : movedPatients)
            removePatient(patientId, generalRoom);
        movedPatients.clear();
    }

    private void movePatientRoom(String roomTo, int patientId){
        Patient patient = patientsList.get(patientId);
        String prevRoom = patient.getRoomName();
        System.out.println("Patient "+patientId+" is moved from "+prevRoom+" to "+roomTo);
        patient.setPrevRoomName(patient.getRoomName());
        patient.setRoomName(roomTo);
        switch (roomTo){
            case "General":
                generalRoom.add(patientId);
                availGeneral--;
                break;
            case "Intensive":
                intensiveRoom.add(patientId);
                availIntensive--;
                break;
            case "Operating":
                operatingRoom.add(patientId);
                availOperating--;
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
        System.out.println("Hospital DEAD: "+removeList.size());
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