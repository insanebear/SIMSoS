package simsos.scenario.mci.cs;

import simsos.scenario.mci.Location;
import simsos.scenario.mci.Patient;
import simsos.scenario.mci.Policy;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static simsos.scenario.mci.Environment.*;

/**
 *
 * Created by Youlim Jung on 22/07/2017.
 *
 */
public class GndAmbulance extends Agent{
    private enum Status {WAITING, LOADING, READY_TO_TRANSFER, TRANSFERRING, DELIVERING, BACK_TO_SCENE}

    private Patient.InjuryType patientType;

    private String affiliation;
    private String name;
    private Location location; // for waiting, loading
    private int gAmbId;
    private int loadPatientId;
    private ArrayList<Integer> spotPatientList;
    private Status status;
    private int moveLimit;

    private Hospital destHospital;
    private Location destination; // for transferring
    private String pRoomType;

    //
    private int reachTime;
    private int waitTime;
    private int defaultWait;

    public GndAmbulance(World world, int gAmbId, String name, String affiliation, int moveLimit) {
        super(world);
        Random rd = new Random();
        this.name = name;
        this.affiliation = affiliation;
        this.gAmbId = gAmbId;
        this.location = new Location(rd.nextInt(patientMapSize.getLeft()), 0);
        this.loadPatientId = -1;
        this.status = Status.WAITING;
        this.moveLimit = moveLimit;
        //
        this.reachTime = setReachTime();
        this.defaultWait = 1; //TODO review the policy which can manage waiting time.
        setWaitTime(defaultWait);
    }

    @Override
    public Action step() {
        switch(status){
            case WAITING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if(waitTime>0){
                            System.out.println("왜 아무것도 없어??? "+stageZone[location.getX()]);
                            if(stageZone[location.getX()].size()>0 && loadPatientId == -1) {
                                System.out.println("이제 태우는거야?");
                                status = Status.LOADING;
                            }
                            else
                                waitTime--;
                        }else{ //TODO review the policy which can manage waiting time.
                            if(location.getX()+1 < hospitalMapSize.getLeft())
                                location.moveX(1);
                            else
                                location.moveX(-hospitalMapSize.getLeft()+1);
                            setWaitTime(defaultWait);
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
                        //TODO change to searching a patient whose status is not DEAD.
                        spotPatientList = stageZone[location.getX()];
                        System.out.println("Ambulance location: "+location.getX()+", "+location.getY());
                        if(spotPatientList.size()>0){ // double check if there is a patient
                            loadPatientId = spotPatientList.get(0);
                            spotPatientList.remove(0);
                            System.out.println("Patient "+loadPatientId+" is ready to be transferred.");

                            patientsList.get(loadPatientId).changeStat(); // LOADED

                            status = Status.READY_TO_TRANSFER;
                            System.out.println(getAffiliation()+" "+getName()+" "+getId()+"is "+getStatus()+". Start transferring.");
                        }else{ // Another waiting ambulance took a patient!
                            status = Status.WAITING;

                            if(location.getX()+1 < hospitalMapSize.getLeft())
                                location.moveX(1);
                            else
                                location.moveX(-hospitalMapSize.getLeft()+1);

                            System.out.println("Ambulance waits patients on changed place.");
                            System.out.println("Changed location: "+location.getX()+", "+location.getY());
                        }
                    }
                    @Override
                    public String getName() {
                        return "PTS Loading";
                    }
                };
            case READY_TO_TRANSFER:
                return new Action(1) {
                    @Override
                    public void execute() {
                        Patient p = patientsList.get(loadPatientId);
                        if(p.getSeverity()<5)
                            pRoomType = "General";
                        else if(p.getSeverity()>=5 && p.getSeverity()<8)
                            pRoomType = "Intensive";
                        else
                            pRoomType = "Operating";

                        destHospital = checkHospital(pRoomType);

                        if(destHospital==null && pRoomType.equals("Operating")){
                            pRoomType = "Intensive";
                            destHospital = checkHospital(pRoomType);
                            // check immediately again due to severity.
                        }

                        if(destHospital != null){
                            status = Status.TRANSFERRING;
                            destHospital.reserveRoom(pRoomType);

                            destination = destHospital.getLocation();
                            reachTime = setReachTime();
                            p.changeStat(); // TRANSFERRING
                        }
                        else
                            System.out.println(getAffiliation()+" "+getName()+" "+getId()+"do not get a destination hospital.");
                    }

                    @Override
                    public String getName() {
                        return "PTS Searching Hospital";
                    }
                };
            case TRANSFERRING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if(reachTime == 0){
                            location = destination;
                            status = Status.DELIVERING;
                        }else
                            reachTime--;
                    }

