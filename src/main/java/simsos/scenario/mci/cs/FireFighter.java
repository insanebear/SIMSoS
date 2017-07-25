package simsos.scenario.mci.cs;

import simsos.scenario.mci.*;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static simsos.scenario.mci.Environment.patientMapSize;
import static simsos.scenario.mci.Environment.patientsList;
import static simsos.scenario.mci.Environment.stageZone;

/**
 *
 * Created by Youlim Jung on 18/07/2017.
 *
 */
public class FireFighter extends Agent{
    private enum Status{
        SEARCHING, RESCUING, TRANSFERRING
    }
    private enum Directions{
        WEST, SOUTH, EAST, NORTH
    }
    private Patient.InjuryType patientType;

    private String affiliation;
    private String name;
    private int fighterId;
    private Location location;
    private int rescuedPatientId;
    private ArrayList<Integer> spotPatientList;
    private Status status;
    private int moveLimit;


    public FireFighter(World world, int fighterId, String name, String affiliation, int moveLimit) {
        super(world);
        this.name = name;
        this.affiliation = affiliation;
        this.fighterId = fighterId;
        this.location = new Location(0, 0);
        this.rescuedPatientId = -1;     // no patient rescued
        this.status = Status.SEARCHING;
        this.moveLimit = moveLimit;
    }

