package de.prob2.ui.simulation.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Objects;

public class UIListenerConfiguration extends DiagramConfiguration {

	private final String event;

	private final String predicate;

	private final List<String> activating;

	public UIListenerConfiguration(String id, String event, String predicate, List<String> activating) {
		super(id);
		this.event = event;
		this.predicate = predicate;
		this.activating = activating;
	}

	public String getEvent() {
		return event;
	}

	public String getPredicate() {
		return predicate;
	}

	public List<String> getActivating() {
		return activating;
	}

	@JsonIgnore
	public String getActivatingAsString() {
		return activating == null ? "" : activating.toString();
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UIListenerConfiguration that = (UIListenerConfiguration) o;
		return Objects.equals(id, that.id) && Objects.equals(event, that.event) && Objects.equals(predicate, that.predicate) && Objects.equals(activating, that.activating);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, event, predicate, activating);
	}
}
