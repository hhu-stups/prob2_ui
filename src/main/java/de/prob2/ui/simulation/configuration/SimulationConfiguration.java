package de.prob2.ui.simulation.configuration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.json.HasMetadata;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;

@JsonPropertyOrder({
	"activations",
	"metadata",
})
public class SimulationConfiguration implements HasMetadata {
	public static final String FILE_TYPE = "Timed_Trace";
	public static final int CURRENT_FORMAT_VERSION = 1;

	private final List<ActivationConfiguration> activations;
	private final JsonMetadata metadata;

	public SimulationConfiguration(List<ActivationConfiguration> activations, JsonMetadata metadata) {
		this.activations = activations;
		this.metadata = metadata;
	}

	public static JsonMetadataBuilder metadataBuilder() {
		return new JsonMetadataBuilder(FILE_TYPE, CURRENT_FORMAT_VERSION)
			.withSavedNow()
			.withUserCreator();
	}

	@JsonProperty("activations")
	public List<ActivationConfiguration> getActivationConfigurations() {
		return activations;
	}

	@Override
	public JsonMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	public HasMetadata withMetadata(final JsonMetadata metadata) {
		return new SimulationConfiguration(this.getActivationConfigurations(), metadata);
	}
}
