package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.prob2fx.CurrentProject;
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

	private final Modelchecker modelchecker;

	private final LTLFormulaChecker ltlChecker;

	private final SymbolicCheckingFormulaHandler symbolicChecker;

	private final TraceChecker traceChecker;

	private final SimulationItemHandler simulationItemHandler;

	@Inject
	public VOChecker(final CurrentProject currentProject, final Modelchecker modelchecker, final LTLFormulaChecker ltlChecker, final SymbolicCheckingFormulaHandler symbolicChecker,
					 final TraceChecker traceChecker, final SimulationItemHandler simulationItemHandler) {
		this.currentProject = currentProject;
		this.modelchecker = modelchecker;
		this.ltlChecker = ltlChecker;
		this.symbolicChecker = symbolicChecker;
		this.traceChecker = traceChecker;
		this.simulationItemHandler = simulationItemHandler;
	}

	public void check(IAbstractRequirement requirement) {
		if(requirement instanceof Requirement) {
			check((Requirement) requirement);
		} else if(requirement instanceof ValidationObligation) {
			check((ValidationObligation) requirement);
		}
	}

	// TODO:
	// Unclear whether requirement is checked for a machine or for multiple machine (depends on view)
	public void check(Requirement requirement) {
		//requirement.getValidationObligations().forEach(this::check);
		//requirement.updateChecked();
	}


	public void check(ValidationObligation validationObligation) {
		// TODO: Implement for composed Validation Obligation
		// Currently assumes that a VO consists of one VT
		String voExpression = validationObligation.getExpression();
		validationObligation.checkedProperty().unbind();
		for(ValidationTask validationTask : currentProject.getCurrentMachine().getValidationTasks()) {
			if(validationTask.getId().equals(voExpression)) {
				validationObligation.checkedProperty().bind(validationTask.checkedProperty());
				check(validationTask);
				return;
			}
		}
	}

	public void check(ValidationTask validationTask) {
		ValidationTechnique validationTechnique = validationTask.getValidationTechnique();
		IExecutableItem executable = validationTask.getExecutable();
		switch (validationTechnique) {
			case MODEL_CHECKING:
				modelchecker.checkItem((ModelCheckingItem) executable, false, false);
				break;
			case LTL_MODEL_CHECKING:
				ltlChecker.checkFormula((LTLFormulaItem) executable);
				break;
			case SYMBOLIC_MODEL_CHECKING:
				symbolicChecker.handleItem((SymbolicCheckingFormulaItem) executable, false);
				break;
			case TRACE_REPLAY:
				traceChecker.check((ReplayTrace) executable, true);
				break;
			case SIMULATION:
				simulationItemHandler.checkItem((SimulationItem) executable, false);
				break;
			default:
				throw new RuntimeException("Validation technique is not valid: " + validationTechnique);
		}
	}

}
