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
 * Created by Youlim Jung on 17/08/2017.
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

//        JavaType arrCondType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, Condition.class);
        JavaType arrStringType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class);

        policy.setPolicyType(jsonNode.get("policyType").asText());

        JsonNode conditionNode = jsonNode.get("conditions");
        ArrayList<Condition> tempConditions = new ArrayList<>();
        Iterator <JsonNode> conditionIterator = conditionNode.elements();
        while(conditionIterator.hasNext()){
            JsonNode node = conditionIterator.next();
            Condition tCond = new Condition();

            tCond.setVariable(mapper.convertValue(node.get("variable"), String.class));
            tCond.setOperator(mapper.convertValue(node.get("operator"), String.class));
            tCond.setValue(mapper.convertValue(node.get("value"), arrStringType));

            tempConditions.add(tCond);
        }
        policy.setConditions(tempConditions);

//        ArrayList<Condition> tempConds = mapper.convertValue(jsonNode.get("conditions"), arrCondType);
//        policy.setConditions(tempConds);
        policy.setMinCompliance(mapper.convertValue(jsonNode.get("minCompliance"), Double.class));
        policy.setEnforce(mapper.convertValue(jsonNode.get("enforce"), Boolean.class));

        policy.setRole(jsonNode.get("role").asText());
        Action tempAction = new Action();
        tempAction.setActionName(jsonNode.get("action").get("actionName").asText());
//        tempAction.setActionTarget(jsonNode.get("action").get("actionTarget").asText());
//        tempAction.setGuideType(jsonNode.get("action").get("guideType").asText());
//        tempAction.setOperator(jsonNode.get("action").get("operator").asText());
        ArrayList<String> tempValues = mapper.convertValue(jsonNode.get("action").get("actionMethod"), arrStringType);
        tempAction.setActionMethod(tempValues);
        tempAction.setMethodValue(jsonNode.get("action").get("actionName").asInt());
//        Action tempAction = mapper.convertValue(jsonNode.get("action"), Action.class);
        policy.setAction(tempAction);

        return policy;
    }
}
