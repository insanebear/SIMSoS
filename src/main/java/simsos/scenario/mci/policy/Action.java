package simsos.scenario.mci.policy;

/**
 *
 * Created by Youlim Jung on 06/08/2017.
 *
 */
public class Action {
    private String actType;
    private String target;
    private String operator;
    private String value;

    public String getActType() {
        return actType;
    }

    public void setActType(String actType) {
        this.actType = actType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
