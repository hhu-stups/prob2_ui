package de.prob2.ui.vomanager.feedback;

import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public class VOValidationFeedback {

	private final String voID;

	private final String requirement;

	private final Set<String> dependentVOs;

	private final Set<String> dependentVTs;

	private final Set<String> dependentRequirements;

	public VOValidationFeedback(String voID, String requirement, Set<String> dependentVOs, Set<String> dependentVTs, Set<String> dependentRequirements) {
		this.voID = voID;
		this.requirement = requirement;
		this.dependentVOs = dependentVOs;
		this.dependentVTs = dependentVTs;
		this.dependentRequirements = dependentRequirements;
	}

	public String getVoID() {
		return voID;
	}

	public String getRequirement() {
		return requirement;
	}

	public Set<String> getDependentVOs() {
		return dependentVOs;
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
		return Objects.equals(voID, that.voID);
	}

	@Override
	public int hashCode() {
		return Objects.hash(voID);
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", VOValidationFeedback.class.getSimpleName() + "[", "]")
				.add("voID='" + voID + "'")
				.add("requirement=" + requirement)
				.add("dependentVOs=" + dependentVOs)
				.add("dependentVTs=" + dependentVTs)
				.add("dependentRequirements=" + dependentRequirements)
				.toString();
	}
}