                    @Override
                    public String getName() {
                        return "PTS Transferring";
                    }
                };
            case DELIVERING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        Random rd = new Random();
                        Patient p = patientsList.get(loadPatientId);

                        p.changeStat(); // SURGERY_WAIT

                        destHospital.setPatient(pRoomType, loadPatientId);

                        status = Status.BACK_TO_SCENE;
                        loadPatientId = -1;
                        destHospital = null;
                        destination = setLocation();
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
                            status = Status.WAITING;
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
    public HashMap<String, Object> getProperties() {
        return new HashMap<String, Object>();
    }

    @Override
    public void injectPolicies(ArrayList<Policy> policies) {

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

    public void move(){
        Random rd = new Random();
        int distX = destination.getX()-location.getX();
        int distY = destination.getY()-location.getY();
        boolean goX = rd.nextBoolean();

        if(distY==0 || goX){
            if(Math.abs(distX)>moveLimit)
                location.moveX(rd.nextInt(moveLimit));
            else if(distX<0 && Math.abs(distX)<moveLimit)
                location.moveX(-rd.nextInt(Math.abs(distX)));
            else if(distX>0 && Math.abs(distX)<moveLimit)
                location.moveX(rd.nextInt(distX));
        }else{
            if(Math.abs(distY)>moveLimit)
                location.moveY(rd.nextInt(moveLimit));
            else if(distY<0 && Math.abs(distY)<moveLimit)
                location.moveY(-rd.nextInt(Math.abs(distY)));
            else if(distY>0 && Math.abs(distY)<moveLimit)
                location.moveY(rd.nextInt(distY));
        }
    }

    private Hospital checkHospital(String roomType){
        int avail;
        ArrayList<Hospital> sortedHospitals = sortHospitalByDist();

        for(Hospital h : sortedHospitals){
            switch (roomType){
                case "General":
                    avail = h.getTotGeneral()-h.getGeneralList().size();
                    if(avail > 0)
                        return h;

                case "Intensive":
                    avail = h.getTotIntensive()-h.getIntensiveList().size();
                    if(avail > 0)
                        return h;

                case "Operating":
                    avail = h.getTotOperating()-h.getOperatingList().size();
                    if(avail > 0)
                        return h;
            }
        }
        return null;
    }

    private ArrayList<Hospital> sortHospitalByDist(){
        ArrayList<Hospital> tempList = new ArrayList<>(SoSManager.hospitals);
        tempList = (ArrayList<Hospital>)SoSManager.hospitals.clone();

        Collections.sort(tempList, new Comparator<Hospital>() {
            @Override
            public int compare(Hospital h1, Hospital h2) {
                return h1.getLocation().distanceTo(location)-h2.getLocation().distanceTo(location);
            } // check if neg or pos value.
        });

        return tempList;
    }

    private int setReachTime(){
        //TODO review
        // This can be revised to get a certain distribution from outside if policy is applied.
        return ThreadLocalRandom.current().nextInt(3, 10);
    }

    private Location setLocation(){ //stage location
        Random rd = new Random();

        return new Location(rd.nextInt(stageZone.length),0);
    }

    private void setWaitTime(int time){
        this.waitTime = time;
    }

}
