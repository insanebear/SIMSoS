package simsos.scenario.mci.cs;

import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.HashMap;

/**
 * Created by mgjin on 2017-06-28.
 *
 * Edited by Youlim Jung on 2017-07-18.
 */
public class PTSCenter extends Agent {
    private String name;
    private int ptsCenterId;
    private int allocGndAmbul;

    private double conformRate; // indicates how much CS will follow policies

    private GndAmbulance[] workGndAmbuls;

    public PTSCenter(World world, int ptsCenterId, String name, double conformRate) {
        super(world);
        this.name = name;
        this.ptsCenterId = ptsCenterId;
        this.conformRate = conformRate;
        this.reset();

        this.allocGndAmbul = 20;     // 지금은 고정

        workGndAmbuls = new GndAmbulance[allocGndAmbul];
    }

    @Override
    public Action step() {
        return Action.getNullAction(1, this.getName() + ": Waiting");
    }

    @Override
    public void reset() {
    }

    @Override
    public int getId() {
        return this.ptsCenterId;
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

    public int getAllocGndAmbul() {
        return allocGndAmbul;
    }

    public void setWorkGndAmbuls(int idx, GndAmbulance gndAmbulance){
        this.workGndAmbuls[idx] = gndAmbulance;
    }
}