    @Override
    public Action step() {
        switch(status){
            case SEARCHING:
                return new Action(1) {
                    // duration 어떻게하지
                    @Override
                    public void execute() {
                        move();
                        System.out.println(getAffiliation()+" Fighter "+getId()+" is at "+ location.getX()+", "+location.getY());
                        spotPatientList = Environment.patientMap[location.getX()][location.getY()];
                        if(spotPatientList.size()>0)
                            status = Status.RESCUING;
                    }

                    @Override
                    public String getName() {
                        return "Searching";
                    }
                };
            case RESCUING:
                return new Action(1){
                    // duration 어떻게하지
                    @Override
                    public void execute() {
                        rescue();
                    }

                    @Override
                    public String getName() {
                        return "Rescuing";
                    }
                };
            case TRANSFERRING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if(location.getX() == patientMapSize.getLeft())
                            stage();
                        else
                            move();
                    }

                    @Override
                    public String getName() {
                        return "Transferring";
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
    public HashMap<String, Object> getProperties() {
        return new HashMap<String, Object>();
    }

    @Override
    public void injectPolicies(ArrayList<Policy> policies) {

    }

    public String getAffiliation() {
        return affiliation;
    }

    public void rescue(){
        if(checkPatient(spotPatientList) && rescuedPatientId == -1){
            // later, policy can be applied
            // now rescue the first patient
            rescuedPatientId = spotPatientList.get(0);
            spotPatientList.remove(0);
            System.out.println("Patient Status:"+patientsList.get(rescuedPatientId).getStatus());
            patientsList.get(rescuedPatientId).changeStat(); // RESCUED

            status = Status.TRANSFERRING;

            System.out.println("Patient "+rescuedPatientId+" rescued. " +
                    "Patient stat is changed into "+Environment.patientsList.get(rescuedPatientId).getStatus());
        }
    }

    public void move(){
        Random rd = new Random();
        int move = rd.nextInt(moveLimit)+1;
        int currentX = location.getX();
        int currentY = location.getY();

        if(status == Status.SEARCHING){
            Directions direction = setDirection();
            switch(direction){
                case EAST:
                    if(currentX+move > patientMapSize.getLeft())
                        location.moveX(patientMapSize.getLeft()-currentX);
                    else
                        location.moveX(move);
                    break;
                case WEST:
                    if(currentX-move < 0)
                        location.moveX(-currentX);
                    else
                        location.moveX(-move);
                    break;
                case NORTH:
                    if(currentY+move > patientMapSize.getRight())
                        location.moveY(patientMapSize.getRight()-currentY);
                    else
                        location.moveY(move);
                    break;
                case SOUTH:
                    if(currentY-move < 0)
                        location.moveY(-currentY);
                    else
                        location.moveY(-move);
                    break;
            }
        }else{
            // transferring
            if(currentX+move > patientMapSize.getLeft())
                location.moveX(patientMapSize.getLeft()-currentX);
            else
                location.moveX(move);
            System.out.println(getAffiliation()+" "+getName()+" "+getId()+" is at "+location.getX()+", "+location.getY());
        }
    }

    public void stage(){
        //stage patient on the stage zone.(number is same as fighter's Y cord.)
        stageZone[location.getY()].add(rescuedPatientId);
        Environment.patientsList.get(rescuedPatientId).changeStat(); // TRANSFER_WAIT

        System.out.println(getAffiliation()+" "+getName()+" "+getId()+" is at "+location.getX()+", "+location.getY());
        System.out.println("Patient "+rescuedPatientId+" is staged on "
                +location.getY()+"th area. Patient is "+Environment.patientsList.get(rescuedPatientId).getStatus());

        rescuedPatientId = -1;  // initialize rescuePatient
        status = Status.SEARCHING;
    }

    public Directions setDirection(){
        // 너무 랜덤해서 자꾸 다시 돌아옴. 최소한 조금 전에 왔던 방향으로는 가지 않도록 수정필요
        Random rd = new Random();
        Directions[] directionArr;
        int currentX = location.getX();
        int currentY = location.getY();

        if(currentX==0 || currentY==0){
            if(currentX==0 && currentY==0){
                // (0, 0): N or E
                directionArr = new Directions[2];
                directionArr[0] = Directions.NORTH;
                directionArr[1] = Directions.EAST;

                return directionArr[rd.nextInt(2)];
            }else if(currentX==0 && currentY== patientMapSize.getRight()){
                // (0, limit): S or E
                directionArr = new Directions[2];
                directionArr[0] = Directions.SOUTH;
                directionArr[1] = Directions.EAST;

                return directionArr[rd.nextInt(2)];
            }else if(currentX== patientMapSize.getLeft() && currentY==0){
                // (limit, 0): N or W
                directionArr = new Directions[2];
                directionArr[0] = Directions.NORTH;
                directionArr[1] = Directions.WEST;

                return directionArr[rd.nextInt(2)];
            }else{
                if(currentX==0){
                    // (0, 1~limit-1): S, N, or E
                    directionArr = new Directions[3];
                    directionArr[0] = Directions.SOUTH;
                    directionArr[1] = Directions.NORTH;
                    directionArr[2] = Directions.EAST;

                    return directionArr[rd.nextInt(3)];
                }else{
                    // (1~limit-1, 0): N, W, or E
                    directionArr = new Directions[3];
                    directionArr[0] = Directions.NORTH;
                    directionArr[1] = Directions.WEST;
                    directionArr[2] = Directions.EAST;

                    return directionArr[rd.nextInt(3)];
                }
            }
        }else{
            if(currentX== patientMapSize.getLeft() && currentY== patientMapSize.getRight()){
                // (limit, limit): S or W
                directionArr = new Directions[2];
                directionArr[0] = Directions.SOUTH;
                directionArr[1] = Directions.WEST;

                return directionArr[rd.nextInt(2)];
            }else{
                if(currentX== patientMapSize.getLeft()){
                    // (limit, 1~limit-1): N, S, or W
                    directionArr = new Directions[3];
                    directionArr[0] = Directions.NORTH;
                    directionArr[1] = Directions.SOUTH;
                    directionArr[2] = Directions.WEST;

                    return directionArr[rd.nextInt(3)];
                }else{
                    // (1~limit-1, limit): S, W or E
                    directionArr = new Directions[3];
                    directionArr[0] = Directions.SOUTH;
                    directionArr[1] = Directions.WEST;
                    directionArr[2] = Directions.EAST;

                    return directionArr[rd.nextInt(3)];
                }
            }
        }
    }

    public boolean checkPatient(ArrayList<Integer> spotPatientList){
        if(!spotPatientList.isEmpty())
            return true;
        else
            return false;
    }
}
