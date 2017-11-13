package simsos.scenario.mci;

import java.util.Random;

/**
 *
 * Created by Youlim Jung on 17/07/2017.
 *
 */
public class Patient {
    public final static int TOT_STRENGTH = 499;

    public enum Status {
        RESCUE_WAIT, RESCUED, TRANSPORT_WAIT, LOADED, TRANSPORTING,
        SURGERY_WAIT, SURGERY, CURED, DEAD
    }

    public enum InjuryType {
        FRACTURED, BURN, BLEEDING
    }
    // basic information
    private int patientId;
    private int strength;       // 0~499 (500) 0: dead
    private int severity;       // 0~9 (10)
    private InjuryType injuryType;
    private boolean rescued;
    private boolean transported;
    private boolean operated;
    private boolean released;
    private boolean dead;

    // MCI zone information
    private int story;
    private Location location;      // within MCI radius
    private Status status;
    private int holdupTime;         // from the start to the rescued tim
    public final int FRAC_DEC_RATE = 0;
    public final int BURN_DEC_RATE = 3;
    public final int BLEED_DEC_RATE = 5;

    // Hospital information
    private int hospital;
    private String prevRoomName;
    private String roomName; // "General", "Intensive" (for knowing previous room)
    private boolean isTreated;
    private int treatPeriod;
    private int waitPeriod;

    private int operateTime;
    private int stayTime;


    public Patient(int patientId, int strength, InjuryType injuryType, int story, Location location) {
        this.patientId = patientId;
        this.strength = strength;
        this.severity = calcSeverity();
        this.injuryType = injuryType;
        this.rescued = false;
        this.transported = false;
        this.operated = false;
        this.released = false;
        this.dead = false;
        this.story = story;
        this.location = location;
        this.status = Status.RESCUE_WAIT;
        this.prevRoomName = "";
        this.roomName = "None";
        this.holdupTime = 0;
        this.isTreated = false;
        this.stayTime = 0;
        this.roomName = "";
        this.hospital = -1;
    }

    // Status
    public void changeStat() {
        if (strength <= 0) {
            this.status = Status.DEAD;
            this.dead = true;
        }else if(strength >= TOT_STRENGTH * 0.85){ // 170
            this.status = Status.CURED;
        }else{
            switch (status) {
                case RESCUE_WAIT:
                    rescued = true;
                    this.status = Status.RESCUED;
                    break;
                case RESCUED:
                    this.status = Status.TRANSPORT_WAIT;
                    break;
                case TRANSPORT_WAIT:
                    this.status = Status.LOADED;
                    break;
                case LOADED:
                    this.status = Status.TRANSPORTING;
                    break;
                case TRANSPORTING:
                    this.transported = true;
                    this.status = Status.SURGERY_WAIT;
                    break;
                case SURGERY_WAIT:
                    this.status = Status.SURGERY;
                    break;
                case SURGERY:
                    this.status = Status.CURED;
                    break;
            }
        }
    }

    // Strength and Severity
    public int calcSeverity() {
        if (strength >= 1 && strength < TOT_STRENGTH * 0.1)
            return 9;
        else if (strength >= TOT_STRENGTH * 0.1 && strength < TOT_STRENGTH * 0.2)
            return 8;
        else if (strength >= TOT_STRENGTH * 0.2 && strength < TOT_STRENGTH * 0.3)
            return 7;
        else if (strength >= TOT_STRENGTH * 0.3 && strength < TOT_STRENGTH * 0.4)
            return 6;
        else if (strength >= TOT_STRENGTH * 0.4 && strength < TOT_STRENGTH * 0.5)
            return 5;
        else if (strength >= TOT_STRENGTH * 0.5 && strength < TOT_STRENGTH * 0.6)
            return 4;
        else if (strength >= TOT_STRENGTH * 0.6 && strength < TOT_STRENGTH * 0.7)
            return 3;
        else if (strength >= TOT_STRENGTH * 0.7 && strength < TOT_STRENGTH * 0.8)
            return 2;
        else if (strength >= TOT_STRENGTH * 0.8 && strength < TOT_STRENGTH * 0.9)
            return 1;
        else if (strength >= TOT_STRENGTH * 0.9)
            return 0;
        else
            return -1;  // DEAD
    }

