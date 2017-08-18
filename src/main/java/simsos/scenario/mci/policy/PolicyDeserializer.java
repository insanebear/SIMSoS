package simsos.scenario.mci.policy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Youlim Jung on 17/08/2017.
 */
public class PolicyDeserializer extends StdDeserializer<Policy>{

    public PolicyDeserializer() {
        super(Policy.class);
    }

    @Override
    public Policy deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        Policy policy = new Policy();
        policy.setPolicyType(jsonParser.getCodec().readValue(jsonParser, String.class));
        Condition[] conditions = jsonParser.getCodec().readValue(jsonParser, Condition[].class);
        policy.setAction(jsonParser.getCodec().readValue(jsonParser, Action.class));
        policy.setConditions(new ArrayList<>(Arrays.asList(conditions)));

        return policy;
    }
}
