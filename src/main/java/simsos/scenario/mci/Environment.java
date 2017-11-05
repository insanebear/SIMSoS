package simsos.scenario.mci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;
import simsos.scenario.mci.policy.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * Created by Youlim Jung on 20/07/2017.
 *
 */
public class Environment {
    public static Pair<Integer, Integer> worldMapSize;
    public static Pair<Integer, Integer> patientMapSize;
    public static Pair<Integer, Integer> hospitalMapSize;

    private int initCasualty;
    private int initDamageFire;
    private int initDamageCollapse;
    private int buildingHeight;
    private int mciRadius;

    private static int totalCasualty;
    private static int MCILevel;
    public static int damageFire;
    public static int damageCollapse;

    public static ArrayList<Patient> patientsList;

    public static ArrayList<Floor> building;
    public static ArrayList<Integer>[] stageZone;
    public static ArrayList<Integer>[][] hospitalMap;

    // Policy
    public static ArrayList<Policy> policies = new ArrayList<>();

    // initialize environment
    public void initEnvironment(){
        totalCasualty = this.initCasualty;
        MCILevel = calcMCILevel(totalCasualty);
        damageFire = this.initDamageFire;
        damageCollapse = this.initDamageCollapse;

        setMapSize();
        building = new ArrayList<>();                           initBuilding(building);
        stageZone = new ArrayList[mciRadius+1];                 initStageZone();
        hospitalMap = new ArrayList[mciRadius+1][mciRadius+1];    initMap(hospitalMap);

        // generate patients
        patientsList = new ArrayList<>();
        PatientFactory patientFactory = new PatientFactory(totalCasualty);
        patientFactory.generatePatient(patientsList, building, mciRadius);
    }

    private void initStageZone(){
        for(int i=0; i<stageZone.length; i++)
            stageZone[i] = new ArrayList<>();
    }

    private void initMap(ArrayList<Integer>[][] map){
        for(int i=0; i<map.length; i++)
            for(int j=0 ; j<map.length; j++)
                map[i][j] = new ArrayList<>();
    }

    private void initBuilding(ArrayList<Floor> building){
        for(int i=0; i<buildingHeight; i++){
            building.add(new Floor(mciRadius));
        }
    }

    private void setMapSize(){
        worldMapSize = new Pair<Integer, Integer>(mciRadius+10, mciRadius+10);
        patientMapSize = new Pair<Integer, Integer>(mciRadius, mciRadius);
        hospitalMapSize = new Pair<Integer, Integer>(mciRadius, mciRadius);
    }

    // reset environment
    public void resetEnvironment(){
        totalCasualty = initCasualty;
        MCILevel = calcMCILevel(totalCasualty);
        damageFire = initDamageFire;
        damageCollapse = initDamageCollapse;
    }

    // update environment
    void updatePatientsList(){
        // update patients' strength according to their status
        for(Patient patient : patientsList){
            patient.updateStrength();
        }
    }

    public static void updateCasualty(){
        // In this context, casualties mean "not-staged patient"
        totalCasualty--;
        MCILevel = calcMCILevel(totalCasualty);
    }

    public void rearrangeStageZone(){
        // exclude dead patients in stageZone
        for(int i=0; i<stageZone.length; i++){
            ArrayList<Integer> stageSpot = stageZone[i];
            for(int idx=0; i<stageSpot.size(); i++){
                int patientId = stageSpot.get(idx);
                Patient patient = patientsList.get(patientId);
                if(patient.isDead())
                    stageSpot.remove(idx);
            }
        }
    }

    // manipulate methods
    private static int calcMCILevel(int totalCasualty){
        if(totalCasualty<10)
            return 1;
        else if(totalCasualty>=10 && totalCasualty<20)
            return 2;
        else if(totalCasualty>=20 && totalCasualty<100)
            return 3;
        else if(totalCasualty>=100 && totalCasualty<1000)
            return 4;
        else
            return 5;
    }

