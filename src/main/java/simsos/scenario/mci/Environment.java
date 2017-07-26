package simsos.scenario.mci;

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

    private int totalCasualty;
    private int MCILevel;
    private int mciRadius;
    private int damageFire;
    private int damageCollapse;

    public static ArrayList<Integer>[][] patientMap;
    public static ArrayList<Integer>[] stageZone;
    public static ArrayList<Integer>[][] hospitalMap;
    public static ArrayList<Patient> patientsList;

    // backup info
    private int bckCasualty;
    private int bckMCILevel;
    private int bckDamageFire;
    private int bckDamageCollapse;

    public Environment(int totalCasualty, int damageFire, int damageCollapse, int radius) {
        // initialize variables
        this.totalCasualty = totalCasualty;
        this.MCILevel = calcMCILevel();
        this.mciRadius = radius;
        this.damageFire = damageFire;
        this.damageCollapse = damageCollapse;

        // initialize maps
        setMapSize();
        patientMap = new ArrayList[radius+1][radius+1];
        initMap(patientMap);
        stageZone = new ArrayList[radius+1];
        initStageZone();
        hospitalMap = new ArrayList[radius+1][radius+1];
        initMap(hospitalMap);

        // generate patients
        patientsList = new ArrayList<>();
        PatientFactory patientFactory = new PatientFactory(totalCasualty);
        patientFactory.generatePatient(patientMap, patientsList);

        // backup info to reset
        backupEnvironment();

    }

    private void setMapSize(){
        worldMapSize = new Pair<Integer, Integer>(mciRadius+10, mciRadius+10);
        patientMapSize = new Pair<Integer, Integer>(mciRadius, mciRadius);
        hospitalMapSize = new Pair<Integer, Integer>(mciRadius, mciRadius);
    }

    private int calcMCILevel(){
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

    public void updatePatientsList(){
        for(Patient patient : patientsList){
            patient.updateStrength();
        }
    }

    public void backupEnvironment(){
        //TODO backup 없애고 initial value (CONSTANT?로 쓸 수 있게 수정하자)
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


}
