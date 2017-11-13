package simsos.simulation;

import simsos.scenario.mci.Patient;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Snapshot;
import simsos.simulation.component.World;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static simsos.SIMSoS.printAllPatientStatus;
import static simsos.scenario.mci.Environment.patientsList;
import static simsos.scenario.mci.Environment.stageZone;

/**
 * Created by mgjin on 2017-06-21.
 *
 * Edited by Youlim Jung on 2017-07-26.
 */
public class Simulator {
    public static ArrayList<Snapshot> execute(World world, int endOfTime) throws IOException {
        ArrayList<Snapshot> simulationLog = new ArrayList<Snapshot>();

        boolean stoppingCondition = false;
        ArrayList<Action> actions = new ArrayList<>();
        ArrayList<Action> immediateActions = new ArrayList<>();
        // Env reset..

        world.reset();
        simulationLog.add(world.getCurrentSnapshot(patientsList)); // Initial snapshot
//        printAllPatientStatus();
        while (!stoppingCondition) {
            if (world.getTime() == 0)
                System.out.println(">> Simulation Start.");
//            System.out.println("World Time: " + world.getTime());
            do {
                immediateActions.clear();
                actions.clear();
                for (Agent agent : world.getAgents()) {
                    Action action = agent.step();
                    if (action.isImmediate()) {
                        immediateActions.add(action);
                    } else {
                        actions.add(action);
                    }
                }
                Collections.shuffle(immediateActions);
                progress(immediateActions);
            } while (immediateActions.size() > 0);

            Collections.shuffle(actions);

            ArrayList<Action> exoActions = world.generateExogenousActions();
            actions.addAll(exoActions);

            progress(actions);

            ArrayList<Action> msgActions = world.getMessageQueue();
            progress(msgActions);
            world.progress(1);

            // Snapshot
//            simulationLog.add(world.getCurrentSnapshot(patientsList));
            // Verdict - evaluateProperties();
            if (world.getTime() >= endOfTime) {
                stoppingCondition = true;
                System.out.println(">> Simulation End.");
            }
        }
        // Last snapshot only
        simulationLog.add(world.getCurrentSnapshot(patientsList));

        return simulationLog;
    }

    private static void progress(BufferedWriter out, ArrayList<Action> actions) throws IOException {
        for (Action action : actions) {
            String actionSpec = action.getName();
            if(!actionSpec.equals("PTS Waiting") || !actionSpec.equals("Hospital waiting")){
                out.write(actionSpec);  //
                out.newLine();  //
//                System.out.println(actionSpec);
            }
            action.execute();
        }
    }

    private static void progress(ArrayList<Action> actions) throws IOException {
        for (Action action : actions) {
//            String actionSpec = action.getName();
//            if(!actionSpec.equals("PTS Waiting") || !actionSpec.equals("Hospital waiting")){
////                System.out.println(actionSpec);
//            }
            action.execute();
        }
    }
}
