package simsos.scenario;

import simsos.scenario.mci.Location;
import simsos.scenario.mci.cs.Information;

import java.util.ArrayList;

/**
 * Created by Youlim Jung on 18/08/2017.
 */
public class SoSInfrastructure {

    //NOTE Compliance descriptions based on mean values
    private String typeSoS;
    private double minCompliance;
    private double maxCompliance;
    private double rescueCompliance; // indicates how much CS will follow policies
    private double transportCompliance;
    private double treatmentCompliance;
    private double enforceRate;

    private int totFireFighters;
    private int totGndAmbulances;
    private int totHospital;

    private int crew;
    private int general;
    private int intensive;
    private int operating;

    private ArrayList<Location> hospitalLocations;
    private ArrayList<Information> csInformation;

    public String getTypeSoS() {
        return typeSoS;
    }

    public void setTypeSoS(String typeSoS) {
        this.typeSoS = typeSoS;
    }

    public double getMinCompliance() {
        return minCompliance;
    }

    public void setMinCompliance(double minCompliance) {
        this.minCompliance = minCompliance;
    }

    public double getMaxCompliance() {
        return maxCompliance;
    }

    public void setMaxCompliance(double maxCompliance) {
        this.maxCompliance = maxCompliance;
    }

    public double getRescueCompliance() {
        return rescueCompliance;
    }

    public void setRescueCompliance(double rescueCompliance) {
        this.rescueCompliance = rescueCompliance;
    }

    public double getTransportCompliance() {
        return transportCompliance;
    }

    public void setTransportCompliance(double transportCompliance) {
        this.transportCompliance = transportCompliance;
    }

    public double getTreatmentCompliance() {
        return treatmentCompliance;
    }

    public void setTreatmentCompliance(double treatmentCompliance) {
        this.treatmentCompliance = treatmentCompliance;
    }

    public SoSInfrastructure() {
        this.csInformation = new ArrayList<>();
    }

    public int getTotFireFighters() {
        return totFireFighters;
    }

    public void setTotFireFighters(int totFireFighters) {
        this.totFireFighters = totFireFighters;
    }

    public int getNumPTSCenter() {
        return totGndAmbulances;
    }

    public void setNumPTSCenter(int numPTSCenter) {
        this.totGndAmbulances = numPTSCenter;
    }

    public int getTotHospital() {
        return totHospital;
    }

    public void setTotHospital(int totHospital) {
        this.totHospital = totHospital;
    }

    public int getCrew() {
        return crew;
    }

    public void setCrew(int crew) {
        this.crew = crew;
    }

    public int getGeneral() {
        return general;
    }

    public void setGeneral(int general) {
        this.general = general;
    }

    public int getIntensive() {
        return intensive;
    }

    public void setIntensive(int intensive) {
        this.intensive = intensive;
    }

    public int getOperating() {
        return operating;
    }

    public void setOperating(int operating) {
        this.operating = operating;
    }

    public void putCsInformation(Information information){
        this.csInformation.add(information);
    }

    public double getEnforceRate() {
        return enforceRate;
    }

    public void setEnforceRate(double enforceRate) {
        this.enforceRate = enforceRate;
    }

    public ArrayList<Location> getHospitalLocations() {
        return hospitalLocations;
    }

    public void setHospitalLocations(ArrayList<Location> hospitalLocations) {
        this.hospitalLocations = hospitalLocations;
    }
}
