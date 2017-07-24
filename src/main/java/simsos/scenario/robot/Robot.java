package simsos.scenario.robot;

import simsos.scenario.mci.Policy;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

/**
 * Created by mgjin on 2017-06-21.
 */
public class Robot extends Agent{

    private String name;
    private int id;

    public int xpos;
    public boolean token;
    private boolean immediateStep;
    private Action move;

    public Robot(World world, String name) {
        super(world);

        this.name = name;
        this.reset();
    }

    @Override
    public Action step() {
        Action next = this.move;

        if (this.immediateStep) {
            if (xpos == 10) { // Immediate action 1
                next = new Action(0) {

                    @Override
                    public void execute() {
                        token = true;
                    }

                    @Override
                    public String getName() {
                        return "Grab";
                    }
                };
                this.immediateStep = false;
            } else if (xpos > 10) { // Immediate action 2
                int drop = new Random().nextInt(100);
                if (drop == 0) {
                    next = new Action(0) {

                        @Override
                        public void execute() {
                            token = false;
                        }

                        @Override
                        public String getName() {
                            return "Drop";
                        }
                    };
                    this.immediateStep = false;
                } else {
                    // Normal Step
                }
            }
        } else {
            this.immediateStep = true;
        }

        return next;
    }

    @Override
    public void reset() {
        this.xpos = 10;
        this.token = true;
        this.immediateStep = true;
        this.move = new Action(1) {

            @Override
            public void execute() {
                xpos++;
            }

            @Override
            public String getName() {
                return "Move Right";
            }
        };
    }

    @Override
    public int getId() {
        return this.id;
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
        LinkedHashMap<String, Object> agentProperties = new LinkedHashMap<String, Object>();
        agentProperties.put("xpos", xpos);
        agentProperties.put("token", token);

        return agentProperties;
    }

    @Override
    public void injectPolicies(ArrayList<Policy> policies) {

    }
}
