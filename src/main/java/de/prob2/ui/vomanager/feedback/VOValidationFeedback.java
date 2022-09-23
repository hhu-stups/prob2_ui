package de.prob2.ui.vomanager.feedback;

import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public class VOValidationFeedback {
	private final String requirement;

	private final Set<String> dependentVTs;

	private final Set<String> dependentRequirements;

	public VOValidationFeedback(String requirement, Set<String> dependentVTs, Set<String> dependentRequirements) {
		this.requirement = requirement;
		this.dependentVTs = dependentVTs;
		this.dependentRequirements = dependentRequirements;
	}

	public String getRequirement() {
		return requirement;
	}

	public Set<String> getDependentVTs() {
		return dependentVTs;
	}

	public Set<String> getDependentRequirements() {
		return dependentRequirements;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VOValidationFeedback that = (VOValidationFeedback) o;
		return Objects.equals(requirement, that.requirement);
	}

	@Override
	public int hashCode() {
		return Objects.hash(requirement);
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", VOValidationFeedback.class.getSimpleName() + "[", "]")
				.add("requirement=" + requirement)
				.add("dependentVTs=" + dependentVTs)
				.add("dependentRequirements=" + dependentRequirements)
				.toString();
	}
}
