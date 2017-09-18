package simsos.scenario.mci.policy;

import java.util.ArrayList;

/**
 *
 * Created by Youlim Jung on 15/09/2017.
 *
 */
public class PolicyElement {

    String varName;
    String scope; // '"policyType", "condition", "action"
    String type; // "range", "value"
    ArrayList<String> values = new ArrayList<>();
    int[] range;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public ArrayList<String> getValues() {
        return values;
    }

    public void setValues(ArrayList<String> values) {
        this.values = values;
    }

    public int[] getRange() {
        return range;
    }

    public void setRange(int[] range) {
        this.range = range;
    }
}
