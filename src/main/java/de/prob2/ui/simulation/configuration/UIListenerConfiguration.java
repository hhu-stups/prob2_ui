package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

@JsonPropertyOrder({ "id", "event", "predicate", "activating" })
public final class UIListenerConfiguration extends DiagramConfiguration {

	private String event;
	private String predicate;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<String> activating;

	@JsonCreator
	public UIListenerConfiguration(
			@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "event", required = true) String event,
			@JsonProperty(value = "predicate", defaultValue = "1=1") String predicate,
			@JsonProperty("activating") List<String> activating
	) {
		super(id);
		this.event = Objects.requireNonNull(event, "event");
		this.predicate = predicate != null ? predicate : "1=1";
		this.activating = activating != null ? List.copyOf(activating) : List.of();
	}

	@JsonGetter("event")
	public String getEvent() {
		return this.event;
	}

	public void setEvent(String event) {
		this.event = Objects.requireNonNull(event, "event");
	}

	@JsonGetter("predicate")
	public String getPredicate() {
		return this.predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	@JsonGetter("activating")
	public List<String> getActivating() {
		return this.activating;
	}

	public void setActivating(List<String> activating) {
		this.activating = activating != null ? List.copyOf(activating) : List.of();
	}

	@JsonIgnore
	public String getActivatingAsString() {
		var s = this.activating.toString();
		return s.substring(1, s.length() - 1);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof UIListenerConfiguration that)) {
			return false;
		} else {
			return Objects.equals(this.getId(), that.getId()) && Objects.equals(this.getEvent(), that.getEvent()) && Objects.equals(this.getPredicate(), that.getPredicate()) && Objects.equals(this.getActivating(), that.getActivating());
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getId(), this.getEvent(), this.getPredicate(), this.getActivating());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				       .add("id", this.getId())
				       .add("event", this.getEvent())
				       .add("predicate", this.getPredicate())
				       .add("activating", this.getActivating())
				       .toString();
	}
}
