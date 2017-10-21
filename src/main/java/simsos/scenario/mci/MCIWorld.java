package simsos.scenario.mci;

import com.fasterxml.jackson.databind.ObjectMapper;
import simsos.simulation.component.Action;
import simsos.simulation.component.World;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by mgjin on 2017-06-28.
 *
 * Edited by Youlim Jung on 2017-07-18.
 */
public class MCIWorld extends World {
    private Environment environment;

    public MCIWorld() throws IOException {
        setEnvironment();
    }
    @Override
    public void reset() {
        super.reset();
        environment.resetEnvironment();
    }

    @Override
    public ArrayList<Action> generateExogenousActions() {
        ArrayList<Action> updateEnvironment = new ArrayList<>();

        updateEnvironment.add(new Action(0) {
            @Override
            public void execute() {
                // update patients' strength
                environment.updatePatientsList();
//                environment.rearrangeStageZone();
            }

            @Override
            public String getName() {
                return "World: Update environment";
            }
        });

        return updateEnvironment;
    }

    private void setEnvironment(){
        ObjectMapper mapper = new ObjectMapper();
        try {
            environment = mapper.readValue(new File("src/main/json/scenario/envProperties.json"), Environment.class);
            environment.initEnvironment();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
