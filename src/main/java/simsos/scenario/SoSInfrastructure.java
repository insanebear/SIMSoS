package simsos.scenario;

import simsos.scenario.mci.cs.Information;

import java.util.ArrayList;

/**
 * Created by Youlim Jung on 18/08/2017.
 */
public class SoSInfrastructure {

    //TODO role-compliance mapping?
    //NOTE Compliance descriptions based on mean values
    private double rescueCompliance; // indicates how much CS will follow policies
    private double transportCompliance;
    private double treatmentCompliance;
    private boolean enforced=false;

    private int numFireDepartment;
    private int numPTSCenter;
    private int numHospital;

    private int meanCrew;
    private int varCrew;
    private int meanGeneral;
    private int varGeneral;
    private int meanIntensive;
    private int varIntensive;
    private int meanOperating;
    private int varOperating;

    private ArrayList<Information> csInformation;

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

    public boolean isEnforced() {
        return enforced;
    }

    public void setEnforced(boolean enforced) {
        this.enforced = enforced;
    }

    public SoSInfrastructure() {
        this.csInformation = new ArrayList<>();
    }

    public int getNumFireDepartment() {
        return numFireDepartment;
    }

    public void setNumFireDepartment(int numFireDepartment) {
        this.numFireDepartment = numFireDepartment;
    }

    public int getNumPTSCenter() {
        return numPTSCenter;
    }

    public void setNumPTSCenter(int numPTSCenter) {
        this.numPTSCenter = numPTSCenter;
    }

    public int getNumHospital() {
        return numHospital;
    }

    public void setNumHospital(int numHospital) {
        this.numHospital = numHospital;
    }


    public int getMeanCrew() {
        return meanCrew;
    }

    public void setMeanCrew(int meanCrew) {
        this.meanCrew = meanCrew;
    }

    public int getVarCrew() {
        return varCrew;
    }

    public void setVarCrew(int varCrew) {
        this.varCrew = varCrew;
    }

    public int getMeanGeneral() {
        return meanGeneral;
    }

    public void setMeanGeneral(int meanGeneral) {
        this.meanGeneral = meanGeneral;
    }

    public int getVarGeneral() {
        return varGeneral;
    }

    public void setVarGeneral(int varGeneral) {
        this.varGeneral = varGeneral;
    }

    public int getMeanIntensive() {
        return meanIntensive;
    }

    public void setMeanIntensive(int meanIntensive) {
        this.meanIntensive = meanIntensive;
    }

    public int getVarIntensive() {
        return varIntensive;
    }

    public void setVarIntensive(int varIntensive) {
        this.varIntensive = varIntensive;
    }

    public int getMeanOperating() {
        return meanOperating;
    }

    public void setMeanOperating(int meanOperating) {
        this.meanOperating = meanOperating;
    }

    public int getVarOperating() {
        return varOperating;
    }

    public void setVarOperating(int varOperating) {
        this.varOperating = varOperating;
    }

    public void putCsInformation(Information information){
        this.csInformation.add(information);
    }

}
