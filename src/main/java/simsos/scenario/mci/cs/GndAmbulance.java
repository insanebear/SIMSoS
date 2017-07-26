package simsos.scenario.mci.cs;

import simsos.scenario.mci.Location;
import simsos.scenario.mci.Patient;
import simsos.scenario.mci.Policy;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.*;

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
    private Location location;
    private int gAmbId;
    private int loadPatientId;
    private ArrayList<Integer> spotPatientList;
    private Status status;
    private int moveLimit;

    private Hospital destHospital;
    private Location destination;
    private String pRoomType;

    public GndAmbulance(World world, int gAmbId, String name, String affiliation, int moveLimit) {
        super(world);
        Random rd = new Random();
        this.name = name;
        this.affiliation = affiliation;
        this.gAmbId = gAmbId;
        this.location = new Location(rd.nextInt(patientMapSize.getLeft()),0);
        this.loadPatientId = -1;
        this.status = Status.WAITING;
        this.moveLimit = moveLimit;

    }

    @Override
    public Action step() {
        switch(status){
            case WAITING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if(checkPatient(location.getY()) && loadPatientId == -1){
                            spotPatientList = stageZone[location.getY()];
                            status = Status.LOADING;
                            System.out.println(getAffiliation()+" "+getName()+" "+getId()+"is "+getStatus()+". Ready to loading");
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
                        System.out.println(spotPatientList);
                        loadPatientId = spotPatientList.get(0);
                        spotPatientList.remove(0);
                        System.out.println("Patient "+loadPatientId+" is ready to be transferred.");
                        patientsList.get(loadPatientId).changeStat(); // TRANSFERRING

                        status = Status.READY_TO_TRANSFER;
                        System.out.println(getAffiliation()+" "+getName()+" "+getId()+"is "+getStatus()+". Start transferring.");
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
                            destination = destHospital.getLocation();
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
                        move();
                        if(location.equals(destination))
                            status = Status.DELIVERING;
                    }

                    @Override
                    public String getName() {
                        return "PTS Transferring";
                    }
                };
            case DELIVERING:
                return new Action(0) {
                    @Override
                    public void execute() {
                        Random rd = new Random();

                        destHospital.setPatient(pRoomType, loadPatientId);
                        destHospital = null;

                        destination = new Location(0,  rd.nextInt(hospitalMapSize.getRight()));
                        loadPatientId = -1;
                        status = Status.BACK_TO_SCENE;
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
                        move();
                        if(location.equals(destination))
                            status = Status.WAITING;
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

    private boolean checkPatient(int idx){
        return stageZone[idx].size() > 0;
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
        System.out.println("distX:"+distX);
        System.out.println("distY:"+distY);

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

}
