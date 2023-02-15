package de.prob2.ui.vomanager;

import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.StateSpace;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.SimulationItemHandler;
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

@Singleton
public class VOChecker {
	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final CliTaskExecutor cliExecutor;

	private final TraceChecker traceChecker;

	private final SimulationItemHandler simulationItemHandler;

	@Inject
	public VOChecker(final CurrentProject currentProject, final CurrentTrace currentTrace, final CliTaskExecutor cliExecutor,
					 final TraceChecker traceChecker, final SimulationItemHandler simulationItemHandler) {
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.cliExecutor = cliExecutor;
		this.traceChecker = traceChecker;
		this.simulationItemHandler = simulationItemHandler;
	}

	public void checkRequirement(Requirement requirement) {
		for (final ValidationObligation vo : requirement.getValidationObligations()) {
			this.checkVO(vo);
		}
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


	public void checkVO(ValidationObligation validationObligation) {
		if (validationObligation.getParsedExpression() == null) {
			final Machine machine = currentProject.get().getMachine(validationObligation.getMachine());
			validationObligation.parse(machine);
		}
		checkVOExpression(validationObligation.getParsedExpression());
	}

	private CompletableFuture<?> checkVT(IValidationTask validationTask) {
		final Machine machine = currentProject.getCurrentMachine();
		final StateSpace stateSpace = currentTrace.getStateSpace();
		if (validationTask instanceof ValidationTaskNotFound) {
			// Nothing to be done - it already shows an error status
			return CompletableFuture.completedFuture(null);
		} else if (validationTask instanceof ModelCheckingItem) {
			return cliExecutor.submit(() -> Modelchecker.executeIfNeeded((ModelCheckingItem) validationTask, stateSpace));
		} else if (validationTask instanceof LTLFormulaItem) {
			return cliExecutor.submit(() -> LTLFormulaChecker.checkFormula((LTLFormulaItem) validationTask, machine, stateSpace));
		} else if (validationTask instanceof SymbolicCheckingFormulaItem) {
			return cliExecutor.submit(() -> SymbolicCheckingFormulaHandler.checkItem((SymbolicCheckingFormulaItem) validationTask, stateSpace));
		} else if (validationTask instanceof ReplayTrace) {
			return traceChecker.checkNoninteractive((ReplayTrace) validationTask, stateSpace);
		} else if (validationTask instanceof SimulationItem) {
			simulationItemHandler.checkItem((SimulationItem) validationTask);
			// TODO Make SimulationItemHandler return a correct CompletableFuture!
			return CompletableFuture.completedFuture(null);
		} else {
			throw new AssertionError("Unhandled validation task type: " + validationTask.getClass());
		}
	}


}
