package simsos.scenario.mci;

/**
 *
 * Created by Youlim Jung on 17/07/2017.
 *
 */
public class Patient {
    private enum Status {
        RESCUE_WAIT, RESCUED, TRANSFER_WAIT, TRANSFERRING,
        SURGERY_WAIT, SURGERY, RECOVERY, CURED, DEAD
    }

    public enum InjuryType {
        FRACTURED, BURN, BLEEDING
    }

    private int patientId;
    private int strength;       // 0~199 (200) 0: dead
    private int severity;       // 0~9 (10)
    private InjuryType injuryType;
    private Location location;      // within MCI radius
    private Status status = Status.RESCUE_WAIT;

    public static final int FRAC_DEC_RATE = 2;
    public static final int BURN_DEC_RATE = 4;
    public static final int BLEED_DEC_RATE = 6;


    public Patient(int patientId, int strength, InjuryType injuryType, Location location) {
        this.patientId = patientId;
        this.strength = strength;
        this.severity = calcSeverity();
        this.injuryType = injuryType;
        this.location = location;
    }

    public int calcSeverity() {
        int resSeve;

        if (strength >= 1 && strength < 20)
            resSeve = 9;
        else if (strength >= 20 && strength < 40)
            resSeve = 8;
        else if (strength >= 40 && strength < 60)
            resSeve = 7;
        else if (strength >= 60 && strength < 80)
            resSeve = 6;
        else if (strength >= 80 && strength < 100)
            resSeve = 5;
        else if (strength >= 100 && strength < 120)
            resSeve = 4;
        else if (strength >= 120 && strength < 140)
            resSeve = 3;
        else if (strength >= 140 && strength < 160)
            resSeve = 2;
        else if (strength >= 160 && strength < 180)
            resSeve = 1;
        else
            resSeve = 0;

        return resSeve;
    }

    public void updateStrength(){
        switch (status) {
            // decrease strength
            case RESCUE_WAIT:
            case RESCUED:
            case TRANSFER_WAIT:
                if (injuryType == InjuryType.FRACTURED)
                    strength -= FRAC_DEC_RATE;
                else if (injuryType == InjuryType.BURN)
                    strength -= BURN_DEC_RATE;
                else
                    strength -= BLEED_DEC_RATE;
                break;
            case TRANSFERRING:
            case SURGERY_WAIT:
                int firstAid = 1;
                if (injuryType == InjuryType.FRACTURED)
                    strength -= (FRAC_DEC_RATE - firstAid);
                else if (injuryType == InjuryType.BURN)
                    strength -= (BURN_DEC_RATE - firstAid);
                else
                    strength -= (BLEED_DEC_RATE - firstAid);
                break;

            // increase strength
            case SURGERY:
                int surgeryCured = 20;
                strength += surgeryCured;
                break;
            case RECOVERY:
                int recoveryRate = 5;
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
                    status = Status.TRANSFER_WAIT;
                    break;
                case TRANSFER_WAIT:
                    status = Status.TRANSFERRING;
                    break;
                case TRANSFERRING:
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
}
