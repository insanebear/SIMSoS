package simsos.scenario.mci.cs;

import simsos.scenario.mci.Location;
import simsos.scenario.mci.Patient;
import simsos.scenario.mci.Policy;
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
    private int totGeneral;
    private int totIntensive;
    private int totOperating;

    private ArrayList<Integer> generalList;
    private ArrayList<Integer> intensiveList;
    private ArrayList<Integer> operatingList;

    private Status status;
    private Action treatment;



    public Hospital(World world, int hospitalId, String name, ArrayList<Policy> mciPolicies,
                    int general, int intensive, int operating, Location location) {
        super(world);
        this.name = name;
        this.hospitalId = hospitalId;
        this.totGeneral = general;
        this.totIntensive = intensive;
        this.totOperating = operating;

        this.location = location;

        generalList = new ArrayList<>();
        intensiveList = new ArrayList<>();
        operatingList = new ArrayList<>();

        this.status = Status.WAITING;

        System.out.println(getName()+" "+getId()+" is at "+location.getX()+", "+location.getY());

        this.reset();
    }

    @Override
    public Action step() {

        if(status == Status.WAITING)
            return Action.getNullAction(1, this.getName() + ": No patient");
        else
            return new Action(1) {
                @Override
                public void execute() {
                    operatePatient();
                    movePatient();

                    if(isEmpty(generalList) && isEmpty(intensiveList) && isEmpty(operatingList))
                        status = Status.WAITING;
                }

                @Override
                public String getName() {
                    return this.getName()+""+getId()+" is working on.";
                }
            };

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
    public HashMap<String, Object> getProperties() {
        return new HashMap<String, Object>();
    }

    @Override
    public void injectPolicies(ArrayList<Policy> policies) {

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
        for(Integer pId:operatingList){
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
        for(Integer pId : intensiveList){
            if(patientsList.get(pId).getStrength()>=80) {
                generalList.add(pId);
                intensiveList.remove(pId);
            }
        }

        for(Integer pId : generalList){
            if(patientsList.get(pId).getStrength()>=170)
                patientsList.get(pId).changeStat(); // CURED
                generalList.remove(pId);
        }

    }

    private boolean isEmpty(ArrayList<Integer> list){
        if (list.size() == 0) return true;
        else return false;
    }
}