    // TODO conflict handling
    // check policy
    public static Policy checkActionPolicy(String role, String actionName, CallBack callBack){
        //TODO Policy Conflict Handling (with assumption)
        /* "activePolicy" ArrayList stores valid policies that satisfy current condition.*/
        ArrayList<Policy> activePolicies;
        switch (role){
            // Find an appropriate policy on current action name and role.
            case "RESCUE":
                activePolicies = evalPolicyCond("Action",policies, callBack);
                for(Policy p : activePolicies){
                    if(p.getRole().equals("RESCUE") && p.getAction().getActionName().equals(actionName))
                        return p;
                }
            case "TRANSPORT":
                activePolicies = evalPolicyCond("Action", policies, callBack);
                for(Policy p : activePolicies){
                    if(p.getRole().equals("TRANSPORT") && p.getAction().getActionName().equals(actionName))
                        return p;
                }
                break;
            case "TREATMENT":
                activePolicies = evalPolicyCond("Action", policies, callBack);
                for(Policy p : activePolicies){
                    if(p.getRole().equals("TREATMENT") && p.getAction().getActionName().equals(actionName))
                        return p;
                }
                break;
        }
        return null;
    }

    public static ArrayList<Policy> checkCompliancePolicy(String role){
        //TODO Policy Conflict Handling (with assumption)
        /* "activePolicy" ArrayList stores valid policies that satisfy current condition.*/
        ArrayList<Policy> activePolicies = evalPolicyCond(policies);
        ArrayList<Policy> activeByRole = new ArrayList<>();
        switch (role){
            case "RESCUE":

                for(Policy p : activePolicies){
                    if(p.getRole().equals("RESCUE"))
                        activeByRole.add(p);
                }
            case "TRANSPORT":
                for(Policy p : activePolicies){
                    if(p.getRole().equals("TRANSPORT"))
                        activeByRole.add(p);
                }
                break;
            case "TREATMENT":
                for(Policy p : activePolicies){
                    if(p.getRole().equals("TREATMENT"))
                        activeByRole.add(p);
                }
                break;
        }
        return activeByRole;
    }

    private static ArrayList<Policy> evalPolicyCond(String policyType, ArrayList<Policy> policies, CallBack callBack){
        //NOTE 1. Check condition comparing with environment.
        //NOTE 2. Condition contains CS variables. (compares not only common environment but CS environment using callback)
        ArrayList<Policy> activePolicies = new ArrayList<>();
        ArrayList<Policy> candidPolicies = new ArrayList<>();
        boolean isValid = true;

        for(Policy policy : policies){
            if(policy.getPolicyType().equals("Action"))
                candidPolicies.add(policy);
            else if(policy.getPolicyType().equals("Compliance"))
                candidPolicies.add(policy);
        }
        for(Policy policy : candidPolicies){
            ArrayList<Condition> conditions = policy.getConditions();
            for(Condition condition : conditions){
                if(condition.getVariable().equals("MCILevel")){
                    String operator = condition.getOperator();
                    int value = Integer.parseInt(condition.getValue());
//                    int value = Integer.parseInt(condition.getValue().get(0));
                    isValid = compareValueByOp(MCILevel, value, operator);
                }else if(condition.getVariable().equals("DamageType")){
                    String damageType = condition.getValue();
                    if((damageType.equals("Fire") && damageFire>0)
                            || (damageType.equals("Collapse") && damageCollapse>0))
                        isValid = true;
                    else
                        isValid = false;
                    if(!isValid)
                        break;
//                    ArrayList<String> damageTypes = condition.getValue();
//                    for(String damageType : damageTypes){
//                        if((damageType.equals("Fire") && damageFire>0)
//                                || (damageType.equals("Collapse") && damageCollapse>0))
//                            isValid = true;
//                        else
//                            isValid = false;
//                        if(!isValid)
//                            break;
//                    }


                }else if(condition.getVariable().equals("Story")){
                    int value = Integer.parseInt(condition.getValue());
//                    int value = Integer.parseInt(condition.getValue().get(0));
                    if(callBack.checkCSStat("Story", value))
                        isValid = true;
                }else if(condition.getVariable().equals("Time")){
                    int value = Integer.parseInt(condition.getValue());
//                    int value = Integer.parseInt(condition.getValue().get(0));
                    if(callBack.checkCSStat("Time", value))
                        isValid = true;
                }
                if(!isValid)
                    break;
            }
            if(isValid)
                activePolicies.add(policy);
        }
        return activePolicies;
    }

