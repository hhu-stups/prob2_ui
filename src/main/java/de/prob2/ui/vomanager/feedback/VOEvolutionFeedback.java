package de.prob2.ui.vomanager.feedback;

import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public class VOEvolutionFeedback {

	private final String voID;

	private final Set<String> contradictedVOs;

	private final Set<String> dependentVTs;

	private final Set<String> contradictedVTs;

	private final Set<String> dependentRequirements;

	private final Set<String> contradictedRequirements;

	public VOEvolutionFeedback(String voID, Set<String> contradictedVOs, Set<String> dependentVTs,
							   Set<String> contradictedVTs, Set<String> dependentRequirements,
							   Set<String> contradictedRequirements) {
		this.voID = voID;
		this.contradictedVOs = contradictedVOs;
		this.dependentVTs = dependentVTs;
		this.contradictedVTs = contradictedVTs;
		this.dependentRequirements = dependentRequirements;
		this.contradictedRequirements = contradictedRequirements;
	}

	public String getVoID() {
		return voID;
	}

	public Set<String> getContradictedVOs() {
		return contradictedVOs;
	}

	public Set<String> getDependentVTs() {
		return dependentVTs;
	}

	public Set<String> getContradictedVTs() {
		return contradictedVTs;
	}

	public Set<String> getDependentRequirements() {
		return dependentRequirements;
	}

	public Set<String> getContradictedRequirements() {
		return contradictedRequirements;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VOEvolutionFeedback that = (VOEvolutionFeedback) o;
		return Objects.equals(voID, that.voID);
	}

	@Override
	public int hashCode() {
		return Objects.hash(voID);
	}


	@Override
	public String toString() {
		return new StringJoiner(", ", VOEvolutionFeedback.class.getSimpleName() + "[", "]")
				.add("voID='" + voID + "'")
				.add("contradictedVOs=" + contradictedVOs)
				.add("dependentVTs=" + dependentVTs)
				.add("contradictedVTs=" + contradictedVTs)
				.add("dependentRequirements=" + dependentRequirements)
				.add("contradictedRequirements=" + contradictedRequirements)
				.toString();
	}
}
