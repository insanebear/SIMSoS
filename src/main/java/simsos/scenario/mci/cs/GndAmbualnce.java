package simsos.scenario.mci.cs;

import simsos.scenario.mci.Policy;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * Created by Youlim Jung on 22/07/2017.
 *
 */
public class GndAmbualnce extends Agent{
    private int gAmbId;
    private boolean loadFlag;

    private enum Status {WAITING, BACK_TO_SCENE, TRANSFERRING}

    public GndAmbualnce(World world) {
        super(world);
    }

    @Override
    public Action step() {
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void messageIn(Message msg) {

    }

    @Override
    public HashMap<String, Object> getProperties() {
        return null;
    }

    @Override
    public void injectPolicies(ArrayList<Policy> policies) {

    } // Ground Ambulance
}
