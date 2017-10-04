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
//    public static ArrayList<Integer>[][] patientMap;
    public static ArrayList<Integer>[] stageZone;
    public static ArrayList<Integer>[][] hospitalMap;

    // TODO Move these (policy related parts) to SoS Infrastructure in the future
    // Policy
    public static ArrayList<Policy> policies = new ArrayList<>();
    public static ArrayList<Policy> transportPolicies = new ArrayList<>();
    public static ArrayList<Policy> treatmentPolicies = new ArrayList<>();
    public static ArrayList<Policy> evacuatePolicies = new ArrayList<>();

    // initialize environment
    public void initEnvironment(){
        totalCasualty = this.initCasualty;
        MCILevel = calcMCILevel(totalCasualty);
        damageFire = this.initDamageFire;
        damageCollapse = this.initDamageCollapse;

        setMapSize();
        building = new ArrayList<>();                           initBuilding(building);
//        patientMap = new ArrayList[mciRadius+1][mciRadius+1];     initMap(patientMap);
        //
        stageZone = new ArrayList[mciRadius+1];                 initStageZone();
        hospitalMap = new ArrayList[mciRadius+1][mciRadius+1];    initMap(hospitalMap);



        // generate patients
        patientsList = new ArrayList<>();
        PatientFactory patientFactory = new PatientFactory(totalCasualty);
        patientFactory.generatePatient(patientsList, building, mciRadius);

        getPolicies();
    }

    private void initStageZone(){
        for(int i=0; i<stageZone.length; i++){
            stageZone[i] = new ArrayList<>();
        }
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
        //NOTE patient는 리셋을 해야되나.?;;
    }

    //update environment
    void updatePatientsList(){
        for(Patient patient : patientsList){
            patient.updateStrength();
        }
    }

    //TODO update patient location (self-escaping)

    public static void updateCasualty(){
        // In this context, casualties mean "not-staged patient"
        totalCasualty--;
        MCILevel = calcMCILevel(totalCasualty);
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



    // Policy related methods
    private void getPolicies() {
        SimpleModule module = new SimpleModule();
        ObjectMapper mapper = new ObjectMapper();
//        JavaType type = mapper.getTypeFactory().constructCollectionType(ArrayList.class, Condition.class);

        module.addDeserializer(Policy.class, new PolicyDeserializer(mapper));


        mapper.registerModule(module);
        CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, Policy.class);

        try {
            policies = mapper.readValue(new File("src/main/json/policies/previousPolicy.json"), collectionType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Policy checkPolicy(String currPolicyType){
        // TODO policy하나만 리턴하는거 바꿔야할듯.
        ArrayList<Policy> activePolicies;
        switch (currPolicyType){
            case "RESCUE":
                activePolicies = evalPolicyCond(policies);
                for(Policy p : activePolicies){
                    if(p.getPolicyType().equals("RESCUE"))
                        return p;
                }
            case "TRANSPORT":
                break;
            case "TREATMENT":
                break;
        }
        return null;
    }

    private static ArrayList<Policy> evalPolicyCond(ArrayList<Policy> policies){
        //NOTE condition 부분과 environment가 맞는지 확인.
        ArrayList<Policy> activePolicies = new ArrayList<>();
        boolean isValid = true;

        for(Policy p : policies){
            ArrayList<Condition> conditions = p.getConditions();
            for(Condition condition : conditions){
                if(condition.getVariable().equals("MCILevel")){
                    String operator = condition.getOperator();
                    int value = Integer.parseInt(condition.getValue());
                    isValid = compareValueByOp(MCILevel, value, operator);
                }else if(condition.getVariable().equals("DamageType")){
                    String damageType = condition.getValue();
                    if((damageType.equals("Fire") && damageFire>0)
                            || (damageType.equals("Collapse") && damageCollapse>0))
                        isValid = true;
                    else
                        isValid = false;
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



    // getters and setters for environment information
    public int getInitCasualty() {
        return this.initCasualty;
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

    public int getMciRadius() {
        return mciRadius;
    }

    public void setMciRadius(int mciRadius) {
        this.mciRadius = mciRadius;
    }

    public int getBuildingHeight() {
        return buildingHeight;
    }

    public void setBuildingHeight(int buildingHeight) {
        this.buildingHeight = buildingHeight;
    }
}