    public void recoverStrength(int strength) {
        if(this.strength+strength > TOT_STRENGTH)
            this.strength = TOT_STRENGTH;
        else
            this.strength += strength;
    }

    public void updateStrength(){
        Random rd = new Random();
        switch (this.status) {
            // decrease strength
            case RESCUE_WAIT:
            case RESCUED:
                if (injuryType == InjuryType.FRACTURED)
                    strength -= FRAC_DEC_RATE;
                else if (injuryType == InjuryType.BURN)
                    strength -= BURN_DEC_RATE;
                else
                    strength -= BLEED_DEC_RATE;
                break;
            case TRANSPORT_WAIT:
                if(rd.nextFloat() < 0.4){
                    if (injuryType == InjuryType.FRACTURED)
                        strength -= FRAC_DEC_RATE;
                    else if (injuryType == InjuryType.BURN)
                        strength -= BURN_DEC_RATE;
                    else
                        strength -= BLEED_DEC_RATE;
                } else
                    recoverStrength(3);     // thanks to paramedics
                break;
            case LOADED:
            case TRANSPORTING:
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
                boolean downStrength = new Random().nextBoolean();
                if(downStrength) {
                    if (injuryType == InjuryType.BURN)
                        strength -= BURN_DEC_RATE;
                    else
                        strength -= BLEED_DEC_RATE;
                }
                break;
        }
        if(this.strength<=0 || this.strength>=TOT_STRENGTH*0.85){
            changeStat();
            if(this.strength <0)
                this.strength = 0;
            else if (this.strength > TOT_STRENGTH)
                this.strength =TOT_STRENGTH;
        }
        this.severity = calcSeverity();
    }

    // Treat and wait time
    public int getTreatPeriod() {
        return treatPeriod;
    }

    public void setTreatPeriod(int treatPeriod) {
        this.treatPeriod = treatPeriod;
    }

    public int getWaitPeriod() {
        return waitPeriod;
    }

    public void decreaseWaitPeriod() { this.waitPeriod--; }

    public void resetWaitPeriod() {
        this.waitPeriod = getTreatPeriod();
    }

    // Operate time
    public int getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(int operateTime) {
        this.operateTime = operateTime;
    }

    public void decreaseOperateTime(){
        this.operateTime--;
    }

    public void increaseOperateTime() {
        this.operateTime++;
    }

    public int getStayTime() {
        return stayTime;
    }

    public void increaseStayTime() {
        this.stayTime++;
    }


    // Getters and Setters
    public int getPatientId() {
        return patientId;
    }

    public boolean isRescued() {
        return rescued;
    }

    public void setRescued(boolean rescued) {
        this.rescued = rescued;
    }

    public boolean isTransported() {
        return transported;
    }

    public void setTransported(boolean transported) {
        this.transported = transported;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public boolean isDead() {
        return dead;
    }

    public int getStrength() {
        return strength;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public InjuryType getInjuryType() {
        return injuryType;
    }

    public int getHoldupTime() {
        return holdupTime;
    }

    public void updateHoldupTime() {
        if(!isRescued())
            this.holdupTime++;
    }

    // Hospital information
    public int getStory() {
        return story;
    }

    public Location getLocation() {
        return location;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getPrevRoomName() {
        return prevRoomName;
    }

    public void setPrevRoomName(String prevRoomName) {
        this.prevRoomName = prevRoomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public boolean isTreated() {
        return isTreated;
    }

    public void setTreated(boolean treated) {
        isTreated = treated;
    }

    public boolean isOperated() {
        return operated;
    }

    public void setOperated(boolean isOperated) {
        this.operated = isOperated;
    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }



    public void setHospital(int hospital) {
        this.hospital = hospital;
    }

    // Other manipulation
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

    public void printPatientStatus(){
        System.out.println("|| Patient   "+patientId+"   | "+dead+"     | "+strength+"      | "+ severity+"     | "
                +hospital+"       | "+ operated +"       | "+roomName+"      | "+stayTime+"      | "+status.toString()+"      | "+released+" ||");
    }


}
