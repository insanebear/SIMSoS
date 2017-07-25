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
    private int occupGeneral;
    private int occupIntensive;
    private int occupOperating;

    private Action treatment;

    public Hospital(World world, int hospitalId, String name, ArrayList<Policy> mciPolicies,
                    int general, int intensive, int operating) {
        super(world);
        this.name = name;
        this.hospitalId = hospitalId;
        this.totGeneral = general;
        this.totIntensive = intensive;
        this.totOperating = operating;

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
}
