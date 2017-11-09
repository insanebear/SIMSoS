package simsos.scenario.mci.policy;

import java.util.ArrayList;

/**
 *
 * Created by Youlim Jung on 06/08/2017.
 *
 */
public class Policy {
    private String policyType;
    protected ArrayList<Condition> conditions;
    private String role;
    private Action action;
    private double minCompliance;
    private boolean enforce;


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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public double getMinCompliance() {
        return minCompliance;
    }

    public void setMinCompliance(double minCompliance) {
        this.minCompliance = minCompliance;
    }

    public boolean isEnforce() {
        return enforce;
    }

    public void setEnforce(boolean enforce) {
        this.enforce = enforce;
    }


    public void printPolicy(){
        String condStr = "";
        for(Condition condition : conditions){
            String tempStr = condition.getVariable()+condition.getOperator()+condition.getValue()+" ";
            condStr+=tempStr;
        }

        System.out.println(policyType+" "+condStr+" "+role+" "+
        action.getActionName()+" "+action.getActionMethod()+" "+action.getMethodValue()+" "+
        minCompliance+" "+enforce);
    }
}
