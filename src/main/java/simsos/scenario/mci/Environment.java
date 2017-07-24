package simsos.scenario.mci;

import java.util.ArrayList;

/**
 *
 * Created by Youlim Jung on 20/07/2017.
 *
 */
public class Environment {
    // 맵 생성
    // 환경 설정
    // 환자 생성
    public static Pair<Integer, Integer> worldMapSize;
    public static Pair<Integer, Integer> mciMapSize;
    public static Pair<Integer, Integer> stageMapSize;
    public static Pair<Integer, Integer> hospitalMapSize;
    private int totalCasualty;
    private int MCILevel;
    private int mciRadius;
    private int damageFire;
    private int damageCollapse;

    public static ArrayList<Integer>[][] patientMCIMap;
    public static ArrayList<Integer>[] stageZone;
    public static ArrayList<Patient> patientsList;

    // backup info
    private int bckCasualty;
    private int bckMCILevel;
    private int bckDamageFire;
    private int bckDamageCollapse;

    public Environment(int totalCasualty, int damageFire, int damageCollapse, int mciRadius) {
        // initialize variables
        this.totalCasualty = totalCasualty;
        this.MCILevel = calcMCILevel();
        this.mciRadius = mciRadius;
        this.damageFire = damageFire;
        this.damageCollapse = damageCollapse;

        // initialize maps
        setMapSize();
        stageZone = new ArrayList[mciRadius];
        initStageZone();

        // generate patients
        patientsList = new ArrayList<>();
        PatientFactory patientFactory = new PatientFactory(totalCasualty, mciRadius);
        patientMCIMap = patientFactory.generatePatient(mciRadius, patientsList);

        // backup info to reset
        backupEnvironment();

    }
    private void setMapSize(){
        worldMapSize = new Pair<Integer, Integer>(mciRadius+10, mciRadius+10);
        mciMapSize = new Pair<Integer, Integer>(mciRadius, mciRadius);
    }

    public int calcMCILevel(){
        int resMCILevel=0;

        if(totalCasualty<10)
            resMCILevel = 1;
        else if(totalCasualty>=10 && totalCasualty<20)
            resMCILevel = 2;
        else if(totalCasualty>=20 && totalCasualty<100)
            resMCILevel = 3;
        else if(totalCasualty>=100 && totalCasualty<1000)
            resMCILevel = 4;
        else
            resMCILevel = 5;

        return resMCILevel;
    }

    public ArrayList[][] getPatientMCIMap() {
        return patientMCIMap;
    }

    public int getTotalCasualty() {
        return totalCasualty;
    }

    public void setTotalCasualty(int totalCasualty) {
        this.totalCasualty = totalCasualty;
    }

    public int getMCILevel() {
        return MCILevel;
    }

    public void setMCILevel(int MCILevel) {
        this.MCILevel = MCILevel;
    }

    public int getMciRadius() {
        return mciRadius;
    }

    public void setMciRadius(int mciRadius) {
        this.mciRadius = mciRadius;
    }

    public int getDamageFire() {
        return damageFire;
    }

    public void setDamageFire(int damageFire) {
        this.damageFire = damageFire;
    }

    public int getDamageCollapse() {
        return damageCollapse;
    }

    public void setDamageCollapse(int damageCollapse) {
        this.damageCollapse = damageCollapse;
    }

    public ArrayList<Patient> getPatientsList() {
        return patientsList;
    }

    public void setPatientsList(ArrayList<Patient> patientsList) {
        this.patientsList = patientsList;
    }


    public void updatePatientsList(){
        for(Patient patient : patientsList){
            patient.updateStrength();
        }
    }
    public void backupEnvironment(){
        bckCasualty = this.totalCasualty;
        bckMCILevel = this.MCILevel;
        bckDamageFire = this.damageFire;
        bckDamageCollapse = this.damageCollapse;
    }
    public void resetEnvironment(){
        this.totalCasualty = bckCasualty;
        this.MCILevel = bckMCILevel;
        this.damageFire = bckDamageFire;
        this.damageCollapse = bckDamageCollapse;
    }
    public void initStageZone(){
        for(int i=0; i<stageZone.length; i++){
            stageZone[i] = new ArrayList<>();
        }
    }

}
