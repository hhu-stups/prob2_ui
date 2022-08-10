package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.voparser.VOParseException;
import de.prob.voparser.VOParser;
import de.prob.voparser.VTType;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.SimulationItemHandler;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.modelchecking.Modelchecker;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaHandler;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.vomanager.ast.AndValidationExpression;
import de.prob2.ui.vomanager.ast.IValidationExpression;
import de.prob2.ui.vomanager.ast.NotValidationExpression;
import de.prob2.ui.vomanager.ast.OrValidationExpression;
import de.prob2.ui.vomanager.ast.SequentialValidationExpression;
import de.prob2.ui.vomanager.ast.ValidationTaskExpression;

@Singleton
public class VOChecker {

	private final CurrentProject currentProject;

	private final RequirementHandler requirementHandler;

	private final Modelchecker modelchecker;

	private final LTLFormulaChecker ltlChecker;

	private final SymbolicCheckingFormulaHandler symbolicChecker;

	private final TraceChecker traceChecker;

	private final SimulationItemHandler simulationItemHandler;

	private final VOParser voParser;

	@Inject
	public VOChecker(final CurrentProject currentProject, final RequirementHandler requirementHandler, final Modelchecker modelchecker,
					 final LTLFormulaChecker ltlChecker, final SymbolicCheckingFormulaHandler symbolicChecker,
					 final TraceChecker traceChecker, final SimulationItemHandler simulationItemHandler) {
		this.voParser = new VOParser();
		this.currentProject = currentProject;
		this.requirementHandler = requirementHandler;
		this.modelchecker = modelchecker;
		this.ltlChecker = ltlChecker;
		this.symbolicChecker = symbolicChecker;
		this.traceChecker = traceChecker;
		this.simulationItemHandler = simulationItemHandler;
	}

	public void checkRequirement(Requirement requirement, Machine machine, VOManagerSetting setting) throws VOParseException{
		if(setting == VOManagerSetting.REQUIREMENT) {
			checkRequirementOnRequirementView(requirement);
		} else if(setting == VOManagerSetting.MACHINE) {
			checkRequirementOnMachineView(requirement, machine);
		}
		requirementHandler.updateChecked(currentProject.get(), machine, requirement, setting);
	}

	private void checkRequirementOnRequirementView(Requirement requirement) throws VOParseException {
		for(Machine machine : currentProject.getMachines()) {
			for (ValidationObligation validationObligation : machine.getValidationObligations()) {
				if(validationObligation.getRequirement().equals(requirement.getName())) {
					this.checkVO(validationObligation);
				}
			}
		}
	}

	private void checkRequirementOnMachineView(Requirement requirement, Machine machine) throws VOParseException {
		for (ValidationObligation validationObligation : machine.getValidationObligations()) {
			if(validationObligation.getRequirement().equals(requirement.getName())) {
				this.checkVO(validationObligation);
			}
		}
	}

	public void registerTask(String id, VTType type) {
		voParser.registerTask(id, type);
	}

	public void deregisterAllTasks() {
		voParser.getTasks().clear();
	}

	public void deregisterTask(String id) {
		voParser.deregisterTask(id);
	}

	public void parseVO(ValidationObligation vo) throws VOParseException {
		final IValidationExpression expression = IValidationExpression.parse(voParser, vo.getExpression());
		Machine machine = currentProject.getCurrentMachine();
		expression.getAllTasks().forEach(taskExpr -> {
			IValidationTask validationTask;
			if (machine.getValidationTasks().containsKey(taskExpr.getIdentifier())) {
				validationTask = machine.getValidationTasks().get(taskExpr.getIdentifier());
			} else {
				validationTask = new ValidationTaskNotFound(taskExpr.getIdentifier());
			}
			taskExpr.setTask(validationTask);
		});
		vo.setParsedExpression(expression);
	}

	private void checkVOExpression(IValidationExpression expression) {
		if (expression instanceof ValidationTaskExpression) {
			checkVT(((ValidationTaskExpression)expression).getTask());
		} else if (expression instanceof NotValidationExpression) {
			checkNotExpression((NotValidationExpression)expression);
		} else if (expression instanceof AndValidationExpression) {
			checkAndExpression((AndValidationExpression)expression);
		} else if (expression instanceof OrValidationExpression) {
			checkOrExpression((OrValidationExpression)expression);
		} else if (expression instanceof SequentialValidationExpression) {
			checkSequentialExpression((SequentialValidationExpression)expression);
		} else {
			throw new RuntimeException("VO expression type is unknown: " + expression.getClass());
		}
	}

	private void checkNotExpression(NotValidationExpression expression) {
		checkVOExpression(expression.getExpression());
	}

	private void checkAndExpression(AndValidationExpression expression) {
		checkVOExpression(expression.getLeft());
		checkVOExpression(expression.getRight());
		// TODO: Implement short circuiting
	}

	private void checkOrExpression(OrValidationExpression expression) {
		checkVOExpression(expression.getLeft());
		checkVOExpression(expression.getRight());
		// TODO: Implement short circuiting
	}

	private void checkSequentialExpression(SequentialValidationExpression expression) {
		// TODO
	}


	public void checkVO(ValidationObligation validationObligation) throws VOParseException {
		if (validationObligation.getParsedExpression() == null) {
			this.parseVO(validationObligation);
		}
		checkVOExpression(validationObligation.getParsedExpression());
	}

	public void checkVT(IValidationTask validationTask) {
		// FIXME Currently ignores exceptions from CompletableFutures!
		// (Those normally should never happen, but still...)
		if (validationTask instanceof ValidationTaskNotFound) {
			// Nothing to be done - it already shows an error status
		} else if (validationTask instanceof ModelCheckingItem) {
			modelchecker.startCheckIfNeeded((ModelCheckingItem) validationTask);
		} else if (validationTask instanceof LTLFormulaItem) {
			ltlChecker.checkFormulaNoninteractive((LTLFormulaItem) validationTask);
		} else if (validationTask instanceof SymbolicCheckingFormulaItem) {
			symbolicChecker.handleItemNoninteractive((SymbolicCheckingFormulaItem) validationTask);
		} else if (validationTask instanceof ReplayTrace) {
			traceChecker.checkNoninteractive((ReplayTrace) validationTask);
		} else if (validationTask instanceof SimulationItem) {
			simulationItemHandler.checkItem((SimulationItem) validationTask);
		} else {
			throw new AssertionError("Unhandled validation task type: " + validationTask.getClass());
		}
	}


}
