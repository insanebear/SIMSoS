package simsos.scenario.mci.cs;

import simsos.scenario.mci.Location;
import simsos.scenario.mci.Policy;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by mgjin on 2017-06-28.
 *
 * Edited by Youlim Jung on 2017-07-25.
 */
public class Hospital extends Agent {
    private String name;
    private int hospitalId;
    private Location location;
    private int totGeneral;
    private int totIntensive;
    private int totOperating;

    private ArrayList<Integer> generalList;
    private ArrayList<Integer> intensiveList;
    private ArrayList<Integer> operatingList;

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

        System.out.println(getName()+" "+getId()+" is at "+location.getX()+", "+location.getY());

        this.reset();
    }

    @Override
    public Action step() {

        return Action.getNullAction(1, this.getName() + ": No treatment");
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
}
