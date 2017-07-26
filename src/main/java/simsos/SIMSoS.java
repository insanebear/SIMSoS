package simsos;

import mci.Main;
import simsos.scenario.mci.Environment;
import simsos.scenario.mci.MCIScenario;
import simsos.scenario.mci.Patient;
import simsos.scenario.mci.Policy;
import simsos.simulation.Simulator;
import simsos.simulation.component.Scenario;
import simsos.simulation.component.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by mgjin on 2017-06-12.
 */
public class SIMSoS {
    public static void main(String[] args) throws IOException {
        if (args.length > 0 && args[0].equals("old")) {
            String[] passedArgs = Arrays.copyOfRange(args, 1, args.length);
            try {
                Main.experimentMain(passedArgs);
            } catch (IOException e) {
                System.out.println("Error: Old version is not runnable");
                e.printStackTrace();
            } finally {
                System.exit(0);
            }
        }

        ArrayList<Policy> mciPolicies = new ArrayList<>(); // change this into external input
        Scenario scenario = new MCIScenario(mciPolicies);
        World world = scenario.getWorld();

        Simulator.execute(world, 100);

        for(Patient p: Environment.patientsList){
            System.out.println("Patient "+p.getPatientId()+" "+p.getStatus());
        }
    }
}
