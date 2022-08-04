package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.animator.command.CheckWellDefinednessCommand;
import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.CBCDeadlockChecker;
import de.prob.check.CBCInvariantChecker;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicFormulaHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SymbolicCheckingFormulaHandler implements SymbolicFormulaHandler<SymbolicCheckingFormulaItem> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicCheckingFormulaHandler.class);
	
	private final CurrentTrace currentTrace;
	private final SymbolicCheckingResultHandler resultHandler;
	private final CliTaskExecutor cliExecutor;
	
	@Inject
	public SymbolicCheckingFormulaHandler(final CurrentTrace currentTrace, final SymbolicCheckingResultHandler resultHandler, final CliTaskExecutor cliExecutor) {
		this.currentTrace = currentTrace;
		this.resultHandler = resultHandler;
		this.cliExecutor = cliExecutor;
	}
	
	@Override
	public List<SymbolicCheckingFormulaItem> getItems(final Machine machine) {
		return machine.getSymbolicCheckingFormulas();
	}
	
	private void updateTrace(SymbolicCheckingFormulaItem item) {
		List<Trace> counterExamples = item.getCounterExamples();
		if(!counterExamples.isEmpty()) {
			currentTrace.set(counterExamples.get(0));
		}
	}
	
	private CompletableFuture<SymbolicCheckingFormulaItem> checkItem(final SymbolicCheckingFormulaItem item, final Runnable task) {
		return cliExecutor.submit(task, item).exceptionally(e -> {
			LOGGER.error("Exception during symbolic checking", e);
			resultHandler.handleFormulaException(item, e);
			return item;
		});
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleInvariant(SymbolicCheckingFormulaItem item) {
		final ArrayList<String> eventNames;
		if (item.getCode().isEmpty()) {
			// Check all operations/events
			eventNames = null;
		} else {
			// Check only one specific operation/event
			eventNames = new ArrayList<>();
			eventNames.add(item.getCode());
		}
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), eventNames);
		return checkItem(item, () -> resultHandler.handleFormulaResult(item, checker.call()));
	}
		
	public CompletableFuture<SymbolicCheckingFormulaItem> handleRefinement(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedRefinementCheckCommand cmd = new ConstraintBasedRefinementCheckCommand();
		return checkItem(item, () -> {
			stateSpace.execute(cmd);
			resultHandler.handleRefinementChecking(item, cmd);
		});
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleStaticAssertions(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(ConstraintBasedAssertionCheckCommand.CheckingType.STATIC, stateSpace);
		return checkItem(item, () -> {
			stateSpace.execute(cmd);
			resultHandler.handleAssertionChecking(item, cmd, stateSpace);
		});
	}

	public CompletableFuture<SymbolicCheckingFormulaItem> handleDynamicAssertions(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(ConstraintBasedAssertionCheckCommand.CheckingType.DYNAMIC, stateSpace);
		return checkItem(item, () -> {
			stateSpace.execute(cmd);
			resultHandler.handleAssertionChecking(item, cmd, stateSpace);
		});
	}

	public CompletableFuture<SymbolicCheckingFormulaItem> handleWellDefinedness(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		CheckWellDefinednessCommand cmd = new CheckWellDefinednessCommand();
		return checkItem(item, () -> {
			stateSpace.execute(cmd);
			resultHandler.handleWellDefinednessChecking(item, cmd);
		});
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleSymbolic(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		final SymbolicModelcheckCommand.Algorithm algorithm = SymbolicModelcheckCommand.Algorithm.valueOf(item.getCode());
		SymbolicModelcheckCommand cmd = new SymbolicModelcheckCommand(algorithm);
		return checkItem(item, () -> {
			stateSpace.execute(cmd);
			resultHandler.handleSymbolicChecking(item, cmd);
		});
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleDeadlock(SymbolicCheckingFormulaItem item) {
		IEvalElement constraint = new ClassicalB(item.getCode(), FormulaExpand.EXPAND);
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace(), constraint);
		return checkItem(item, () -> resultHandler.handleFormulaResult(item, checker.call()));
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> findRedundantInvariants(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		GetRedundantInvariantsCommand cmd = new GetRedundantInvariantsCommand();
		return checkItem(item, () -> {
			stateSpace.execute(cmd);
			resultHandler.handleFindRedundantInvariants(item, cmd);
		});
	}
	
	@Override
	public CompletableFuture<SymbolicCheckingFormulaItem> handleItemNoninteractive(final SymbolicCheckingFormulaItem item) {
		switch(item.getType()) {
			case INVARIANT:
				return handleInvariant(item);
			case CHECK_REFINEMENT:
				return handleRefinement(item);
			case CHECK_STATIC_ASSERTIONS:
				return handleStaticAssertions(item);
			case CHECK_DYNAMIC_ASSERTIONS:
				return handleDynamicAssertions(item);
			case CHECK_WELL_DEFINEDNESS:
				return handleWellDefinedness(item);
			case DEADLOCK:
				return handleDeadlock(item);
			case FIND_REDUNDANT_INVARIANTS:
				return findRedundantInvariants(item);
			case SYMBOLIC_MODEL_CHECK:
				return handleSymbolic(item);
			default:
				throw new AssertionError("Unhandled symbolic checking type: " + item.getType());
		}
	}
	
	@Override
	public CompletableFuture<SymbolicCheckingFormulaItem> handleItem(SymbolicCheckingFormulaItem item, boolean checkAll) {
		if(!item.selected()) {
			return CompletableFuture.completedFuture(item);
		}
		return handleItemNoninteractive(item).thenApply(r -> {
			if(!checkAll) {
				updateTrace(item);
			}
			return r;
		});
	}
	
	@Override
	public void handleMachine(Machine machine) {
		machine.getSymbolicCheckingFormulas().forEach(item -> handleItem(item, true));
	}
		
}
