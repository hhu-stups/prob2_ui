package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.SimulationItemHandler;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.modelchecking.Modelchecker;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaHandler;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;

@Singleton
public class VOChecker {

	private final CurrentProject currentProject;

	private final RequirementHandler requirementHandler;

	private final Modelchecker modelchecker;

	private final LTLFormulaChecker ltlChecker;

	private final SymbolicCheckingFormulaHandler symbolicChecker;

	private final TraceChecker traceChecker;

	private final SimulationItemHandler simulationItemHandler;

	@Inject
	public VOChecker(final CurrentProject currentProject, final RequirementHandler requirementHandler, final Modelchecker modelchecker,
					 final LTLFormulaChecker ltlChecker, final SymbolicCheckingFormulaHandler symbolicChecker,
					 final TraceChecker traceChecker, final SimulationItemHandler simulationItemHandler) {
		this.currentProject = currentProject;
		this.requirementHandler = requirementHandler;
		this.modelchecker = modelchecker;
		this.ltlChecker = ltlChecker;
		this.symbolicChecker = symbolicChecker;
		this.traceChecker = traceChecker;
		this.simulationItemHandler = simulationItemHandler;
	}

	public void checkRequirement(Requirement requirement, Machine machine, VOManagerSetting setting) {
		if(setting == VOManagerSetting.REQUIREMENT) {
			checkRequirementOnRequirementView(requirement);
		} else if(setting == VOManagerSetting.MACHINE) {
			checkRequirementOnMachineView(requirement, machine);
		}
		requirementHandler.updateChecked(currentProject.get(), machine, requirement, setting);
	}

	private void checkRequirementOnRequirementView(Requirement requirement) {
		for(Machine machine : currentProject.getMachines()) {
			for (ValidationObligation validationObligation : machine.getValidationObligations()) {
				if(validationObligation.getRequirement().equals(requirement.getName())) {
					this.checkVO(validationObligation);
				}
			}
		}
	}

	private void checkRequirementOnMachineView(Requirement requirement, Machine machine) {
		for (ValidationObligation validationObligation : machine.getValidationObligations()) {
			if(validationObligation.getRequirement().equals(requirement.getName())) {
				this.checkVO(validationObligation);
			}
		}
	}


	public void checkVO(ValidationObligation validationObligation) {
		// TODO Implement full validation task syntax (not just conjunctions)
		for (IValidationTask validationTask : validationObligation.getTasks()) {
			if (validationTask != null) {
				checkVT(validationTask);
			}
		}
	}

	public void checkVT(IValidationTask validationTask) {
		if (validationTask instanceof ModelCheckingItem) {
			modelchecker.checkItem((ModelCheckingItem)validationTask, false, false);
		} else if (validationTask instanceof LTLFormulaItem) {
			ltlChecker.checkFormula((LTLFormulaItem)validationTask);
		} else if (validationTask instanceof SymbolicCheckingFormulaItem) {
			symbolicChecker.handleItem((SymbolicCheckingFormulaItem)validationTask, false);
		} else if (validationTask instanceof ReplayTrace) {
			traceChecker.check((ReplayTrace)validationTask, true);
		} else if (validationTask instanceof SimulationItem) {
			simulationItemHandler.checkItem((SimulationItem)validationTask, false);
		} else {
			throw new AssertionError("Unhandled validation task type: " + validationTask.getClass());
		}
	}

}
