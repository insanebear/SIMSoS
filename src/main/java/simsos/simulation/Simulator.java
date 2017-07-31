package simsos.simulation;

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

import static simsos.scenario.mci.Environment.stageZone;

/**
 * Created by mgjin on 2017-06-21.
 *
 * Edited by Youlim Jung on 2017-07-26.
 */
public class Simulator {
    public static ArrayList<Snapshot> execute(World world, int endOfTime) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter("Simul_result.txt"));    //
        ArrayList<Snapshot> simulationLog = new ArrayList<Snapshot>();

        boolean stoppingCondition = false;
        ArrayList<Action> actions = new ArrayList();
        ArrayList<Action> immediateActions = new ArrayList();

        world.reset();
        simulationLog.add(world.getCurrentSnapshot()); // Initial snapshot

        while (!stoppingCondition) {
            System.out.println();
            System.out.println();
            System.out.println("World Time: " + world.getTime());

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
                progress(out, immediateActions);
            } while (immediateActions.size() > 0);

            Collections.shuffle(actions);

            ArrayList<Action> exoActions = world.generateExogenousActions();
            actions.addAll(exoActions);

            progress(out, actions);

            ArrayList<Action> msgActions = world.getMessageQueue();
            progress(out, msgActions);

            world.progress(1);
            simulationLog.add(world.getCurrentSnapshot());
            // Verdict - evaluateProperties();
            if (world.getTime() >= endOfTime)
                stoppingCondition = true;

            System.out.println("STAGE ZONE STATUS: "+Arrays.toString(stageZone));
        }

        return simulationLog;
    }

    private static void progress(BufferedWriter out, ArrayList<Action> actions) throws IOException {
        for (Action action : actions) {
            String actionSpec = action.getName();
            if(!actionSpec.equals("PTS Waiting")){
                out.write(actionSpec);  //
                out.newLine();  //
                System.out.println(actionSpec);
            }
            action.execute();
        }
    }
}
