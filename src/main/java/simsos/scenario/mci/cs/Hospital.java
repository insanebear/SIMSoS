package simsos.scenario.mci.cs;

import simsos.scenario.mci.Location;
import simsos.scenario.mci.Patient;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.ArrayList;
import java.util.HashMap;

import static simsos.scenario.mci.Environment.patientsList;

/**
 * Created by mgjin on 2017-06-28.
 *
 * Edited by Youlim Jung on 2017-07-25.
 */
public class Hospital extends Agent {
    private enum Status {WAITING, TREATING}
    private String name;
    private int hospitalId;
    private Location location;

    private double conformRate; // indicates how much CS will follow policies

    private int totGeneral;
    private int totIntensive;
    private int totOperating;

    private ArrayList<Integer> generalList;
    private ArrayList<Integer> intensiveList;
    private ArrayList<Integer> operatingList;

    private int needSurguryGeneral;
    private int needSurguryIntensive;

    private int availGeneral;
    private int availIntensive;
    private int availOperating;


    private Status status;
    private Action treatment;



    public Hospital(World world, int hospitalId, String name,
                    int general, int intensive, int operating, Location location, double conformRate) {
        super(world);
        this.name = name;
        this.hospitalId = hospitalId;
        this.totGeneral = general;
        this.totIntensive = intensive;
        this.totOperating = operating;

        this.availGeneral = this.totGeneral;
        this.availIntensive = this.totIntensive;
        this.availOperating = this.totOperating;

        this.location = location;

        this.conformRate = conformRate;

        generalList = new ArrayList<>();
        intensiveList = new ArrayList<>();
        operatingList = new ArrayList<>();

        this.status = Status.WAITING;

        System.out.println(getName()+" "+getId()+" is at "+location.getX()+", "+location.getY());

        this.reset();
    }

    @Override
    public Action step() {
        switch(status){
            case WAITING:
                return new Action(1) {
                    @Override
                    public void execute() {
                        if(generalList.size()>0 || intensiveList.size()>0 ||operatingList.size()>0){
                            status = Status.TREATING;
                        }
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
                        operatePatient();
                        movePatient();
                        calcNeedSurgeryPatients();
                        if(generalList.isEmpty() && intensiveList.isEmpty() && operatingList.isEmpty())
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
        return false;
    }

    @Override
    public HashMap<String, Object> getProperties() {
        return new HashMap<String, Object>();
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

    public ArrayList<Integer> getGeneralList() {
        return generalList;
    }

    public ArrayList<Integer> getIntensiveList() {
        return intensiveList;
    }

    public ArrayList<Integer> getOperatingList() {
        return operatingList;
    }

    public Location getLocation() {
        return location;
    }

    public void setPatient(String roomType, int patientId){
        switch (roomType){
            case "General":
                generalList.add(patientId);
            case "Intensive":
                intensiveList.add(patientId);
            case "Operating":
                operatingList.add(patientId);
        }
    }

    private void operatePatient(){
        for(Integer pId : operatingList){
            Patient p = patientsList.get(pId);

            if(p.isSurgeried()){
                p.changeStat(); // RECOVERY
                if(p.getStrength()<=80){
                    intensiveList.add(pId);
                    operatingList.remove(pId);
                }else{
                    generalList.add(pId);
                    operatingList.remove(pId);
                }
                break;
            }
            p.changeStat(); // SURGERY
            p.doSurgery();

        }
    }

    private void movePatient(){
        // check intensive care room
        ArrayList<Integer> removeList = new ArrayList<>();

        if(!intensiveList.isEmpty()){
            for(Integer pId : intensiveList){
                if(patientsList.get(pId).getStatus() == Patient.Status.DEAD){
                    removeList.add(pId); // list up on removeList
                    availIntensive++;
                }
                if(patientsList.get(pId).getStrength()>=80) {
                    generalList.add(pId);
                    availGeneral--;
                    removeList.add(pId);// list up on removeList
                    availIntensive++;
                }
            }
        }

        intensiveList.removeAll(removeList); // remove patient from intensiveList
        removeList.clear();

    // check general room
        if(!generalList.isEmpty()){
            for(Integer pId : generalList){
                if(patientsList.get(pId).getStatus() == Patient.Status.DEAD){
                    removeList.add(pId); // list up on removeList
                    availGeneral++;
                }
                if(patientsList.get(pId).getStrength()>=170) {
                    patientsList.get(pId).changeStat(); // CURED
                    removeList.add(pId); // list up on removeList
                    availGeneral++;
                }
            }
        }
        generalList.removeAll(removeList);
        removeList.clear();
    }

    public void reserveRoom(String roomType){
        switch (roomType){
            case "General":
                availGeneral--;
            case "Intensive":
                availIntensive--;
        }
    }

    private void calcNeedSurgeryPatients(){
        int tempGeneral=0, tempIntensive=0;

        for(Integer pId : generalList){
            Patient p = patientsList.get(pId);
            if(!p.isSurgeried())
                tempGeneral++;
        }
        for(Integer pId : intensiveList){
            Patient p = patientsList.get(pId);
            if(!p.isSurgeried())
                tempIntensive++;
        }

        needSurguryGeneral = tempGeneral;
        needSurguryIntensive = tempIntensive;
    }

    public int getNeedSurguryGeneral() {
        return needSurguryGeneral;
    }

    public int getNeedSurguryIntensive() {
        return needSurguryIntensive;
    }

    public int getAvailGeneral() {
        return availGeneral;
    }

    public int getAvailIntensive() {
        return availIntensive;
    }

    public int getAvailOperating() {
        return availOperating;
    }
}