    private static ArrayList<Policy> evalPolicyCond(ArrayList<Policy> policies){
        //NOTE condition 부분과 environment가 맞는지 확인.
        ArrayList<Policy> activePolicies = new ArrayList<>();
        ArrayList<Policy> candidPolicies = new ArrayList<>();
        boolean isValid = true;

        for(Policy policy : policies){
            if(policy.getPolicyType().equals("Action"))
                candidPolicies.add(policy);
            else if(policy.getPolicyType().equals("Compliance"))
                candidPolicies.add(policy);
        }
        for(Policy p : candidPolicies){
            ArrayList<Condition> conditions = p.getConditions();
            for(Condition condition : conditions){
                if(condition.getVariable().equals("MCILevel")){
                    String operator = condition.getOperator();
                    int value = Integer.parseInt(condition.getValue());
//                    int value = Integer.parseInt(condition.getValue().get(0));
                    isValid = compareValueByOp(MCILevel, value, operator);
                }else if(condition.getVariable().equals("DamageType")) {
                    String damageType = condition.getValue();

                    if ((damageType.equals("Fire") && damageFire > 0)
                            || (damageType.equals("Collapse") && damageCollapse > 0))
                        isValid = true;
                    else
                        isValid = false;
                    if (!isValid)
                        break;
//                    ArrayList<String> damageTypes = condition.getValue();
//                    for (String damageType : damageTypes) {
//                        if ((damageType.equals("Fire") && damageFire > 0)
//                                || (damageType.equals("Collapse") && damageCollapse > 0))
//                            isValid = true;
//                        else
//                            isValid = false;
//                        if (!isValid)
//                            break;
//                    }
                }
                if(!isValid)
                    break;
            }
            if(isValid)
                activePolicies.add(p);
        }
        return activePolicies;
    }

    private static boolean compareValueByOp(int envValue, int policyValue, String operator){
        switch(operator) {
            case ">":
            case ">=":
                if (envValue >= policyValue)
                    return true;
                break;
            case "<":
            case "<=":
                if (envValue <= policyValue)
                    return true;
                break;
            case "==":
                if (envValue == policyValue)
                    return true;
                break;
            case "!=":
                if (envValue != policyValue)
                    return true;
                break;
        }
        return false;
    }

    // getters and setters
    public int getInitCasualty() {
        return initCasualty;
    }

    public void setInitCasualty(int initCasualty) {
        this.initCasualty = initCasualty;
    }

    public int getInitDamageFire() {
        return initDamageFire;
    }

    public void setInitDamageFire(int initDamageFire) {
        this.initDamageFire = initDamageFire;
    }

    public int getInitDamageCollapse() {
        return initDamageCollapse;
    }

    public void setInitDamageCollapse(int initDamageCollapse) {
        this.initDamageCollapse = initDamageCollapse;
    }

    public int getBuildingHeight() {
        return buildingHeight;
    }

    public void setBuildingHeight(int buildingHeight) {
        this.buildingHeight = buildingHeight;
    }

    public int getMciRadius() {
        return mciRadius;
    }

    public void setMciRadius(int mciRadius) {
        this.mciRadius = mciRadius;
    }

    public static int getTotalCasualty() {
        return totalCasualty;
    }

    public static void setTotalCasualty(int totalCasualty) {
        Environment.totalCasualty = totalCasualty;
    }

    public static int getMCILevel() {
        return MCILevel;
    }

    public static void setMCILevel(int MCILevel) {
        Environment.MCILevel = MCILevel;
    }
}
