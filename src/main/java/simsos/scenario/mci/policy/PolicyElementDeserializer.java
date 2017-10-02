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

/**
 *
 * Created by Youlim Jung on 16/09/2017.

 */
public class PolicyElementDeserializer extends StdDeserializer<PolicyElement> {

    ObjectMapper mapper;

    public PolicyElementDeserializer() {
        super(PolicyElement.class);
    }

    public PolicyElementDeserializer(ObjectMapper mapper) {
        super(PolicyElement.class);
        this.mapper = mapper;
    }


    @Override
    public PolicyElement deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        PolicyElement pe = new PolicyElement();

        ObjectCodec objectCodec = jsonParser.getCodec();
        JsonNode jsonNode = objectCodec.readTree(jsonParser);
        JavaType arrStringType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class);

        pe.setVarName(jsonNode.get("varName").asText());
        pe.setScope(jsonNode.get("scope").asText());
//        ArrayList<String> tempRelatedCS = mapper.convertValue(jsonNode.get("relatedComponent"),arrStringType);
//        pe.setRelatedComponent(tempRelatedCS);
        String tempType = jsonNode.get("type").asText();
        pe.setType(tempType);
        ArrayList<String> tempValues = mapper.convertValue(jsonNode.get("values"), arrStringType);
        if (tempType.equals("range")) {
            int[] tempRange = new int[2];
            tempRange[0] = Integer.parseInt(tempValues.get(0));
            tempRange[1] = Integer.parseInt(tempValues.get(1));
            pe.setRange(tempRange);
        } else {
            pe.setValues(tempValues);
        }
        return pe;
    }
}