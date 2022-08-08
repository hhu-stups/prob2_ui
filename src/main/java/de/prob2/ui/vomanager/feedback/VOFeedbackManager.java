package de.prob2.ui.vomanager.feedback;

import com.google.inject.Singleton;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.vomanager.IValidationTask;
import de.prob2.ui.vomanager.ValidationObligation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class VOFeedbackManager {

	public Map<String, VOValidationFeedback> computeValidationFeedback(List<ValidationObligation> validationObligations) {
		Map<String, VOValidationFeedback> result = new HashMap<>();
		Set<String> dependentVOs = new HashSet<>();
		Set<String> dependentVTs = new HashSet<>();
		Set<String> dependentRequirements = new HashSet<>();
		for(ValidationObligation vo : validationObligations) {
			// For each failed VO
			if(vo.getChecked() == Checked.FAIL) {

				// Determine possible error sources in VTs
				dependentVTs.addAll(vo.getTasks().stream()
						.filter(task -> task.getChecked() == Checked.FAIL)
						.map(IValidationTask::getId)
						.collect(Collectors.toList()));

				// Determine other VOs using VTs that are possible error sources
				dependentVOs.addAll(computeDependentVOs(vo.getId(), validationObligations, dependentVTs)
						.stream()
						.map(ValidationObligation::getId).collect(Collectors.toList()));

				// Determine possible error sources in requirements
				dependentRequirements.addAll(computeDependentRequirements(computeDependentVOs(vo.getId(), validationObligations, dependentVTs)));

				result.put(vo.getId(), new VOValidationFeedback(vo.getId(), dependentVOs, dependentVTs, dependentRequirements));
			}
		}
		return result;
	}

	private Set<ValidationObligation> computeDependentVOs(String currentVO, List<ValidationObligation> validationObligations, Set<String> dependentVTs) {
		Set<ValidationObligation> result = new HashSet<>();
		for(String dependentVT : dependentVTs) {
			for(ValidationObligation vo : validationObligations) {
				if(vo.getId().equals(currentVO)) {
					continue;
				}
				if(vo.getTasks().stream()
						.map(IValidationTask::getId)
						.collect(Collectors.toList())
						.contains(dependentVT)) {
					result.add(vo);
				}
			}
		}
		return result;
	}

	private Set<String> computeDependentRequirements(Set<ValidationObligation> dependentVOs) {
		return dependentVOs.stream()
				.map(ValidationObligation::getRequirement)
				.collect(Collectors.toSet());
	}

	private Set<VOEvolutionFeedback> computeEvolutionFeedback(Map<String, VOValidationFeedback> prevFeedback, Map<String, VOValidationFeedback> currentFeedback) {
		Set<VOEvolutionFeedback> result = new HashSet<>();
		Set<String> conVOs = new HashSet<>();
		Set<String> conVTs = new HashSet<>();
		Set<String> conReqs = new HashSet<>();

		// Compute contradicting VOs
		for(String prevVO : prevFeedback.keySet()) {
			if(!currentFeedback.containsKey(prevVO)) {
				conVOs.add(prevVO);
			}
		}

		// Compute contradicting VTs and requirements
		for(String vo : conVOs) {
			conVTs.addAll(prevFeedback.get(vo).getDependentVTs());
			conReqs.addAll(prevFeedback.get(vo).getDependentRequirements());
		}

		// Compute contradicting results
		for (String currentVO : currentFeedback.keySet()) {
			if(!prevFeedback.containsKey(currentVO)) {
				VOValidationFeedback feedback = currentFeedback.get(currentVO);
				result.add(new VOEvolutionFeedback(currentVO, conVOs, feedback.getDependentVTs(), conVTs, feedback.getDependentRequirements(), conReqs));
			}
		}

		return result;
	}

}
