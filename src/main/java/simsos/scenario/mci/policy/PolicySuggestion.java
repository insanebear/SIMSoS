package simsos.scenario.mci.policy;

import simsos.scenario.SoSInfrastructure;
import simsos.scenario.mci.Environment;
import simsos.scenario.mci.MCIScenario;
import simsos.scenario.mci.Patient;
import simsos.simulation.Simulator;
import simsos.simulation.component.Scenario;
import simsos.simulation.component.World;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Youlim Jung on 18/09/2017.
 */
public class PolicySuggestion {

    ArrayList<Policy> prevPolicies;
    ArrayList<PolicyElement> policyElementsPool;

    public PolicySuggestion(ArrayList<PolicyElement> policyElementsPool) { // make policy from the scratch
        this.policyElementsPool = policyElementsPool;
    }

    public PolicySuggestion(ArrayList<Policy> prevPolicies, ArrayList<PolicyElement> policyElementsPool) {
        this.prevPolicies = prevPolicies;
        this.policyElementsPool = policyElementsPool;
    }

    public void geneticAlgorithm(){

        // while( number != endCondition )

            // make initial pop

            // evaluate fitness
                // call Simulator for evaluation

            // update the best solution

        // return the best solution
    }

    public void simulatePolicy(SoSInfrastructure infrastructure) throws IOException {
        Scenario scenario = new MCIScenario(infrastructure);
        World world = scenario.getWorld();

        // NOTE simulation repeat
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
