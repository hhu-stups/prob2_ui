package de.prob2.ui.vomanager;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.CheckingStatus;

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
		ObjectExpression<CheckingStatus> statusProperty() {
			return statusPropertyConjunction(this.getRequirement().getValidationObligations().stream()
				.map(ValidationObligation::statusProperty)
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
		ObjectExpression<CheckingStatus> statusProperty() {
			return statusPropertyConjunction(this.projectRequirements.stream()
				.map(req -> req.getValidationObligation(this.getMachineName()))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.map(ValidationObligation::statusProperty)
				.collect(Collectors.toList()));
		}
	}
	
	abstract static class SecondLevelItem extends VOManagerItem {
		SecondLevelItem(final Requirement requirement, final Machine machine, final ValidationObligation vo) {
			super(requirement, machine, vo);
		}
		
		@Override
		ObjectExpression<CheckingStatus> statusProperty() {
			if (this.getVo() == null) {
				// If the requirement doesn't contain any VOs yet,
				// the tree will contain a RequirementUnderMachine item with a null VO.
				return new SimpleObjectProperty<>(null);
			} else {
				return this.getVo().statusProperty();
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
	
	private static ObjectExpression<CheckingStatus> statusPropertyConjunction(final Collection<? extends ObjectExpression<CheckingStatus>> statuses) {
		return Bindings.createObjectBinding(() ->
				statuses.stream()
					.map(ObservableObjectValue::get)
					.reduce(CheckingStatus::and)
					.orElse(null),
			statuses.toArray(new Observable[0]));
	}
	
	abstract String getDisplayText();
	
	abstract ObjectExpression<CheckingStatus> statusProperty();
	
	CheckingStatus getStatus() {
		return this.statusProperty().get();
	}
}
