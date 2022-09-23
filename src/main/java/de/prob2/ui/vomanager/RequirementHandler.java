package de.prob2.ui.vomanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.inject.Singleton;

import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;

import javafx.beans.value.ChangeListener;

@Singleton
public class RequirementHandler {

	private final Map<Requirement, ChangeListener<Checked>> listenerMap = new HashMap<>();

	private Stream<Checked> getVOStream(Machine machine, Requirement requirement, VOManagerSetting setting) {
		return getValidationObligations(machine, requirement, setting).stream()
				.map(ValidationObligation::getChecked);
	}

	private List<ValidationObligation> getValidationObligations(Machine machine, Requirement requirement, VOManagerSetting setting) {
		if (setting == VOManagerSetting.REQUIREMENT) {
			return new ArrayList<>(requirement.getValidationObligations());
		} else {
			return requirement.getValidationObligation(machine)
				.map(Collections::singletonList)
				.orElse(Collections.emptyList());
		}
	}

	public void updateChecked(Machine machine, Requirement requirement, VOManagerSetting setting) {
		List<ValidationObligation> validationObligations = this.getValidationObligations(machine, requirement, setting);
		if (validationObligations.isEmpty()) {
			requirement.setChecked(Checked.NOT_CHECKED);
		} else {
			final boolean parseError = getVOStream(machine, requirement, setting).anyMatch(Checked.PARSE_ERROR::equals);
			final boolean failed = getVOStream(machine, requirement, setting).anyMatch(Checked.FAIL::equals);
			final boolean success = !failed && getVOStream(machine, requirement, setting).allMatch(Checked.SUCCESS::equals);
			final boolean timeout = !failed && getVOStream(machine, requirement, setting).anyMatch(Checked.TIMEOUT::equals);
			if (parseError) {
				requirement.setChecked(Checked.PARSE_ERROR);
			} else if (success) {
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

	public void resetListeners() {
		listenerMap.forEach((req, listener) -> {
			for (final ValidationObligation vo : req.getValidationObligations()) {
				vo.checkedProperty().removeListener(listener);
			}
		});
		listenerMap.clear();
	}

	public void initListeners(Machine machine, Requirement requirement, VOManagerSetting setting) {
		if (listenerMap.containsKey(requirement)) {
			final ChangeListener<Checked> oldListener = listenerMap.get(requirement);
			for (final ValidationObligation vo : requirement.getValidationObligations()) {
				vo.checkedProperty().removeListener(oldListener);
			}
		}

		// TODO: We should distinguish between requirement and requirement tree item. A requirement is linked to all machines while a requirement in a machine-based view is linked to a machine only
		final ChangeListener<Checked> newListener = (observable, from, to) -> updateChecked(machine, requirement, setting);
		listenerMap.put(requirement, newListener);
		for (final ValidationObligation vo : requirement.getValidationObligations()) {
			vo.checkedProperty().addListener(newListener);
		}
		updateChecked(machine, requirement, setting);
	}
}
