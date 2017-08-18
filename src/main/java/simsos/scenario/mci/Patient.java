package simsos.scenario.mci;

import java.util.Random;

/**
 *
 * Created by Youlim Jung on 17/07/2017.
 *
 */
public class Patient {
    public enum Status {
        RESCUE_WAIT, RESCUED, TRANSPORT_WAIT, LOADED, TRANSPORTING,
        SURGERY_WAIT, SURGERY, RECOVERY, CURED, DEAD
    }

    public enum InjuryType {
        FRACTURED, BURN, BLEEDING
    }

    private int patientId;
    private int strength;       // 0~199 (200) 0: dead
    private int severity;       // 0~9 (10)
    private InjuryType injuryType;
    private int story; // Todo: randomly generated
    private Location location;      // within MCI radius
    private Status status = Status.RESCUE_WAIT;
    public final int FRAC_DEC_RATE = 4;
    public final int BURN_DEC_RATE = 6;
    public final int BLEED_DEC_RATE = 8;

    private boolean isSurgeried;

    public Patient(int patientId, int strength, InjuryType injuryType, Location location) {
        this.patientId = patientId;
        this.strength = strength;
        this.severity = calcSeverity();
        this.injuryType = injuryType;
        this.location = location;
        this.isSurgeried = false;
    }

    public int calcSeverity() {
        if (strength >= 1 && strength < 20)
            return 9;
        else if (strength >= 20 && strength < 40)
            return 8;
        else if (strength >= 40 && strength < 60)
            return 7;
        else if (strength >= 60 && strength < 80)
            return 6;
        else if (strength >= 80 && strength < 100)
            return 5;
        else if (strength >= 100 && strength < 120)
            return 4;
        else if (strength >= 120 && strength < 140)
            return 3;
        else if (strength >= 140 && strength < 160)
            return 2;
        else if (strength >= 160 && strength < 180)
            return 1;
        else
            return 0;
    }

    public void updateStrength(){
        switch (status) {
            // decrease strength
            case RESCUE_WAIT:
            case RESCUED:
            case TRANSPORT_WAIT:
                if (injuryType == InjuryType.FRACTURED)
                    strength -= FRAC_DEC_RATE;
                else if (injuryType == InjuryType.BURN)
                    strength -= BURN_DEC_RATE;
                else
                    strength -= BLEED_DEC_RATE;
                break;
            case LOADED:
            case TRANSPORTING:
                Random rd = new Random();
                if(rd.nextBoolean()){
                    int firstAid = 1;
                    if (injuryType == InjuryType.FRACTURED)
                        strength -= (FRAC_DEC_RATE - firstAid);
                    else if (injuryType == InjuryType.BURN)
                        strength -= (BURN_DEC_RATE - firstAid);
                    else
                        strength -= (BLEED_DEC_RATE - firstAid);
                }
                break;
            case SURGERY_WAIT:
                rd = new Random();
                int treatment;

                if(rd.nextBoolean())
                    treatment=1;
                else
                    treatment=3;

                if (injuryType == InjuryType.FRACTURED)
                    strength -= (FRAC_DEC_RATE - treatment);
                else if (injuryType == InjuryType.BURN)
                    strength -= (BURN_DEC_RATE - treatment);
                else
                    strength -= (BLEED_DEC_RATE - treatment);

                break;

            // increase strength
            case SURGERY:
                int surgeryCured = 60;
                strength += surgeryCured;
                break;
            case RECOVERY:
                int recoveryRate = 8;
                strength += recoveryRate;
                break;
        }

        if(strength == 0 || strength >= 180)
            changeStat();

        calcSeverity();
    }

    public void changeStat() {
        if (strength == 0) {
            status = Status.DEAD;
        }else if(strength >= 180){
            status = Status.CURED;
        }else{
            switch (status) {
                case RESCUE_WAIT:
                    status = Status.RESCUED;
                    break;
                case RESCUED:
                    status = Status.TRANSPORT_WAIT;
                    break;
                case TRANSPORT_WAIT:
                    status = Status.LOADED;
                    break;
                case LOADED:
                    status = Status.TRANSPORTING;
                    break;
                case TRANSPORTING:
                    status = Status.SURGERY_WAIT;
                    break;
                case SURGERY_WAIT:
                    status = Status.SURGERY;
                    break;
                case SURGERY:
                    status = Status.RECOVERY;
                    break;
                case RECOVERY:
                    status = Status.CURED;
                    break;
            }
        }
    }

    public InjuryType getInjuryType() {
        return injuryType;
    }

    public int getPatientId() {
        return patientId;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void doSurgery() {
        this.isSurgeried = true;
    }

    public boolean isSurgeried() {
        return isSurgeried;
    }

    public int strengthDecreasingRate(){
        switch (injuryType){
            case BURN:
                return BURN_DEC_RATE;
            case BLEEDING:
                return BLEED_DEC_RATE;
            case FRACTURED:
                return FRAC_DEC_RATE;
        }
        return 0;
    }
}
