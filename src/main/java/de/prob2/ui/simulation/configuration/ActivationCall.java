package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import de.prob2.ui.simulation.SimulatorUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

@JsonPropertyOrder({
		"id",
		"params",
		"probability"
})
@JsonDeserialize(using = ActivationCallDeserializer.class)
public final class ActivationCall {

	private String id;
	private Map<String, String> params;
	private String probability;

	@JsonCreator
	public ActivationCall(
			@JsonProperty(value = "id", required = true) String id,
			@JsonProperty("params") Map<String, String> params,
			@JsonProperty(value = "id") String probability
	) {
		this.id = Objects.requireNonNull(id, "id");
		this.params = params == null ? new HashMap<>() : params;
		this.probability = probability == null ? SimulatorUtils.DEFAULT_PROBABILITY_AS_STRING : probability;
	}

	@JsonGetter("id")
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = Objects.requireNonNull(id, SimulatorUtils.DEFAULT_PROBABILITY_AS_STRING);
	}

	@JsonGetter("params")
	public Map<String, String> getParams() {
		return this.params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params == null ? new HashMap<>() : params;
	}

	public String getProbability() {
		return probability;
	}

	public void setProbability(String probability) {
		this.probability = probability == null ? SimulatorUtils.DEFAULT_PROBABILITY_AS_STRING : probability;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof ActivationCall that)) {
			return false;
		} else {
			return Objects.equals(this.getId(), that.getId()) && Objects.equals(this.getParams(), that.getParams())
					&& Objects.equals(this.getProbability(), that.getProbability());
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getId(), this.getParams(), this.getProbability());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.omitEmptyValues()
				.add("id", this.getId())
				.add("params", this.getParams())
				.add("probability", this.getProbability())
				.toString();
	}

	public String toValue() {
		if(params == null || params.isEmpty()) {
			return id;
		}
		String paramsAsValue = params.entrySet()
				.stream()
				.map(entry -> String.format("%s : %s", entry.getKey(), entry.getValue()))
				.collect(Collectors.joining(", "));
		return String.format("{\"id\": %s, \"params\": {%s}, \"probability\" : %s}", id, paramsAsValue, probability);
	}

}
