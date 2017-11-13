package simsos.scenario.mci.policy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * Created by Youlim Jung on 17/08/2017.
 *
 */
public class PolicyDeserializer extends StdDeserializer<Policy>{

    ObjectMapper mapper;

    public PolicyDeserializer(ObjectMapper mapper) {
        super(Policy.class);
        this.mapper = mapper;
    }

    @Override
    public Policy deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        Policy policy = new Policy();
        ObjectCodec objectCodec = jsonParser.getCodec();
        JsonNode jsonNode = objectCodec.readTree(jsonParser);

        policy.setPolicyType(jsonNode.get("policyType").asText());

        JsonNode conditionNode = jsonNode.get("conditions");
        ArrayList<Condition> tempConditions = new ArrayList<>();
        Iterator <JsonNode> conditionIterator = conditionNode.elements();
        while(conditionIterator.hasNext()){
            JsonNode node = conditionIterator.next();
            Condition tCond = new Condition();

            tCond.setVariable(mapper.convertValue(node.get("variable"), String.class));
            tCond.setOperator(mapper.convertValue(node.get("operator"), String.class));
            tCond.setValue(mapper.convertValue(node.get("value"), String.class));

            tempConditions.add(tCond);
        }
        policy.setConditions(tempConditions);
        policy.setMinCompliance(mapper.convertValue(jsonNode.get("minCompliance"), Double.class));

        String bool = mapper.convertValue(jsonNode.get("enforce"), String.class);
        if(bool.equals("True") || bool.equals("true"))
            policy.setEnforce(true);
        else
            policy.setEnforce(false);

        policy.setRole(jsonNode.get("role").asText());
//        Action tempAction = mapper.convertValue(jsonNode.get("action"), Action.class);
        Action tempAction = new Action();
        tempAction.setActionName(jsonNode.get("action").get("actionName").asText());
//        ArrayList<String> tempValues = mapper.convertValue(jsonNode.get("action").get("actionMethod"), arrStringType);
        tempAction.setActionMethod(mapper.convertValue(jsonNode.get("action").get("actionMethod"), String.class));
        tempAction.setMethodValue(jsonNode.get("action").get("methodValue").asInt());

        policy.setAction(tempAction);

        return policy;
    }
}
