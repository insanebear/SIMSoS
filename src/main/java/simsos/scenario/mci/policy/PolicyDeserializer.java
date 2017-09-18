package simsos.scenario.mci.policy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import jdk.internal.org.objectweb.asm.TypeReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Youlim Jung on 17/08/2017.
 */
public class PolicyDeserializer extends StdDeserializer<Policy>{

    ObjectMapper mapper;

    public PolicyDeserializer() {
        super(Policy.class);
    }

    public PolicyDeserializer(ObjectMapper mapper) {
        super(Policy.class);
        this.mapper = mapper;
    }

    @Override
    public Policy deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        Policy policy = new Policy();
        ObjectCodec objectCodec = jsonParser.getCodec();
        JsonNode jsonNode = objectCodec.readTree(jsonParser);
        JavaType type = mapper.getTypeFactory().constructCollectionType(ArrayList.class, Condition.class);

        policy.setPolicyType(jsonNode.get("policyType").asText());
        ArrayList<Condition> tempConds = mapper.convertValue(jsonNode.get("conditions"), type);
        policy.setConditions(tempConds);
        Action tempAction = mapper.convertValue(jsonNode.get("action"), Action.class);
        policy.setAction(tempAction);

        return policy;
    }
}
