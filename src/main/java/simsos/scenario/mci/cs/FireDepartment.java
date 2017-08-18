package simsos.scenario.mci.cs;

import simsos.scenario.mci.policy.Policy;
import simsos.simulation.component.Action;
import simsos.simulation.component.Agent;
import simsos.simulation.component.Message;
import simsos.simulation.component.World;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * Created by Youlim Jung on 18/07/2017.
 *
 */
public class FireDepartment extends Agent {
    private String name;
    private int fireDeptId;
    private int allocTrucks;
    private int allocExcavotors;
    private int allocFighters;

    private double conformRate; // indicates how much CS will follow policies

    private ArrayList<Policy> rescuePolicies;

    private FireFighter[] workFighterList;

    public FireDepartment(World world, int fireDeptId, String name, double conformRate) {
        super(world);
        this.name = name;
        this.fireDeptId = fireDeptId;
        this.conformRate = conformRate;
        this.reset();

        this.allocFighters = 50;    //NOTE 지금은 고정


        /* policy가 injected되면 얘네는 작동 전에 construction부터 조정하겠지
        * 최초 fd 생성 때 total resource랑 비교해서 제한에 위배되지 않는다면
        * ff를 policy에서 준 수만큼 instantiation
        */

        workFighterList = new FireFighter[allocFighters];
    }

    @Override
    public Action step() {
        /*
         * 여기에는 뭘 구현해야되지 ㅋㅋㅋ fd는 ff생성을 계속 할 것도 아니고
         * 할만한 기능은 agent status update받는거-_-?
         * 아니면 나중에interaction flag같은거 놓고 조건 맞으면
         * staging zone어디에 갖다놓을지? 할 수는 있을 듯.
         */

        return Action.getNullAction(0,"FireDepartment action-_-");
    }

    @Override
    public void reset() {

    }

    @Override
    public int getId() {
        return this.fireDeptId;
    }

    @Override
    public String getName() {
        return "Fire department";
    }

    @Override
    public void messageIn(Message msg) {

    }

    @Override
    public boolean makeDecision() {
        return false;
    }

    @Override
    public HashMap<String, Object> getProperties() {
        return new HashMap<String, Object>();
    }

    public int getAllocFighters() {
        return allocFighters;
    }

    public void setWorkFighterList(int idx, FireFighter fighter) {
        this.workFighterList[idx] = fighter;
    }
}
