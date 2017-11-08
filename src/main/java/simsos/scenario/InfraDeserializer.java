package simsos.scenario;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import simsos.scenario.mci.Location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * Created by Youlim Jung on 2017-11-07.
 *
 */
public class InfraDeserializer extends StdDeserializer<SoSInfrastructure> {
    ObjectMapper mapper;

    public InfraDeserializer(ObjectMapper mapper) {
        super(SoSInfrastructure.class);
        this.mapper = mapper;
    }


    @Override
    public SoSInfrastructure deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        SoSInfrastructure infrastructure = new SoSInfrastructure();
        ObjectCodec objectCodec = jsonParser.getCodec();
        JsonNode jsonNode = objectCodec.readTree(jsonParser);

        JsonNode locationsNode = jsonNode.get("hospitalLocations");
        ArrayList<Location> locations = new ArrayList<>();
        Iterator <JsonNode> locIterator = locationsNode.elements();
        while(locIterator.hasNext()){
            JsonNode node = locIterator.next();
            Location location = new Location(0, 0);
            location.setX(mapper.convertValue(node.get("x"), Integer.class));
            location.setY(mapper.convertValue(node.get("y"), Integer.class));

            locations.add(location);
        }
        infrastructure.setHospitalLocations(locations);

        infrastructure.setTypeSoS(mapper.convertValue(jsonNode.get("typeSoS"), String.class));
        infrastructure.setTotFireFighters(mapper.convertValue(jsonNode.get("totFireFighters"), Integer.class));
        infrastructure.setNumPTSCenter(mapper.convertValue(jsonNode.get("totGndAmbulances"), Integer.class));
        infrastructure.setTotHospital(mapper.convertValue(jsonNode.get("totHospital"), Integer.class));

        infrastructure.setMinCompliance(mapper.convertValue(jsonNode.get("minCompliance"), Double.class));
        infrastructure.setMaxCompliance(mapper.convertValue(jsonNode.get("maxCompliance"), Double.class));
        infrastructure.setRescueCompliance(mapper.convertValue(jsonNode.get("rescueCompliance"), Double.class));
        infrastructure.setTransportCompliance(mapper.convertValue(jsonNode.get("transportCompliance"), Double.class));
        infrastructure.setTreatmentCompliance(mapper.convertValue(jsonNode.get("treatmentCompliance"), Double.class));
        infrastructure.setEnforceRate(mapper.convertValue(jsonNode.get("enforceRate"), Double.class));

        infrastructure.setCrew(mapper.convertValue(jsonNode.get("crew"), Integer.class));
        infrastructure.setGeneral(mapper.convertValue(jsonNode.get("general"), Integer.class));
        infrastructure.setIntensive(mapper.convertValue(jsonNode.get("intensive"), Integer.class));
        infrastructure.setOperating(mapper.convertValue(jsonNode.get("operating"), Integer.class));

        return infrastructure;
    }
}
