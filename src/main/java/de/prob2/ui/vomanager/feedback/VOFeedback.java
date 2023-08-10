package de.prob2.ui.vomanager.feedback;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.vomanager.IValidationTask;
import de.prob2.ui.vomanager.Requirement;
import de.prob2.ui.vomanager.ValidationObligation;

public final class VOFeedback {
	private VOFeedback() {
		throw new AssertionError("Utility class");
	}

	public static Map<Requirement, Set<Requirement>> computeFullDependencies(final Map<Requirement, ValidationObligation> vosByRequirement) {
		Map<Requirement, Set<Requirement>> result = new HashMap<>();
		vosByRequirement.forEach((req1, vo1) ->
			vosByRequirement.forEach((req2, vo2) -> {
				if(vo1.getTasks().stream()
						.map(IValidationTask::getId)
						.collect(Collectors.toList())
						.containsAll(vo2.getTasks().stream()
								.map(IValidationTask::getId)
								.collect(Collectors.toList()))) {
					if(result.containsKey(req1)) {
						result.get(req1).add(req2);
					} else {
						result.put(req1, new HashSet<>(Collections.singletonList(req2)));
					}
				}
			})
		);
		return result;
	}

	public static Map<String, VOValidationFeedback> computeValidationFeedback(final List<Requirement> requirements, final Machine machine) {
		final Map<Requirement, ValidationObligation> vosByRequirement = new HashMap<>();
		for (final Requirement requirement : requirements) {
			requirement.getValidationObligation(machine).ifPresent(vo ->
				vosByRequirement.put(requirement, vo)
			);
		}
		
		Map<String, VOValidationFeedback> result = new LinkedHashMap<>();
		Map<Requirement, Set<Requirement>> fullDependencies = computeFullDependencies(vosByRequirement);
		vosByRequirement.forEach((requirement, vo) -> {
			// For each failed VO
			if(vo.getChecked() == Checked.FAIL) {

				// Determine possible error sources in VTs
				Set<String> dependentVTs = vo.getTasks().stream()
						.filter(task -> task.getChecked() == Checked.FAIL)
						.map(IValidationTask::getId).collect(Collectors.toCollection(LinkedHashSet::new));

				// Determine possible error sources in requirements
				Set<String> dependentRequirements = computeDependentVOs(requirement, vosByRequirement, dependentVTs, fullDependencies)
					.stream()
					.map(Requirement::getName)
					.collect(Collectors.toCollection(LinkedHashSet::new));
				
				result.put(requirement.getName(), new VOValidationFeedback(requirement.getName(), dependentVTs, dependentRequirements));
			}
		});
		return result;
	}

	private static Set<Requirement> computeDependentVOs(Requirement currentRequirement, Map<Requirement, ValidationObligation> vosByRequirement, Set<String> dependentVTs, Map<Requirement, Set<Requirement>> fullDependencies) {
		Set<Requirement> result = new LinkedHashSet<>();
		for(String dependentVT : dependentVTs) {
			vosByRequirement.forEach((requirement, vo) -> {
				if(requirement.equals(currentRequirement)) {
					return;
				}
				if(vo.getTasks().stream()
						.map(IValidationTask::getId)
						.collect(Collectors.toList())
						.contains(dependentVT) && !fullDependencies.get(requirement).contains(currentRequirement)) {
					result.add(requirement);
				}
			});
		}
		return result;
	}

	private static Set<VOEvolutionFeedback> computeEvolutionFeedback(Map<String, VOValidationFeedback> prevFeedback, Map<String, VOValidationFeedback> currentFeedback) {
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
