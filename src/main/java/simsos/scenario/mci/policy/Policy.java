package simsos.scenario.mci.policy;

import java.util.ArrayList;

/**
 *
 * Created by Youlim Jung on 06/08/2017.
 *
 */
public class Policy {
    private int policyId;

    private String policyType;
    protected ArrayList<Condition> conditions;
    private Action action;

    public int getPolicyId() {
        return policyId;
    }

    public void setPolicyId(int policyId) {
        this.policyId = policyId;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public void setConditions(ArrayList<Condition> conditions) {
        this.conditions = conditions;
    }

    public ArrayList<Condition> getConditions() {
        return conditions;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void printConditions(){
        for (Condition condition : conditions)
            System.out.println(condition.getVariable()+condition.getOperator()+condition.getValue());
    }

    public void printAction(){
        System.out.println(action.getConstraintType()+" "+action.getTarget()+action.getOperator()+action.getValue());
    }
}
