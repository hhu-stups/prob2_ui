package de.prob2.ui.vomanager;

import com.google.inject.Singleton;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import javafx.beans.value.ChangeListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Singleton
public class RequirementHandler {

	private Map<Requirement, ChangeListener<Checked>> listenerMap = new HashMap<>();

	private Stream<Checked> getVOStream(Project project, Machine machine, Requirement requirement, VOManagerSetting setting) {
		return getValidationObligations(project, machine, requirement, setting).stream()
				.map(ValidationObligation::getChecked);
	}

	private List<ValidationObligation> getValidationObligations(Project project, Machine machine, Requirement requirement, VOManagerSetting setting) {
		List<ValidationObligation> validationObligations = new ArrayList<>();
		if(setting == VOManagerSetting.REQUIREMENT) {
			// Machine is not used here
			for(Machine mch : project.getMachines()) {
				for(ValidationObligation validationObligation : mch.getValidationObligations()) {
					if(validationObligation.getRequirement().equals(requirement.getName())) {
						validationObligations.add(validationObligation);
					}
				}
			}
		} else if(setting == VOManagerSetting.MACHINE) {
			for(ValidationObligation validationObligation : machine.getValidationObligations()) {
				if(validationObligation.getRequirement().equals(requirement.getName())) {
					validationObligations.add(validationObligation);
				}
			}
		}
		return validationObligations;
	}

	public void updateChecked(Project project, Machine machine, Requirement requirement, VOManagerSetting setting) {
		List<ValidationObligation> validationObligations = this.getValidationObligations(project, machine, requirement, setting);
		if (validationObligations.isEmpty()) {
			requirement.setChecked(Checked.NOT_CHECKED);
		} else {
			final boolean failed = getVOStream(project, machine, requirement, setting).anyMatch(Checked.FAIL::equals);
			final boolean success = !failed && getVOStream(project, machine, requirement, setting).allMatch(Checked.SUCCESS::equals);
			final boolean timeout = !failed && getVOStream(project, machine, requirement, setting).anyMatch(Checked.TIMEOUT::equals);
			if (success) {
				requirement.setChecked(Checked.SUCCESS);
			} else if (failed) {
				requirement.setChecked(Checked.FAIL);
			} else if (timeout) {
				requirement.setChecked(Checked.TIMEOUT);
			} else {
				requirement.setChecked(Checked.NOT_CHECKED);
			}
		}
	}

	public void initListeners(Project project, Machine machine, Requirement requirement, VOManagerSetting setting) {
		// TODO: We should distinguish between requirement and requirement tree item. A requirement is linked to all machines while a requirement in a machine-based view is linked to a machine only
		listenerMap.put(requirement, (observable, from, to) -> updateChecked(project, machine, requirement, setting));
		for(Machine mch : project.getMachines()) {
			for(ValidationObligation validationObligation : mch.getValidationObligations()) {
				validationObligation.checkedProperty().addListener(listenerMap.get(requirement));
			}
		}
	}

}
