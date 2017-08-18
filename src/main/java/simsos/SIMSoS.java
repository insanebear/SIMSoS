package simsos;

import mci.Main;
import simsos.scenario.mci.Environment;
import simsos.scenario.mci.MCIScenario;
import simsos.scenario.mci.Patient;
import simsos.simulation.Simulator;
import simsos.simulation.component.Scenario;
import simsos.simulation.component.World;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by mgjin on 2017-06-12.
 *
 * Edited by Youlim Jung on 2017-08-05.
 */
public class SIMSoS {
    public static void main(String[] args) throws IOException {
        // NOTE How about using JSON file input here to set up MCI scenario's conformRate?

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
        Scenario scenario = new MCIScenario();
        World world = scenario.getWorld();

        Simulator.execute(world, 100);

        // temporary result view
        int alivePatient = 0;
        int deadPatient = 0;

        for(Patient p: Environment.patientsList){
            if(p.getStatus() == Patient.Status.DEAD)
                deadPatient++;
            if(p.getStatus() == Patient.Status.CURED)
                alivePatient++;
            System.out.println("Patient "+p.getPatientId()+" "+p.getStatus());
        }
        System.out.println("Alived patient: "+ alivePatient);
        System.out.println("Dead patient: "+ deadPatient);
    }
}
