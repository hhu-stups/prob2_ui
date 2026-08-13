package de.prob2.ui.simulation.configuration;

import java.lang.RuntimeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import de.prob2.ui.simulation.SimulatorUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ActivationCallDeserializer extends JsonDeserializer<ActivationCall> {


	@Override
	public ActivationCall deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		JsonNode node = parser.getCodec().readTree(parser);

		if(node.isTextual()) {
			return new ActivationCall(node.asText(), new HashMap<>(), SimulatorUtils.DEFAULT_PROBABILITY_AS_STRING);
		}

		JsonNode idNode = node.get("id");
		Map<String, String> params = new HashMap<>();
		if(node.isObject()) {
			if(idNode == null || !idNode.isTextual()) {
				throw new RuntimeException("ID must be present and a textual field");
			}
			JsonNode paramsNode = node.get("params");

			if(paramsNode != null && !paramsNode.isNull()) {
				params = parser.getCodec().treeToValue(paramsNode, Map.class);
			}
		}
		JsonNode probabilityNode = node.get("probability");
		return new ActivationCall(idNode.asText(), params, probabilityNode.asText());

	}

}
