package simsos.scenario.mci.cs;

import simsos.scenario.mci.policy.Policy;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * Created by Youlim Jung on 18/07/2017.
 *
 */
public class FireDepartment extends Agent {
    private String name;
    private int fireDeptId;
    private int allocFighters;

    private double compliance; // indicates how much CS will follow policies
    private boolean enforced;

    private FireFighter[] workFighterList;

    public FireDepartment(World world, int fireDeptId, String name, double compliance, boolean enforced) {
        super(world);
        this.name = name;
        this.fireDeptId = fireDeptId;
        this.compliance = compliance;
        this.enforced = enforced;
        this.reset();

        this.allocFighters = 50;    //NOTE 지금은 고정

        workFighterList = new FireFighter[allocFighters];
    }

    @Override
    public Action step() {
        return Action.getNullAction(0,"FireDepartment action-_-");
    }

    @Override
    public void reset() {

    }

    @Override
    public int getId() {
        return this.fireDeptId;
    }

    @Override
    public String getName() {
        return "Fire department";
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

    public int getAllocFighters() {
        return allocFighters;
    }

    public void setWorkFighterList(int idx, FireFighter fighter) {
        this.workFighterList[idx] = fighter;
    }
}
