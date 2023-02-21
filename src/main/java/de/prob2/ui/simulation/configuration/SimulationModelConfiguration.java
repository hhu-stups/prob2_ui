package de.prob2.ui.simulation.configuration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.json.HasMetadata;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;

@JsonPropertyOrder({
	"activations",
	"listeners",
	"metadata",
})
public class SimulationModelConfiguration implements HasMetadata {

	public static enum SimulationFileType {
		SIMULATION("Simulation"),
		TIMED_TRACE("Timed_Trace"),
		INTERACTION_REPLAY("Interaction_Replay");

		private final String name;

		SimulationFileType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public static final int CURRENT_FORMAT_VERSION = 2;

	private final List<ActivationConfiguration> activations;
	private final List<UIListenerConfiguration> uiListenerConfigurations;
	private final JsonMetadata metadata;

	public SimulationModelConfiguration(List<ActivationConfiguration> activations, List<UIListenerConfiguration> uiListenerConfigurations, JsonMetadata metadata) {
		this.activations = activations;
		this.uiListenerConfigurations = uiListenerConfigurations;
		this.metadata = metadata;
	}

	public static JsonMetadataBuilder metadataBuilder(SimulationFileType fileType) {
		return new JsonMetadataBuilder(fileType.getName(), CURRENT_FORMAT_VERSION)
			.withSavedNow()
			.withUserCreator();
	}

	@JsonProperty("activations")
	public List<ActivationConfiguration> getActivationConfigurations() {
		return activations;
	}

	@JsonProperty("listeners")
	public List<UIListenerConfiguration> getUiListenerConfigurations() {
		return uiListenerConfigurations;
	}

	@Override
	public JsonMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	public HasMetadata withMetadata(final JsonMetadata metadata) {
		return new SimulationModelConfiguration(this.getActivationConfigurations(), this.getUiListenerConfigurations(), metadata);
	}
}
