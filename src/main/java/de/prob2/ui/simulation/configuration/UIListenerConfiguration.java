package de.prob2.ui.simulation.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Objects;

public class UIListenerConfiguration extends DiagramConfiguration {

	private String event;

	private String predicate;

	private List<String> activating;

	public UIListenerConfiguration(String id, String event, String predicate, List<String> activating) {
		super(id);
		this.event = event;
		this.predicate = predicate;
		this.activating = activating;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public List<String> getActivating() {
		return activating;
	}

	public void setActivating(List<String> activating) {
		this.activating = activating;
	}
	
	@JsonIgnore
	public String getActivatingAsString() {
		return activating == null ? "" : activating.toString().substring(1, activating.toString().length() - 1);
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
