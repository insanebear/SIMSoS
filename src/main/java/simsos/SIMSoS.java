package simsos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;
import mci.Main;
import simsos.scenario.SoSInfrastructure;
import simsos.scenario.mci.Environment;
import simsos.scenario.mci.MCIScenario;
import simsos.scenario.mci.Patient;
import simsos.scenario.mci.policy.*;
import simsos.simulation.Simulator;
import simsos.simulation.component.PropertyValue;
import simsos.simulation.component.Scenario;
import simsos.simulation.component.Snapshot;
import simsos.simulation.component.World;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static simsos.scenario.mci.Environment.patientsList;

/**
 * Created by mgjin on 2017-06-12.
 *
 * Edited by Youlim Jung on 2017-08-05.
 */
public class SIMSoS {
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
        mapper.registerModule(module);

        CollectionType pCollectionType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, Policy.class);
        // CHECK
//        Environment.policies = mapper.readValue(new File("src/main/json/policies/previousPolicy.json"), pCollectionType);
//        infrastructure = mapper.readValue(new File("src/main/json/scenario/SoSProperties.json"), SoSInfrastructure.class);
        Environment.policies = mapper.readValue(new File(args[0]), pCollectionType);
        infrastructure = mapper.readValue(new File("./json/SoSProperties.json"), SoSInfrastructure.class);

        Scenario scenario = new MCIScenario(infrastructure);
        World world = scenario.getWorld();
        ArrayList<Double> simResults = new ArrayList<>();
//        printAllPatientStatus();

        // NOTE simulation repeat 50-time
        for(int nSimulation = 0; nSimulation<50; nSimulation++){
            Simulator.execute(world, 100);
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
            if(p.getStatus() == Patient.Status.CURED
                    || p.getStatus() == Patient.Status.RECOVERY)
                alivePatient++;
        }
        System.out.println("Total casualties: "+ totalCasualties);
        System.out.println("Alive patient: "+ alivePatient);
        System.out.println("Dead patient: "+ deadPatient);

        double average = (double)alivePatient/totalCasualties*100;

//        printAllPatientStatus();
        return Math.round(average*100d) / 100d;
    }

    public static double calcMeanAchievement(ArrayList<Double> simResults){
        double sumResult = 0.0;
        for(Double res : simResults)
            sumResult+=res;
        double average = sumResult / simResults.size();
        System.out.println("Average achievement of 50 simulations: "+ average);
        return Math.round(average*100d) / 100d;
    }

}
