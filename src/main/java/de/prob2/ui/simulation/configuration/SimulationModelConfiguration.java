package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob.json.HasMetadata;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;

@JsonPropertyOrder({
	"variables",
	"activations",
	"listeners",
	"metadata",
})
public final class SimulationModelConfiguration implements HasMetadata, ISimulationModelConfiguration {

	public static final String FILE_TYPE = "Simulation";
	public static final int CURRENT_FORMAT_VERSION = 3;

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final Map<String, String> variables;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final List<DiagramConfiguration.NonUi> activations;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final List<UIListenerConfiguration> listeners;
	private final JsonMetadata metadata;

	@JsonCreator
	public SimulationModelConfiguration(
			@JsonProperty("variables") Map<String, String> variables,
			@JsonProperty("activations") List<DiagramConfiguration.NonUi> activations,
			@JsonProperty("listeners") List<UIListenerConfiguration> listeners,
			@JsonProperty("metadata") JsonMetadata metadata
	) {
		this.variables = variables != null ? Map.copyOf(variables) : Map.of();
		this.activations = activations != null ? List.copyOf(activations) : List.of();
		this.listeners = listeners != null ? List.copyOf(listeners) : List.of();
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
	public List<DiagramConfiguration.NonUi> getActivations() {
		return this.activations;
	}

	@JsonGetter("listeners")
	public List<UIListenerConfiguration> getListeners() {
		return this.listeners;
	}

	@Override
	@JsonGetter("metadata")
	public JsonMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	public HasMetadata withMetadata(JsonMetadata metadata) {
		return new SimulationModelConfiguration(this.getVariables(), this.getActivations(), this.getListeners(), metadata);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof SimulationModelConfiguration that)) {
			return false;
		} else {
			return Objects.equals(this.getVariables(), that.getVariables()) && Objects.equals(this.getActivations(), that.getActivations()) && Objects.equals(this.getListeners(), that.getListeners()) && Objects.equals(this.getMetadata(), that.getMetadata());
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getVariables(), this.getActivations(), this.getListeners(), this.getMetadata());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.omitEmptyValues()
				.add("variables", this.getVariables())
				.add("activations", this.getActivations())
				.add("listeners", this.getListeners())
				.add("metadata", this.getMetadata())
				.toString();
	}
}
