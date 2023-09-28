package de.prob2.ui.vomanager;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;

abstract class VOManagerItem {
	static final class TopLevelRequirement extends VOManagerItem {
		TopLevelRequirement(final Requirement requirement) {
			super(requirement, null, null);
		}
		
		@Override
		String getDisplayText() {
			return this.getRequirement().getName();
		}
		
		@Override
		ObjectExpression<Checked> checkedProperty() {
			return checkedPropertyConjunction(this.getRequirement().getValidationObligations().stream()
				.map(ValidationObligation::checkedProperty)
				.collect(Collectors.toList()));
		}
	}
	
	static final class TopLevelMachine extends VOManagerItem {
		private final Collection<Requirement> projectRequirements;
		
		TopLevelMachine(final Collection<Requirement> projectRequirements, final Machine machine) {
			super(null, machine, null);
			this.projectRequirements = projectRequirements;
		}
		
		@Override
		String getDisplayText() {
			return this.getMachineName();
		}
		
		@Override
		ObjectExpression<Checked> checkedProperty() {
			return checkedPropertyConjunction(this.projectRequirements.stream()
				.map(req -> req.getValidationObligation(this.getMachineName()))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.map(ValidationObligation::checkedProperty)
				.collect(Collectors.toList()));
		}
	}
	
	abstract static class SecondLevelItem extends VOManagerItem {
		SecondLevelItem(final Requirement requirement, final Machine machine, final ValidationObligation vo) {
			super(requirement, machine, vo);
		}
		
		@Override
		ObjectExpression<Checked> checkedProperty() {
			if (this.getVo() == null) {
				// If the requirement doesn't contain any VOs yet,
				// the tree will contain a RequirementUnderMachine item with a null VO.
				return new SimpleObjectProperty<>(null);
			} else {
				return this.getVo().checkedProperty();
			}
		}
	}
	
	static final class RequirementUnderMachine extends SecondLevelItem {
		RequirementUnderMachine(final Requirement requirement, final Machine machine, final ValidationObligation vo) {
			super(requirement, machine, vo);
		}
		
		@Override
		String getDisplayText() {
			if (this.getVo() == null) {
				// If the requirement doesn't contain any VOs yet,
				// the tree will contain a RequirementUnderMachine item with a null VO.
				return this.getRequirement().getName();
			} else {
				return this.getRequirement().getName() + ": " + this.getVo().getExpression();
			}
		}
	}
	
	static final class MachineUnderRequirement extends SecondLevelItem {
		MachineUnderRequirement(final Requirement requirement, final Machine machine, final ValidationObligation vo) {
			super(requirement, machine, vo);
		}
		
		@Override
		String getDisplayText() {
			return this.getMachineName() + ": " + this.getVo().getExpression();
		}
	}
	
	private final Requirement requirement;
	private final Machine machine;
	private final ValidationObligation vo;
	
	VOManagerItem(final Requirement requirement, final Machine machine, final ValidationObligation vo) {
		this.requirement = requirement;
		this.machine = machine;
		this.vo = vo;
	}
	
	Requirement getRequirement() {
		return this.requirement;
	}
	
	String getRequirementName() {
		if (this.getRequirement() == null) {
			return null;
		} else {
			return this.getRequirement().getName();
		}
	}
	
	Machine getMachine() {
		return this.machine;
	}
	
	String getMachineName() {
		if (this.getMachine() == null) {
			return null;
		} else {
			return this.getMachine().getName();
		}
	}
	
	ValidationObligation getVo() {
		return this.vo;
	}
	
	private static ObjectExpression<Checked> checkedPropertyConjunction(final Collection<? extends ObjectExpression<Checked>> checkeds) {
		return Bindings.createObjectBinding(() ->
				checkeds.stream()
					.map(ObservableObjectValue::get)
					.reduce(Checked::and)
					.orElse(null),
			checkeds.toArray(new Observable[0]));
	}
	
	abstract String getDisplayText();
	
	abstract ObjectExpression<Checked> checkedProperty();
	
	Checked getChecked() {
		return this.checkedProperty().get();
	}
}
