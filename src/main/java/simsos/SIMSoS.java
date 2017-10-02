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
import simsos.simulation.component.Scenario;
import simsos.simulation.component.World;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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

        // JSON Read
        /*
            SoS properties - from SoSProperties.json
            SoS policy elements (policy element pool) - from PolicyElement.json
            SoS previous policies - from previousPolicy.json
         */

        ArrayList<Policy> prevPolicies;
        ArrayList<PolicyElement> policyElementsPool;
        SoSInfrastructure infrastructure;

        SimpleModule module = new SimpleModule();
        ObjectMapper mapper = new ObjectMapper();

        module.addDeserializer(Policy.class, new PolicyDeserializer(mapper));
        module.addDeserializer(PolicyElement.class, new PolicyElementDeserializer(mapper));
        mapper.registerModule(module);

        CollectionType policyCollectionType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, Policy.class);
        CollectionType policyElementCollectionType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, PolicyElement.class);

        prevPolicies
                = mapper.readValue(new File("src/main/json/policies/previousPolicy.json"), policyCollectionType);
        policyElementsPool
                = mapper.readValue(new File("src/main/json/policies/policyElements.json"), policyElementCollectionType);
        infrastructure = mapper.readValue(
                new File("src/main/json/scenario/SoSProperties.json"), SoSInfrastructure.class);

        // SBPS Module (Search-based Policy Suggestion Module)
        PolicySuggestion policySuggestion = new PolicySuggestion(prevPolicies, policyElementsPool);
//        policySuggestion.geneticAlgorithm();
        policySuggestion.simulatePolicy(infrastructure);


    }

    void printCheck(ArrayList<PolicyElement> policyElementsPool){
        for(PolicyElement pe : policyElementsPool){
            System.out.println(pe.getVarName());
            System.out.println(pe.getScope());
            System.out.println(pe.getType());
            if(pe.getType().equals("range")){
                System.out.println("Minimum: "+pe.getRange()[0]);
                System.out.println("Maximum: "+pe.getRange()[1]);
                System.out.println();
            }else{
                for(String s: pe.getValues())
                    System.out.println(s);
                System.out.println();
            }
        }


        // set policy id
        // id 매길 필요가 있나 -_-..
//        for (int i=0; i<prevPolicies.size(); i++) {
//            prevPolicies.get(i).setPolicyId(i);
//        }
//            //CHECK
//            for(Policy p : rescuePolicies){
//                System.out.println("Policy Id: "+p.getPolicyId()+" Policy Type: "+p.getPolicyType());
//                System.out.println("Policy Conditions:");
//                p.printConditions();
//                System.out.println("Policy Action:");
//                p.printAction();
//            }
    }

}
