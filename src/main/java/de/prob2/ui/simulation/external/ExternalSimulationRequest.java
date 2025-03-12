package de.prob2.ui.simulation.external;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
		"finished",
		"enabledOperations",
})
public record ExternalSimulationRequest(int finished, String enabledOperations) {
}
