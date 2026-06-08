package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

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
})
@JsonDeserialize(using = ActivationCallDeserializer.class)
public final class ActivationCall {

	private String id;
	private Map<String, String> params;

	@JsonCreator
	public ActivationCall(
			@JsonProperty(value = "id", required = true) String id,
			@JsonProperty("params") Map<String, String> params
	) {
		this.id = Objects.requireNonNull(id, "id");
		this.params = params == null ? new HashMap<>() : params;
	}

	@JsonGetter("id")
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = Objects.requireNonNull(id, "id");
	}

	@JsonGetter("params")
	public Map<String, String> getParams() {
		return this.params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params == null ? new HashMap<>() : params;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof ActivationCall that)) {
			return false;
		} else {
			return Objects.equals(this.getId(), that.getId()) && Objects.equals(this.getParams(), that.getParams());
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getId(), this.getParams());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.omitEmptyValues()
				.add("id", this.getId())
				.add("params", this.getParams())
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
		return String.format("{\"id\": %s, \"params\": {%s}}", id, paramsAsValue);
	}

}
