package simsos.simulation.component;

import simsos.scenario.mci.Patient;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 *
 * Created by mgjin on 2017-06-21.
 *
 * Edited by Youlim Jung on 2017-10-11.
 *
 */
public abstract class World {
    protected ArrayList<Agent> agents = new ArrayList<Agent>();
    protected ArrayList<Message> messageQueue = new ArrayList<Message>();

    protected int time = 0;

    public ArrayList<Agent> getAgents() {
        return agents;
    }

    public void addAgent(Agent agent) {
        agents.add(agent);
    }

    public void reset() {
        for (Agent agent : this.agents)
            agent.reset();
        this.messageQueue.clear();

        this.time = 0;
    }

    public void progress(int time) {
        this.time += time;
    }

    public int getTime() {
        return this.time;
    }

    public Snapshot getCurrentSnapshot(ArrayList<Patient> patientsList) {
        // Environment - Property - Value
        // Agent1 - Property - Value
        // Agent2 - Property - Value

        Snapshot snapshot = new Snapshot();

        LinkedHashMap<String, Object> worldProperties = new LinkedHashMap<String, Object>();
        worldProperties.put("InitCasualty", patientsList.size());
        worldProperties.put("Patients", patientsList);
        snapshot.addProperties(null, worldProperties);

        for (Agent agent : agents)
            snapshot.addProperties(agent, agent.getProperties());

        return snapshot;
    }

    public void messageOut(Message msg) {
        this.messageQueue.add(msg);
    }

    public ArrayList<Action> getMessageQueue() {
        ArrayList<Action> msgQueue = new ArrayList<Action>();
        msgQueue.addAll(this.messageQueue);
        this.messageQueue.clear();

        return msgQueue;
    }
    public abstract ArrayList<Action> generateExogenousActions();
}
