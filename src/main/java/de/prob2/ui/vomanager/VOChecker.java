package de.prob2.ui.vomanager;

import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.voparser.VOParseException;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.internal.executor.FxThreadExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.SimulationItemHandler;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.vomanager.ast.AndValidationExpression;
import de.prob2.ui.vomanager.ast.IValidationExpression;
import de.prob2.ui.vomanager.ast.OrValidationExpression;
import de.prob2.ui.vomanager.ast.ValidationTaskExpression;

@Singleton
public class VOChecker {
	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final CliTaskExecutor cliExecutor;

	private final FxThreadExecutor fxExecutor;

	private final SimulationItemHandler simulationItemHandler;

	@Inject
	public VOChecker(
		CurrentProject currentProject,
		CurrentTrace currentTrace,
		CliTaskExecutor cliExecutor,
		FxThreadExecutor fxExecutor,
		SimulationItemHandler simulationItemHandler
	) {
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.cliExecutor = cliExecutor;
		this.fxExecutor = fxExecutor;
		this.simulationItemHandler = simulationItemHandler;
	}

	public CompletableFuture<?> checkRequirement(Requirement requirement) {
		CompletableFuture<?> future = CompletableFuture.completedFuture(null);
		for (final ValidationObligation vo : requirement.getValidationObligations()) {
			future = future.thenCompose(res -> this.checkVO(vo));
		}
		return future;
	}

	private CompletableFuture<?> checkVOExpression(IValidationExpression expression) {
		if (expression instanceof ValidationTaskExpression) {
			return checkVT(((ValidationTaskExpression)expression).getTask());
		} else if (expression instanceof AndValidationExpression) {
			return checkAndExpression((AndValidationExpression)expression);
		} else if (expression instanceof OrValidationExpression) {
			return checkOrExpression((OrValidationExpression)expression);
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

	public CompletableFuture<?> checkVO(ValidationObligation validationObligation) {
		Machine machine = currentProject.get().getMachine(validationObligation.getMachine());
		if (machine == null) {
			throw new VOParseException("Machine not found in project: " + validationObligation.getMachine());
		}

		if (validationObligation.getParsedExpression() == null) {
			validationObligation.parse(machine);
		}

		// Check that the correct machine is loaded in the animator,
		// otherwise load that machine.
		CompletableFuture<?> loadFuture;
		if (currentProject.getCurrentMachine() != machine) {
			// First wait for startAnimation to run on the JavaFX application thread,
			// then wait for the future returned by that method.
			loadFuture = fxExecutor.submit(() -> currentProject.startAnimation(machine)).thenCompose(future -> future);
		} else {
			loadFuture = CompletableFuture.completedFuture(null);
		}

		// Once the correct machine is loaded, check the VO.
		return loadFuture.thenCompose(res -> checkVOExpression(validationObligation.getParsedExpression()));
	}

	private CompletableFuture<?> checkVT(IValidationTask validationTask) {
		final ExecutionContext context = new ExecutionContext(currentProject.get(), currentProject.getCurrentMachine(), currentTrace.getStateSpace());
		if (validationTask instanceof ValidationTaskNotFound) {
			// Nothing to be done - it already shows an error status
			return CompletableFuture.completedFuture(null);
		} else if (validationTask instanceof IExecutableItem) {
			return cliExecutor.submit(() -> ((IExecutableItem)validationTask).execute(context));
		} else if (validationTask instanceof SimulationItem) {
			simulationItemHandler.checkItem((SimulationItem) validationTask);
			// TODO Make SimulationItemHandler return a correct CompletableFuture!
			return CompletableFuture.completedFuture(null);
		} else {
			throw new AssertionError("Unhandled validation task type: " + validationTask.getClass());
		}
	}


}
