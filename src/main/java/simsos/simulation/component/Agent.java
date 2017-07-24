package simsos.simulation.component;

import simsos.scenario.mci.Policy;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by mgjin on 2017-06-21.
 *
 * Edited by Youlim Jung on 2017-07-22.
 */
public abstract class Agent {
    protected World world = null;

    public Agent(World world) {
        this.world = world;
    }

    public abstract Action step();
    public abstract void reset();
    public abstract int getId();
    public abstract String getName();

    public abstract void messageIn(Message msg);

    public abstract HashMap<String, Object> getProperties();

    public abstract void injectPolicies(ArrayList<Policy> policies);
}
