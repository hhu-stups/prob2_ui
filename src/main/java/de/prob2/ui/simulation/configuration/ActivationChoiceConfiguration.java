package de.prob2.ui.simulation.configuration;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

@JsonPropertyOrder({
		"id",
		"comment",
		"activations"
})
public final class ActivationChoiceConfiguration extends DiagramConfiguration.NonUi {

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private Map<String, String> chooseActivation;

	@JsonCreator
	public ActivationChoiceConfiguration(
			@JsonProperty(value = "id", required = true) String id,
			@JsonProperty("chooseActivation") Map<String, String> chooseActivation,
			@JsonProperty("comment") String comment
	) {
		super(id, comment);
		this.chooseActivation = chooseActivation != null ? Map.copyOf(chooseActivation) : Map.of();
	}

	public void setChooseActivation(Map<String, String> chooseActivation) {
		this.chooseActivation = chooseActivation != null ? Map.copyOf(chooseActivation) : Map.of();
	}

	@JsonGetter("chooseActivation")
	public Map<String, String> getChooseActivation() {
		return this.chooseActivation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof ActivationChoiceConfiguration that)) {
			return false;
		} else {
			return Objects.equals(this.getId(), that.getId()) && Objects.equals(this.getComment(), that.getComment()) && Objects.equals(this.getChooseActivation(), that.getChooseActivation());
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getId(), this.getComment(), this.getChooseActivation());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.omitEmptyValues()
				.add("id", this.getId())
				.add("comment", this.getComment())
				.add("chooseActivation", this.getChooseActivation())
				.toString();
	}
}
