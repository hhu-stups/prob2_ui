package de.prob2.ui.vomanager.feedback;

import com.google.inject.Singleton;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.vomanager.IValidationTask;
import de.prob2.ui.vomanager.ValidationObligation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class VOFeedbackManager {

	public Map<String, Set<String>> computeFullDependencies(List<ValidationObligation> validationObligations) {
		Map<String, Set<String>> result = new HashMap<>();
		for(ValidationObligation vo1 : validationObligations) {
			for(ValidationObligation vo2 : validationObligations) {
				if(vo1.getTasks().stream()
						.map(IValidationTask::getId)
						.collect(Collectors.toList())
						.containsAll(vo2.getTasks().stream()
								.map(IValidationTask::getId)
								.collect(Collectors.toList()))) {
					if(result.containsKey(vo1.getId())) {
						result.get(vo1.getId()).add(vo2.getId());
					} else {
						result.put(vo1.getId(), new HashSet<>(Collections.singletonList(vo2.getId())));
					}
				}
			}
		}
		return result;
	}

	public Map<String, VOValidationFeedback> computeValidationFeedback(List<ValidationObligation> validationObligations) {
		Map<String, VOValidationFeedback> result = new LinkedHashMap<>();
		Map<String, Set<String>> fullDependencies = computeFullDependencies(validationObligations);
		for(ValidationObligation vo : validationObligations) {
			Set<String> dependentRequirements = new LinkedHashSet<>();
			// For each failed VO
			if(vo.getChecked() == Checked.FAIL) {

				// Determine possible error sources in VTs
				Set<String> dependentVTs = vo.getTasks().stream()
						.filter(task -> task.getChecked() == Checked.FAIL)
						.map(IValidationTask::getId).collect(Collectors.toCollection(LinkedHashSet::new));

				// Determine other VOs using VTs that are possible error sources
				Set<String> dependentVOs = computeDependentVOs(vo.getId(), validationObligations, dependentVTs, fullDependencies)
						.stream()
						.map(ValidationObligation::getId).collect(Collectors.toCollection(LinkedHashSet::new));

				// Determine possible error sources in requirements
				dependentRequirements.addAll(computeDependentRequirements(computeDependentVOs(vo.getId(), validationObligations, dependentVTs, fullDependencies)));

				result.put(vo.getId(), new VOValidationFeedback(vo.getId(), vo.getRequirement(), dependentVOs, dependentVTs, dependentRequirements));
			}
		}
		return result;
	}

	private Set<ValidationObligation> computeDependentVOs(String currentVO, List<ValidationObligation> validationObligations, Set<String> dependentVTs,
														  Map<String, Set<String>> fullDependencies) {
		Set<ValidationObligation> result = new LinkedHashSet<>();
		for(String dependentVT : dependentVTs) {
			for(ValidationObligation vo : validationObligations) {
				if(vo.getId().equals(currentVO)) {
					continue;
				}
				if(vo.getTasks().stream()
						.map(IValidationTask::getId)
						.collect(Collectors.toList())
						.contains(dependentVT) && !fullDependencies.get(vo.getId()).contains(currentVO)) {
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
		Set<VOEvolutionFeedback> result = new LinkedHashSet<>();
		Set<String> conVOs = new LinkedHashSet<>();
		Set<String> conVTs = new LinkedHashSet<>();
		Set<String> conReqs = new LinkedHashSet<>();

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
