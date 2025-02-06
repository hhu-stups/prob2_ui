package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.json.HasMetadata;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;

@JsonPropertyOrder({
	"variables",
	"activations",
	"listeners",
	"metadata",
})
public class SimulationModelConfiguration implements HasMetadata, ISimulationModelConfiguration {

	public enum SimulationFileType {
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

	public static final String FILE_TYPE = "Simulation";
	public static final int CURRENT_FORMAT_VERSION = 3;

	private final Map<String, String> variables;
	private final List<DiagramConfiguration> activations;
	private final List<UIListenerConfiguration> uiListenerConfigurations;
	private final JsonMetadata metadata;

	public SimulationModelConfiguration(Map<String, String> variables, List<DiagramConfiguration> activations,
										List<UIListenerConfiguration> uiListenerConfigurations, JsonMetadata metadata) {
		this.variables = variables;
		this.activations = activations;
		this.uiListenerConfigurations = uiListenerConfigurations;
		this.metadata = Objects.requireNonNull(metadata, "metadata");
	}

	public static JsonMetadataBuilder metadataBuilder(SimulationFileType fileType) {
		return new JsonMetadataBuilder(fileType.getName(), CURRENT_FORMAT_VERSION)
			.withSavedNow()
			.withUserCreator();
	}

	@JsonProperty("variables")
	public Map<String, String> getVariables() {
		return variables;
	}

	@JsonProperty("activations")
	public List<DiagramConfiguration> getActivationConfigurations() {
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
		return new SimulationModelConfiguration(this.getVariables(), this.getActivationConfigurations(), this.getUiListenerConfigurations(), metadata);
	}
}
