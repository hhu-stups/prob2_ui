package de.prob2.ui.vomanager;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.Trace;
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

	public CompletableFuture<?> checkProject() {
		// Group VOs by machine to reduce switching between machines as much as possible.
		var vosByMachine = currentProject.getRequirements().stream()
			.flatMap(req -> req.getValidationObligations().stream())
			.collect(Collectors.groupingBy(ValidationObligation::getMachine));

		CompletableFuture<?> future = CompletableFuture.completedFuture(null);
		// Iterate over the project's machine list instead of the grouped map to ensure a predictable order.
		for (Machine machine : currentProject.getMachines()) {
			if (!vosByMachine.containsKey(machine.getName())) {
				continue;
			}
			for (ValidationObligation vo : vosByMachine.get(machine.getName())) {
				future = future.thenCompose(res -> this.checkVO(vo));
			}
		}
		return future;
	}

	public CompletableFuture<?> checkRequirement(Requirement requirement) {
		CompletableFuture<?> future = CompletableFuture.completedFuture(null);
		for (final ValidationObligation vo : requirement.getValidationObligations()) {
			future = future.thenCompose(res -> this.checkVO(vo));
		}
		return future;
	}

	public CompletableFuture<?> checkMachine(Machine machine) {
		CompletableFuture<?> future = CompletableFuture.completedFuture(null);
		for (Requirement requirement : currentProject.getRequirements()) {
			Optional<ValidationObligation> vo = requirement.getValidationObligation(machine);
			if (vo.isPresent()) {
				future = future.thenCompose(res -> this.checkVO(vo.get()));
			}
		}
		return future;
	}

	private CompletableFuture<?> checkVOExpression(IValidationExpression expression, ExecutionContext context) {
		if (expression instanceof ValidationTaskExpression) {
			return checkVT(((ValidationTaskExpression)expression).getTask(), context);
		} else if (expression instanceof AndValidationExpression) {
			return checkAndExpression((AndValidationExpression)expression, context);
		} else if (expression instanceof OrValidationExpression) {
			return checkOrExpression((OrValidationExpression)expression, context);
		} else {
			throw new RuntimeException("VO expression type is unknown: " + expression.getClass());
		}
	}

	private CompletableFuture<?> checkAndExpression(AndValidationExpression expression, ExecutionContext context) {
		return checkVOExpression(expression.getLeft(), context).thenCompose(r -> {
			final CompletableFuture<?> future;
			if (expression.getLeft().getChecked() == Checked.SUCCESS) {
				future = checkVOExpression(expression.getRight(), context);
			} else {
				future = CompletableFuture.completedFuture(r);
			}
			return future;
		});
	}

	private CompletableFuture<?> checkOrExpression(OrValidationExpression expression, ExecutionContext context) {
		return checkVOExpression(expression.getLeft(), context).thenCompose(r -> {
			final CompletableFuture<?> future;
			if (expression.getLeft().getChecked() == Checked.SUCCESS) {
				future = CompletableFuture.completedFuture(r);
			} else {
				future = checkVOExpression(expression.getRight(), context);
			}
			return future;
		});
	}

	public CompletableFuture<?> checkVO(ValidationObligation validationObligation) {
		Machine machine = currentProject.get().getMachine(validationObligation.getMachine());
		if (machine == null) {
			return CompletableFuture.failedFuture(new VOParseException("Machine not found in project: " + validationObligation.getMachine()));
		}

		if (validationObligation.getParsedExpression() == null) {
			try {
				validationObligation.parse(machine);
			} catch (VOParseException exc) {
				return CompletableFuture.failedFuture(exc);
			}
		}

		// Check that the correct machine is loaded in the animator,
		// otherwise load that machine.
		CompletableFuture<Trace> loadFuture;
		if (currentProject.getCurrentMachine() != machine) {
			// First wait for loadMachineWithoutConfirmation to run on the JavaFX application thread,
			// then wait for the future returned by that method.
			loadFuture = fxExecutor.submit(() -> currentProject.loadMachineWithoutConfirmation(machine)).thenCompose(future -> future);
		} else {
			loadFuture = CompletableFuture.completedFuture(currentTrace.get());
		}

		// Once the correct machine is loaded, check the VO.
		return loadFuture.thenCompose(trace -> {
			ExecutionContext context = new ExecutionContext(currentProject.get(), machine, trace.getStateSpace());
			return checkVOExpression(validationObligation.getParsedExpression(), context);
		});
	}

	private CompletableFuture<?> checkVT(IValidationTask validationTask, ExecutionContext context) {
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
			// Don't know how to (automatically) check this task, so do nothing.
			// For some tasks, such as proof obligations, there is nothing else we can do.
			// TODO For manual tasks (e. g. visualization), ask the user to decide.
			return CompletableFuture.completedFuture(null);
		}
	}


}
