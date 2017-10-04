package simsos.scenario.mci.policy;

import java.util.ArrayList;

/**
 *
 * Created by Youlim Jung on 06/08/2017.
 *
 */
public class Action {
    private String actionName;
    private String actionTarget;
    private String guideType;
    private String operator;
    private ArrayList<String> actionMethod;

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getActionTarget() {
        return actionTarget;
    }

    public void setActionTarget(String actionTarget) {
        this.actionTarget = actionTarget;
    }

    public String getGuideType() {
        return guideType;
    }

    public void setGuideType(String guideType) {
        this.guideType = guideType;
    }

//    public String getActionMethod() {
//        return actionMethod;
//    }
//
//    public void setActionMethod(String actionMethod) {
//        this.actionMethod = actionMethod;
//    }


    public ArrayList<String> getActionMethod() {
        return actionMethod;
    }

    public void setActionMethod(ArrayList<String> actionMethod) {
        this.actionMethod = actionMethod;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

}
