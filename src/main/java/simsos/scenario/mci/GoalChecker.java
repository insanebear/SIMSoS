package simsos.scenario.mci;

import simsos.propcheck.pattern.ExistenceChecker;
import simsos.simulation.component.Snapshot;

/**
 *
 * Created by Youlim Jung on 31/07/2017.
 *
 */

public class GoalChecker extends ExistenceChecker {

    @Override
    protected boolean evaluate(Snapshot snapshot) { // Not much meaning in this simulation...
        boolean result = true;

        return result;
    }

    public double evaluateSimulation(Snapshot snapshot){
        double result = 0;

        return result;
    }


}
