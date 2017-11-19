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

/**
 * Created by mgjin on 2017-06-12.
 *
 * Edited by Youlim Jung on 2017-08-05.
 */
public class SIMSoS {
//    public final static int nSimulation = 100;
//    public static final String ANSI_RED = "\u001B[31m";
//    public static final String ANSI_GREEN = "\u001B[32m";
//    public static final String ANSI_YELLOW = "\u001B[33m";
//    public static final String ANSI_RESET = "\u001B[0m";

    public static void main(String[] args) throws IOException {
        final int nSimulation = Integer.parseInt(args[1]);
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
//        for(Policy p : policies)
//            p.printPolicy();
        // Read SoS properties for simulation
        infrastructure = mapper.readValue(new File("./json/SoSProperties.json"), SoSInfrastructure.class);


        ArrayList<Double> simResults = new ArrayList<>();
        ArrayList<Integer> rescuedNumbers = new ArrayList<>();
        ArrayList<Integer> avgHoldupTimes = new ArrayList<>();
        ArrayList<Integer> dieB4RescueNumbers = new ArrayList<>();
        ArrayList<Integer> dieB4TransportedNumbers = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // NOTE simulation repeat 50-time
        for(int nSim = 0; nSim< nSimulation; nSim++){
            Scenario scenario = new MCIScenario(infrastructure);
            World world = scenario.getWorld();
            Simulator.execute(world, 100);
            simResults.add(calcGoalAchievement());
            rescuedNumbers.add(getProperties("Rescued"));
            avgHoldupTimes.add(getProperties("HoldupTime"));
            dieB4RescueNumbers.add(getProperties("DieB4Rescued"));
            dieB4TransportedNumbers.add(getProperties("DieB4Transported"));
        }

        long endTime = System.currentTimeMillis();

        // Patient Alive Rate
        int totalPatient = patientsList.size();
        double aliveRate = calcMeanAchievement(simResults);

        // Average
//        System.out.println(ANSI_YELLOW+"Detailed Results of "+args[0]+" policy"+ANSI_RESET);
        System.out.println("Detailed Results of "+args[0]+" policy");
        System.out.println();
        System.out.println();
        System.out.println("<Rescued number> : Total "+ totalPatient);
        int sumRescue = 0;
        for(Integer rescuedNum : rescuedNumbers){
            sumRescue += rescuedNum;
            System.out.print(rescuedNum+" ");
        }
        double avgRescue = sumRescue/ nSimulation;
        System.out.println("Rescued Average:"+avgRescue);

        // Rescue quality
        int sumAvgHoldupTime = 0;
        int sumDieNumber = 0;
        System.out.println("Patients' Holdup Time");
//        System.out.println(ANSI_GREEN+"Patients' Holdup Time"+ANSI_RESET);
        for(Integer holdupTime : avgHoldupTimes) {
            sumAvgHoldupTime += holdupTime;
            System.out.print(holdupTime + ", ");
        }
        System.out.println();
        System.out.println("Average of Holdup Time on whole simulation: "+ (double)sumAvgHoldupTime/ nSimulation);

        for(Integer dieNumber : dieB4RescueNumbers)
            sumDieNumber+=dieNumber;
        System.out.println("Average of dying number before rescued on whole simulation: "+ (double)sumDieNumber/ nSimulation);

        sumDieNumber=0;
        for(Integer dieNumber : dieB4TransportedNumbers)
            sumDieNumber+=dieNumber;
        System.out.println("Average of dying number before transported on whole simulation: "+ (double)sumDieNumber/ nSimulation);

        long simulationTime = endTime - startTime;
        System.out.println("Simulation Time: "+ simulationTime/1000+"s");


        String achievement = Double.toString(aliveRate);
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
        int alivePatient = 0, deadPatient = 0;


        for(Patient p: patientsList){
            if(p.getStatus() == Patient.Status.DEAD)
                deadPatient++;
            if(p.getStatus() == Patient.Status.CURED)
                alivePatient++;
        }
//        System.out.println(ANSI_GREEN+"Total casualties: "+ANSI_RESET+ totalCasualties);
        System.out.println();
        System.out.println("Total casualties: "+ totalCasualties);
        System.out.println("Alive patient: "+ alivePatient);
        System.out.println("Dead patient: "+ deadPatient);



        double average = (double)alivePatient/totalCasualties*100;
        System.out.println("Saved percentage: "+Math.round(average*100d) / 100d+"%");
//        printAllPatientStatus();
        return Math.round(average*100d) / 100d;
    }

    public static double calcMeanAchievement(ArrayList<Double> simResults){
        double sumResult = 0.0;
        System.out.println(">> "+simResults.size()+" of Simulations are ended.");
//        System.out.println(ANSI_RED+"<<<Simulation Ended>>>"+ANSI_RESET);
        System.out.println("<SoS Goal Achievements>");
        for(Double res : simResults) {
            System.out.print(res+"%, ");
            sumResult += res;
        }
        System.out.println();
        double average = sumResult / simResults.size();
        System.out.println();
        System.out.println("-- Average of SoS goal achievements in "+simResults.size()+" simulations: "+ Math.round(average*100d) / 100d+"%");
        return Math.round(average*100d) / 100d;
    }

    public static int getProperties(String property){
        int result = 0;
        int numRescued = 0, numTransported = 0, numOperated = 0;
        int dieAfterOp=0, notYetOp = 0, notYetOpDead=0;
        int dieBeforeTransported=0, dieBeforeRescued=0;
        int sumHoldupTime = 0;
        double avgHoldup = 0;

        for(Patient patient : patientsList){
            if(patient.isRescued()) {
                numRescued++;
                sumHoldupTime += patient.getHoldupTime();
            }
            if(patient.isTransported())
                numTransported++;
            if(patient.isOperated())
                numOperated++;

            if (!patient.isRescued() && patient.isDead())   // Never Touched
                dieBeforeRescued++;
            if (patient.isRescued() && !patient.isTransported() && patient.isDead()) // Die during transfer
                dieBeforeTransported++;
            if(!patient.isOperated() && patient.isTransported())    // Waiting patients
                notYetOp++;
            if(!patient.isOperated() && patient.isTransported() && patient.isDead())    // Die during waiting op
                notYetOpDead++;
            if (patient.isOperated() && patient.isDead())   // Surgery Fail
                dieAfterOp++;
        }


        switch (property){
            case "Rescued":
                result = numRescued;
                break;
            case "Transported":
                result = numTransported;
                break;
            case "Operated":
                result = numOperated;
                break;
            case "HoldupTime":
                if(sumHoldupTime != 0)
                    avgHoldup = sumHoldupTime/numRescued;
                return (int)avgHoldup;
            case "DieB4Rescued":
                return dieBeforeRescued;
            case "DieB4Transported":
                return dieBeforeTransported;
        }
        System.out.println();
        System.out.println("Rescued patients: "+numRescued);
        System.out.println("Transported patients: "+numTransported);
        System.out.println("Operated patients: "+numOperated);
        System.out.println("Not Yet Operated patients: "+notYetOp);
        System.out.println("Not Yet Operated Die patients: "+notYetOpDead);
        System.out.println("Die Before Rescued patients: "+dieBeforeRescued+" (Died in MCI scene.)");
        System.out.println("Die Before Transported patients: "+dieBeforeTransported+" (Rescued but died during waiting for transport or transporting.)");
        System.out.println("Die After Op patients: "+dieAfterOp+" (Accidentally died during surgery.)");
        System.out.println("------------------------------------------------------------------------------------------");
        return result;
    }

}
