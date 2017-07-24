package simsos.scenario.mci.cs;

import simsos.scenario.mci.Location;
import simsos.scenario.mci.Policy;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by mgjin on 2017-06-28.
 *
 * Edited by Youlim Jung on 2017-07-18.
 */
public class PTSCenter extends Agent {

    private String name;
    private int ptsCenterId;
    private Location location;

    private String patientName;
    private Location patientLocation;

    private String hospitalName;
    private Location hospitalLocation;

    public PTSCenter(World world, int ptsCenterId, String name, ArrayList<Policy> mciPolicies) {
        super(world);
        this.name = name;
        this.ptsCenterId = ptsCenterId;
        this.reset();
    }

    @Override
    public Action step() {
//        if (this.status == Status.GoingToPatient) {
//            if (!this.location.equals(patientLocation)) {
//                return new Action(1) {
//
//                    @Override
//                    public void execute() {
//                        if (PTSCenter.this.location.getX() < PTSCenter.this.patientLocation.getX())
//                            PTSCenter.this.location.moveX(1);
//                        else if (PTSCenter.this.location.getX() > PTSCenter.this.patientLocation.getX())
//                            PTSCenter.this.location.moveX(-1);
//                        else if (PTSCenter.this.location.getY() < PTSCenter.this.patientLocation.getY())
//                            PTSCenter.this.location.moveY(1);
//                        else if (PTSCenter.this.location.getY() > PTSCenter.this.patientLocation.getY())
//                            PTSCenter.this.location.moveY(-1);
//                    }
//
//                    @Override
//                    public String getId() {
//                        return PTSCenter.this.getId() + ": Goes to Patient_old";
//                    }
//                };
//            } else {
//                Message outMsg = new Message(world, Message.Purpose.Order, "Order to Get In");
//                outMsg.setSender(this.getId());
//                outMsg.setReceiver(this.patientName);
//                outMsg.payload.put("PTSLocation", this.location);
//                world.messageOut(outMsg);
//
//                this.patientLocation = null;
//
//                this.status = Status.GoingToHospital;
//            }
//        }
//
//        if (this.status == Status.GoingToHospital) {
//            if (!this.location.equals(hospitalLocation)) {
//                return new Action(1) {
//
//                    @Override
//                    public void execute() {
//                        if (PTSCenter.this.location.getX() < PTSCenter.this.hospitalLocation.getX())
//                            PTSCenter.this.location.moveX(1);
//                        else if (PTSCenter.this.location.getX() > PTSCenter.this.hospitalLocation.getX())
//                            PTSCenter.this.location.moveX(-1);
//                        else if (PTSCenter.this.location.getY() < PTSCenter.this.hospitalLocation.getY())
//                            PTSCenter.this.location.moveY(1);
//                        else if (PTSCenter.this.location.getY() > PTSCenter.this.hospitalLocation.getY())
//                            PTSCenter.this.location.moveY(-1);
//                    }
//
//                    @Override
//                    public String getId() {
//                        return PTSCenter.this.getId() + ": Goes to Hospital";
//                    }
//                };
//            } else {
//                Message outMsg = new Message(world, Message.Purpose.Order, "Order to Take the Patient_old");
//                outMsg.setSender(this.getId());
//                outMsg.setReceiver(this.hospitalName);
//                outMsg.payload.put("PatientName", this.patientName);
//                world.messageOut(outMsg);
//
//                this.patientName = null;
//                this.hospitalName = null;
//                this.hospitalLocation = null;
//
//                this.status = Status.Waiting;
//            }
//        }

//        if (this.status == Status.Waiting) {
//        return Action.getNullAction(1, this.getId() + ": Waiting");
//        }
        return Action.getNullAction(1, this.getName() + ": Waiting");
    }

    @Override
    public void reset() {
//        Random rd = new Random();
//
//        this.status = Status.Waiting;
//        this.location = new Location(MCIWorld.mciMapSize.getLeft() / 2, MCIWorld.mciMapSize.getRight() / 2);
    }

//    private int getUtility(Patient_old.Severity severity, Location patientLocation, Location hospitalLocation) {
////        return this.location.distanceTo(patientLocation) + patientLocation.distanceTo(hospitalLocation);
//    }

    @Override
    public int getId() {
        return this.ptsCenterId;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void messageIn(Message msg) {

    }

    @Override
    public HashMap<String, Object> getProperties() {
        return new HashMap<String, Object>();
    }

    @Override
    public void injectPolicies(ArrayList<Policy> policies) {

    }
}
