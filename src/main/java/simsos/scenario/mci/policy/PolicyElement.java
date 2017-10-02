package simsos.scenario.mci.policy;

import java.util.ArrayList;

/**
 *
 * Created by Youlim Jung on 15/09/2017.
 *
 */
public class PolicyElement {

    private String varName;
    private String varType; // "Environment", "policyType", "condition", "action"
//    ArrayList<String> relatedComponent = new ArrayList<>();
    private String valueType; // "range", "value"
    private ArrayList<String> values = new ArrayList<>();
    private int[] range;


    public String getScope() {
        return varType;
    }

    public void setScope(String varType) {
        this.varType = varType;
    }

//    public ArrayList<String> getRelatedComponent() {
//        return relatedComponent;
//    }
//
//    public void setRelatedComponent(ArrayList<String> relatedComponent) {
//        this.relatedComponent = relatedComponent;
//    }

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public int[] getRange() {
        return range;
    }

    public void setRange(int[] range) {
        this.range = range;
    }

    public String getVarType() {
        return varType;
    }

    public void setVarType(String varType) {
        this.varType = varType;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public ArrayList<String> getValues() {
        return values;
    }

    public void setValues(ArrayList<String> values) {
        this.values = values;
    }
}
