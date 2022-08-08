package de.prob2.ui.vomanager.feedback;

import de.prob2.ui.verifications.Checked;
import de.prob2.ui.vomanager.IValidationTask;
import de.prob2.ui.vomanager.ValidationObligation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class VOFeedbackManager {

	public Map<String, VOValidationFeedback> computeValidationFeedback(List<ValidationObligation> validationObligations) {
		Map<String, VOValidationFeedback> result = new HashMap<>();
		Set<String> dependentVOs = new HashSet<>();
		Set<String> dependentVTs = new HashSet<>();
		Set<String> dependentRequirements = new HashSet<>();
		for(ValidationObligation vo : validationObligations) {
			if(vo.getChecked() == Checked.FAIL) {
				dependentVTs.addAll(vo.getTasks().stream()
						.filter(task -> task.getChecked() == Checked.FAIL)
						.map(IValidationTask::getId)
						.collect(Collectors.toList()));
				dependentVOs.addAll(computeDependentVOs(validationObligations, dependentVTs)
						.stream()
						.map(ValidationObligation::getId).collect(Collectors.toList()));
				dependentRequirements.addAll(computeDependentRequirements(computeDependentVOs(validationObligations, dependentVTs)));
				result.put(vo.getId(), new VOValidationFeedback(vo.getId(), dependentVOs, dependentVTs, dependentRequirements));
			}
		}
		return result;
	}

	private Set<ValidationObligation> computeDependentVOs(List<ValidationObligation> validationObligations, Set<String> dependentVTs) {
		Set<ValidationObligation> result = new HashSet<>();
		for(String dependentVT : dependentVTs) {
			for(ValidationObligation vo : validationObligations) {
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

}
