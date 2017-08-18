package simsos.scenario.mci.cs;

import simsos.scenario.mci.Location;
import simsos.scenario.mci.Patient;
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
    private enum Status {WAITING, LOADING, READY_TO_TRANSPORT, TRANSPORTING, DELIVERING, BACK_TO_SCENE}

    private Patient.InjuryType patientType;

    private String affiliation;
    private String name;
    private Location location; // for waiting, loading
    private int gAmbId;
    private int loadPatientId;
    private ArrayList<Integer> spotPatientList;
    private Status status;

    private double conformRate; // indicates how much CS will follow policies

    private Hospital destHospital;
    private Location destination; // for transporting
    private String pRoomType;

    //
    private int reachTime;
    private int waitTime;
    private int defaultWait;

    public GndAmbulance(World world, int gAmbId, String name, String affiliation, double conformRate) {
        super(world);
        Random rd = new Random();
        this.name = name;
        this.affiliation = affiliation;
        this.gAmbId = gAmbId;
        this.location = new Location(rd.nextInt(patientMapSize.getLeft()), 0);
        this.loadPatientId = -1;
        this.status = Status.WAITING;
        this.conformRate = conformRate;
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
                            if(stageZone[location.getX()].size()>0 && loadPatientId == -1) {
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
                            System.out.println("Patient "+loadPatientId+" is ready to be transported.");

                            patientsList.get(loadPatientId).changeStat(); // LOADED

                            status = Status.READY_TO_TRANSPORT;
                            System.out.println(getAffiliation()+" "+getName()+" "+getId()+"is "+getStatus()+". Start transporting.");
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
            case READY_TO_TRANSPORT:
                return new Action(1) {
                    @Override
                    public void execute() {
                        Patient p = patientsList.get(loadPatientId);
                        //TODO review policy in severity
                        if(p.getSeverity()<7)
                            pRoomType = "General";
                        else
                            pRoomType = "Intensive";

                        destHospital = checkHospital(pRoomType);

                        if(destHospital != null){
                            status = Status.TRANSPORTING;
                            destHospital.reserveRoom(pRoomType);

                            destination = destHospital.getLocation();
                            reachTime = setReachTime();
                            p.changeStat(); // TRANSPORTING
                        }
                        else
                            System.out.println(getAffiliation()+" "+getName()+" "+getId()+" did not get a destination hospital.");
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

    public int getgAmbId() {
        return gAmbId;
    }

    public Status getStatus() {
        return status;
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
            }
        }
        return null;
    }

    private ArrayList<Hospital> sortHospitalByDist(){
        ArrayList<Hospital> tempList = (ArrayList<Hospital>) SoSManager.hospitals.clone();

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
