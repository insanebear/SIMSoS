package simsos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;
import mci.Main;
import simsos.scenario.SoSInfrastructure;
import simsos.scenario.mci.Environment;
import simsos.scenario.InfraDeserializer;
import simsos.scenario.mci.MCIScenario;
import simsos.scenario.mci.Patient;
import simsos.scenario.mci.policy.*;
import simsos.simulation.Simulator;
import simsos.simulation.component.Scenario;
import simsos.simulation.component.World;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static simsos.scenario.mci.Environment.patientsList;
import static simsos.scenario.mci.Environment.policies;

/**
 * Created by mgjin on 2017-06-12.
 *
 * Edited by Youlim Jung on 2017-08-05.
 */
public class SIMSoS {
    public final static int nSimuation = 5000;
    public static void main(String[] args) throws IOException {
        // Old version of SIMSoS
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

        // SIMVA-SoS for MCI Policy Simulator
        SoSInfrastructure infrastructure;

        SimpleModule module = new SimpleModule();
        ObjectMapper mapper = new ObjectMapper();

        module.addDeserializer(Policy.class, new PolicyDeserializer(mapper));
        module.addDeserializer(SoSInfrastructure.class, new InfraDeserializer(mapper));
        mapper.registerModule(module);

        CollectionType pCollectionType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, Policy.class);
        // Read policy from an input parameter
        Environment.policies = mapper.readValue(new File(args[0]), pCollectionType);
        for(Policy p : policies)
            p.printPolicy();
        // Read SoS properties for simulation
        infrastructure = mapper.readValue(new File("./json/SoSProperties.json"), SoSInfrastructure.class);


        ArrayList<Double> simResults = new ArrayList<>();

        // NOTE simulation repeat 50-time
        for(int nSim = 0; nSim<nSimuation; nSim++){
            Scenario scenario = new MCIScenario(infrastructure);
            World world = scenario.getWorld();
            Simulator.execute(world, 1000);
            simResults.add(calcGoalAchievement());
        }
        double totalResult = calcMeanAchievement(simResults);
        System.out.println(totalResult);
        String achievement = Double.toString(totalResult);
        BufferedWriter out = new BufferedWriter(new FileWriter("Sim_Result.txt"));
        out.write(achievement);
        out.close();
    }

    public static void printAllPatientStatus(){
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("|| PatientID    |  Dead     | Strength  | Severity  | Hospital    | Operate   | Room Name | Stay Time | Last Status | Released  ||");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------");
        for (Patient patient : patientsList)
            patient.printPatientStatus();
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------");
    }

    public static double calcGoalAchievement(){
        int totalCasualties = patientsList.size();
        int alivePatient = 0;
        int deadPatient = 0;

        for(Patient p: patientsList){
            if(p.getStatus() == Patient.Status.DEAD)
                deadPatient++;
//            if(p.getStatus() == Patient.Status.CURED)
            if(p.getRoomName().equals("") &&
                    (p.getStatus() == Patient.Status.RESCUED)) {
//            if(p.getStatus() == Patient.Status.RESCUED || p.getStatus() == Patient.Status.TRANSPORT_WAIT) // Rescue policy effect
                // if(p.getStatus() == Patient.Status.Loaded || p.getStatus() == Patient.Status.TRANSPORT_WAIT) // Transport policy effect
                alivePatient++;
            }
        }
        System.out.println("Total casualties: "+ totalCasualties);
        System.out.println("Alive patient: "+ alivePatient);
        System.out.println("Dead patient: "+ deadPatient);

        double average = (double)alivePatient/totalCasualties*100;
        System.out.println("Saved percentage: "+average);
        printAllPatientStatus();
        return Math.round(average*100d) / 100d;
    }

    public static double calcMeanAchievement(ArrayList<Double> simResults){
        double sumResult = 0.0;
        System.out.println("Achievements");
        for(Double res : simResults) {
            System.out.print(res+", ");
            sumResult += res;
        }
        System.out.println();
        double average = sumResult / simResults.size();
        System.out.println("Average achievement of 50 simulations: "+ average);
        return Math.round(average*100d) / 100d;
    }

}
