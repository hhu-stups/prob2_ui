package de.prob2.ui.vomanager;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.check.ModelCheckingOptions;
import de.prob.voparser.VOParseException;
import de.prob.voparser.VOParser;
import de.prob.voparser.VTType;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.SimulationItemHandler;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.modelchecking.Modelchecker;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaHandler;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.vomanager.ast.AndValidationExpression;
import de.prob2.ui.vomanager.ast.IValidationExpression;
import de.prob2.ui.vomanager.ast.OrValidationExpression;
import de.prob2.ui.vomanager.ast.SequentialValidationExpression;
import de.prob2.ui.vomanager.ast.ValidationTaskExpression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class VOChecker {
	private static final Logger LOGGER = LoggerFactory.getLogger(VOChecker.class);

	private final CurrentProject currentProject;

	private final Modelchecker modelchecker;

	private final LTLFormulaChecker ltlChecker;

	private final SymbolicCheckingFormulaHandler symbolicChecker;

	private final TraceChecker traceChecker;

	private final SimulationItemHandler simulationItemHandler;

	@Inject
	public VOChecker(final CurrentProject currentProject, final Modelchecker modelchecker,
					 final LTLFormulaChecker ltlChecker, final SymbolicCheckingFormulaHandler symbolicChecker,
					 final TraceChecker traceChecker, final SimulationItemHandler simulationItemHandler) {
		this.currentProject = currentProject;
		this.modelchecker = modelchecker;
		this.ltlChecker = ltlChecker;
		this.symbolicChecker = symbolicChecker;
		this.traceChecker = traceChecker;
		this.simulationItemHandler = simulationItemHandler;

		currentProject.currentMachineProperty().addListener((observable, from, to) -> updateOnMachine(to));
		updateOnMachine(currentProject.getCurrentMachine());
	}

	private void updateOnMachine(Machine machine) {
		if (machine == null) {
			return;
		}

		for (final Requirement requirement : currentProject.getRequirements()) {
			requirement.getValidationObligation(machine).ifPresent(vo -> {
				try {
					parseVO(machine, vo);
				} catch (VOParseException e) {
					LOGGER.warn("Parse error in validation expression", e);
				}
			});
		}
	}

	public void checkRequirement(Requirement requirement, Machine machine, VOManagerSetting setting) throws VOParseException{
		if(setting == VOManagerSetting.REQUIREMENT) {
			checkRequirementOnRequirementView(requirement);
		} else if(setting == VOManagerSetting.MACHINE) {
			checkRequirementOnMachineView(requirement, machine);
		}
	}

	private void checkRequirementOnRequirementView(Requirement requirement) throws VOParseException {
		for (final ValidationObligation vo : requirement.getValidationObligations()) {
			final Machine machine = currentProject.get().getMachine(vo.getMachine());
			this.checkVO(machine, vo);
		}
	}

	private void checkRequirementOnMachineView(Requirement requirement, Machine machine) throws VOParseException {
		for (final ValidationObligation vo : requirement.getValidationObligations()) {
			this.checkVO(machine, vo);
		}
	}

	public void parseVO(Machine machine, ValidationObligation vo) throws VOParseException {
		final VOParser voParser = new VOParser();
		machine.getValidationTasks().forEach((id, vt) -> voParser.registerTask(id, extractType(vt)));
		try {
			final IValidationExpression expression = IValidationExpression.parse(voParser, vo.getExpression());
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
		} catch (VOParseException e) {
			vo.setParsedExpression(null);
			throw e;
		}
	}

	public VTType extractType(IValidationTask validationTask) {
		if(validationTask instanceof ReplayTrace) {
			return VTType.TRACE;
		} else if(validationTask instanceof SimulationItem) {
			SimulationType simulationType = ((SimulationItem) validationTask).getType();
			if(simulationType == SimulationType.MONTE_CARLO_SIMULATION || simulationType == SimulationType.HYPOTHESIS_TEST || simulationType == SimulationType.ESTIMATION) {
				return VTType.EXPLORE;
			}
			// TODO: Implement a single simulation
			return VTType.TRACE;
		} else if(validationTask instanceof LTLFormulaItem) {
			return VTType.EXPLORE;
		} else if(validationTask instanceof ModelCheckingItem) {
			Set<ModelCheckingOptions.Options> options = ((ModelCheckingItem) validationTask).getOptions();
			if(options.contains(ModelCheckingOptions.Options.FIND_GOAL) ||
				((ModelCheckingItem) validationTask).getGoal() == null ||
				!((ModelCheckingItem) validationTask).getGoal().isEmpty()) {
				return VTType.TRACE;
			}
			// Otherwise invariant/deadlock checking, or just covering state space
			return VTType.EXPLORE;
		} else if(validationTask instanceof SymbolicCheckingFormulaItem) {
			return VTType.STATIC;
		}
		return null;
	}

	private CompletableFuture<?> checkVOExpression(IValidationExpression expression) {
		if (expression instanceof ValidationTaskExpression) {
			return checkVT(((ValidationTaskExpression)expression).getTask());
		} else if (expression instanceof AndValidationExpression) {
			return checkAndExpression((AndValidationExpression)expression);
		} else if (expression instanceof OrValidationExpression) {
			return checkOrExpression((OrValidationExpression)expression);
		} else if (expression instanceof SequentialValidationExpression) {
			return checkSequentialExpression((SequentialValidationExpression)expression);
		} else {
			throw new RuntimeException("VO expression type is unknown: " + expression.getClass());
		}
	}

	private CompletableFuture<?> checkAndExpression(AndValidationExpression expression) {
		return checkVOExpression(expression.getLeft()).thenCompose(r -> {
			final CompletableFuture<?> future;
			if (expression.getLeft().getChecked() == Checked.SUCCESS) {
				future = checkVOExpression(expression.getRight());
			} else {
				future = CompletableFuture.completedFuture(r);
			}
			return future;
		});
	}

	private CompletableFuture<?> checkOrExpression(OrValidationExpression expression) {
		return checkVOExpression(expression.getLeft()).thenCompose(r -> {
			final CompletableFuture<?> future;
			if (expression.getLeft().getChecked() == Checked.SUCCESS) {
				future = CompletableFuture.completedFuture(r);
			} else {
				future = checkVOExpression(expression.getRight());
			}
			return future;
		});
	}

	private CompletableFuture<?> checkSequentialExpression(SequentialValidationExpression expression) {
		return checkVOExpression(expression.getLeft()).thenCompose(r -> {
			final CompletableFuture<?> future;
			if (expression.getLeft().getChecked() == Checked.SUCCESS) {
				future = checkVOExpression(expression.getRight());
			} else {
				future = CompletableFuture.completedFuture(r);
			}
			return future;
		});
	}


	public void checkVO(Machine machine, ValidationObligation validationObligation) throws VOParseException {
		if (validationObligation.getParsedExpression() == null) {
			this.parseVO(machine, validationObligation);
		}
		checkVOExpression(validationObligation.getParsedExpression());
	}

	private CompletableFuture<?> checkVT(IValidationTask validationTask) {
		if (validationTask instanceof ValidationTaskNotFound) {
			// Nothing to be done - it already shows an error status
			return CompletableFuture.completedFuture(null);
		} else if (validationTask instanceof ModelCheckingItem) {
			return modelchecker.startCheckIfNeeded((ModelCheckingItem) validationTask);
		} else if (validationTask instanceof LTLFormulaItem) {
			return ltlChecker.checkFormulaNoninteractive((LTLFormulaItem) validationTask);
		} else if (validationTask instanceof SymbolicCheckingFormulaItem) {
			return symbolicChecker.handleItemNoninteractive((SymbolicCheckingFormulaItem) validationTask);
		} else if (validationTask instanceof ReplayTrace) {
			return traceChecker.checkNoninteractive((ReplayTrace) validationTask);
		} else if (validationTask instanceof SimulationItem) {
			simulationItemHandler.checkItem((SimulationItem) validationTask);
			// TODO Make SimulationItemHandler return a correct CompletableFuture!
			return CompletableFuture.completedFuture(null);
		} else {
			throw new AssertionError("Unhandled validation task type: " + validationTask.getClass());
		}
	}


}
