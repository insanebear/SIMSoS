package simsos.scenario.mci;

import simsos.simulation.component.Action;
import simsos.simulation.component.World;

import java.util.ArrayList;

/**
 * Created by mgjin on 2017-06-28.
 *
 * Edited by Youlim Jung on 2017-07-18.
 */
public class MCIWorld extends World {
//    public static final Pair<Integer, Integer> MAP_SIZE = new Pair<Integer, Integer>(19, 19);

    private int[] patientRaisePlan;
    private int patientNumbering = 0;

    private Environment environment;

    public MCIWorld(int totalCasualty, int damageFire, int damageCollapse, int mciRadius) {
        environment = new Environment(totalCasualty, damageFire, damageCollapse, mciRadius);
    }

    @Override
    public void reset() {
        super.reset();
        environment.resetEnvironment();
        //TODO implement reset method in MCIworld
        // 이거 리셋 용도가 어디까지 리셋하는거지 ㅋㅋ
    }

    @Override
    public ArrayList<Action> generateExogenousActions() {
        ArrayList<Action> updateEnvironment = new ArrayList<>();
//        World world = this;

        updateEnvironment.add(new Action(0) {

            @Override
            public void execute() {
                // update patients' strength
                environment.updatePatientsList();
            }

            @Override
            public String getName() {
                return "World: Update environment";
            }
        });

        return updateEnvironment;
    }


}
