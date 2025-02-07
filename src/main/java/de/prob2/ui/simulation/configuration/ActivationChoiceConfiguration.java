package de.prob2.ui.simulation.configuration;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

@JsonPropertyOrder({ "id", "activations" })
public final class ActivationChoiceConfiguration extends DiagramConfiguration.NonUi {

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private Map<String, String> activations;

	@JsonCreator
	public ActivationChoiceConfiguration(
			@JsonProperty(value = "id", required = true) String id,
			@JsonProperty("chooseActivation") Map<String, String> activations
	) {
		super(id);
		this.activations = activations != null ? Map.copyOf(activations) : Map.of();
	}

	public void setActivations(Map<String, String> activations) {
		this.activations = activations != null ? Map.copyOf(activations) : Map.of();
	}

	@JsonGetter("chooseActivation")
	public Map<String, String> getActivations() {
		return this.activations;
	}

	public String getActivationsAsString() {
		return this.activations.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof ActivationChoiceConfiguration that)) {
			return false;
		} else {
			return Objects.equals(this.getActivations(), that.getActivations());
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getId(), this.getActivations());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				       .add("id", this.getId())
				       .add("activations", this.getActivations())
				       .toString();
	}
}
