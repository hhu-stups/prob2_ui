package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Objects;

public class UIListenerConfiguration {

	private final String id;

	private final String event;

	private final String predicate;

	private final List<String> activating;

	public UIListenerConfiguration(String id, String event, String predicate, List<String> activating) {
		this.id = id;
		this.event = event;
		this.predicate = predicate;
		this.activating = activating;
	}

	public String getId() {
		return id;
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
