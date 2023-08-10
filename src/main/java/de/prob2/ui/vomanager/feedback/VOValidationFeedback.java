package de.prob2.ui.vomanager.feedback;

import java.util.Set;
import java.util.StringJoiner;

public class VOValidationFeedback {
	private final Set<String> dependentVTs;

	private final Set<String> dependentRequirements;

	public VOValidationFeedback(Set<String> dependentVTs, Set<String> dependentRequirements) {
		this.dependentVTs = dependentVTs;
		this.dependentRequirements = dependentRequirements;
	}

	public Set<String> getDependentVTs() {
		return dependentVTs;
	}

	public Set<String> getDependentRequirements() {
		return dependentRequirements;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", VOValidationFeedback.class.getSimpleName() + "[", "]")
				.add("dependentVTs=" + dependentVTs)
				.add("dependentRequirements=" + dependentRequirements)
				.toString();
	}
}
