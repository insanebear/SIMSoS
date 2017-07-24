package simsos.scenario.mci;

import java.util.ArrayList;

/**
 *
 * Created by Youlim Jung on 18/07/2017.
 *
 */
public class Policy {
    private String policyTag;
    private String[] condition;
    private ArrayList<String> action;

    public Policy() {
        this.condition = new String[3]; // var, op, val
        this.action = new ArrayList<>(); // not decided yet.
    }

    public String getPolicyTag() {
        return policyTag;
    }

    public void setPolicyTag(String policyTag) {
        this.policyTag = policyTag;
    }
}
