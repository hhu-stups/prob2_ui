package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class SimulationModelConfiguration implements HasMetadata, ISimulationModelConfiguration {

	public static final String FILE_TYPE = "Simulation";
	public static final int CURRENT_FORMAT_VERSION = 3;

	private final Map<String, String> variables;
	private final List<DiagramConfiguration> activations;
	private final List<UIListenerConfiguration> uiListenerConfigurations;
	@JsonInclude
	private final JsonMetadata metadata;

	@JsonCreator
	public SimulationModelConfiguration(
			@JsonProperty("variables") Map<String, String> variables,
			@JsonProperty("activations") List<DiagramConfiguration> activations,
			@JsonProperty("listeners") List<UIListenerConfiguration> uiListenerConfigurations,
			@JsonProperty("metadata") JsonMetadata metadata
	) {
		this.variables = variables != null ? Map.copyOf(variables) : Map.of();
		this.activations = activations != null ? List.copyOf(activations) : List.of();
		this.uiListenerConfigurations = uiListenerConfigurations != null ? List.copyOf(uiListenerConfigurations) : List.of();
		this.metadata = metadata != null ? metadata : metadataBuilder().build();
	}

	public static JsonMetadataBuilder metadataBuilder() {
		return new JsonMetadataBuilder(FILE_TYPE, CURRENT_FORMAT_VERSION)
			.withSavedNow()
			.withUserCreator();
	}

	@JsonGetter("variables")
	public Map<String, String> getVariables() {
		return this.variables;
	}

	@JsonGetter("activations")
	public List<DiagramConfiguration> getActivationConfigurations() {
		return this.activations;
	}

	@JsonGetter("listeners")
	public List<UIListenerConfiguration> getUiListenerConfigurations() {
		return this.uiListenerConfigurations;
	}

	@Override
	@JsonGetter("metadata")
	public JsonMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	public HasMetadata withMetadata(JsonMetadata metadata) {
		return new SimulationModelConfiguration(this.getVariables(), this.getActivationConfigurations(), this.getUiListenerConfigurations(), metadata);
	}
}
