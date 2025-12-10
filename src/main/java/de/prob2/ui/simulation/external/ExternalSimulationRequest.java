package de.prob2.ui.simulation.external;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

@JsonPropertyOrder({
		"finished",
		"enabledOperations",
		"externalValues"
})
public record ExternalSimulationRequest(int finished, String enabledOperations, Map<String, String> externalValues) {
}
