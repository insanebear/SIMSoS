package simsos.scenario.mci.policy;

/**
 *
 * Created by Youlim Jung on 06/08/2017.
 *
 */
public class Condition {
    private String variable;
    private String operator;
    private String value;

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
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
