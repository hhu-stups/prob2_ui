package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.animator.command.AbstractCommand;
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
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckingResult;
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
	
	private CompletableFuture<SymbolicCheckingFormulaItem> checkItem(SymbolicCheckingFormulaItem item, AbstractCommand cmd, final StateSpace stateSpace) {
		final CompletableFuture<SymbolicCheckingFormulaItem> future = cliExecutor.submit(() -> {
			stateSpace.execute(cmd);
			return item;
		});
		return future.exceptionally(e -> {
			LOGGER.error("Exception during symbolic checking", e);
			resultHandler.handleFormulaException(item, e);
			return item;
		});
	}
	
	private CompletableFuture<SymbolicCheckingFormulaItem> checkItem(IModelCheckJob checker, SymbolicCheckingFormulaItem item) {
		final CompletableFuture<SymbolicCheckingFormulaItem> future = cliExecutor.submit(() -> {
			final IModelCheckingResult res = checker.call();
			resultHandler.handleFormulaResult(item, res);
			return item;
		});
		return future.exceptionally(e -> {
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
		return checkItem(checker, item);
	}
		
	public CompletableFuture<SymbolicCheckingFormulaItem> handleRefinement(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedRefinementCheckCommand cmd = new ConstraintBasedRefinementCheckCommand();
		return checkItem(item, cmd, stateSpace).thenApply(r -> {
			resultHandler.handleRefinementChecking(item, cmd);
			return r;
		});
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleStaticAssertions(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(ConstraintBasedAssertionCheckCommand.CheckingType.STATIC, stateSpace);
		return checkItem(item, cmd, stateSpace).thenApply(r -> {
			resultHandler.handleAssertionChecking(item, cmd, stateSpace);
			return r;
		});
	}

	public CompletableFuture<SymbolicCheckingFormulaItem> handleDynamicAssertions(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(ConstraintBasedAssertionCheckCommand.CheckingType.DYNAMIC, stateSpace);
		return checkItem(item, cmd, stateSpace).thenApply(r -> {
			resultHandler.handleAssertionChecking(item, cmd, stateSpace);
			return r;
		});
	}

	public CompletableFuture<SymbolicCheckingFormulaItem> handleWellDefinedness(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		CheckWellDefinednessCommand cmd = new CheckWellDefinednessCommand();
		return checkItem(item, cmd, stateSpace).thenApply(r -> {
			resultHandler.handleWellDefinednessChecking(item, cmd);
			return r;
		});
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleSymbolic(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		final SymbolicModelcheckCommand.Algorithm algorithm = SymbolicModelcheckCommand.Algorithm.valueOf(item.getCode());
		SymbolicModelcheckCommand cmd = new SymbolicModelcheckCommand(algorithm);
		return checkItem(item, cmd, stateSpace).thenApply(r -> {
			resultHandler.handleSymbolicChecking(item, cmd);
			return r;
		});
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleDeadlock(SymbolicCheckingFormulaItem item) {
		IEvalElement constraint = new ClassicalB(item.getCode(), FormulaExpand.EXPAND);
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace(), constraint);
		return checkItem(checker, item);
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> findRedundantInvariants(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		GetRedundantInvariantsCommand cmd = new GetRedundantInvariantsCommand();
		return checkItem(item, cmd, stateSpace).thenApply(r -> {
			resultHandler.handleFindRedundantInvariants(item, cmd);
			return r;
		});
	}
	
	@Override
	public void handleItem(SymbolicCheckingFormulaItem item, boolean checkAll) {
		if(!item.selected()) {
			return;
		}
		final CompletableFuture<SymbolicCheckingFormulaItem> future;
		switch(item.getType()) {
			case INVARIANT:
				future = handleInvariant(item);
				break;
			case CHECK_REFINEMENT:
				future = handleRefinement(item);
				break;
			case CHECK_STATIC_ASSERTIONS:
				future = handleStaticAssertions(item);
				break;
			case CHECK_DYNAMIC_ASSERTIONS:
				future = handleDynamicAssertions(item);
				break;
			case CHECK_WELL_DEFINEDNESS:
				future = handleWellDefinedness(item);
				break;
			case DEADLOCK:
				future = handleDeadlock(item);
				break;
			case FIND_REDUNDANT_INVARIANTS:
				future = findRedundantInvariants(item);
				break;	
			case SYMBOLIC_MODEL_CHECK:
				future = handleSymbolic(item);
				break;
			default:
				throw new AssertionError("Unhandled symbolic checking type: " + item.getType());
		}
		future.thenAccept(r -> {
			if(!checkAll) {
				updateTrace(item);
			}
		});
	}
	
	@Override
	public void handleMachine(Machine machine) {
		machine.getSymbolicCheckingFormulas().forEach(item -> handleItem(item, true));
	}
		
}
