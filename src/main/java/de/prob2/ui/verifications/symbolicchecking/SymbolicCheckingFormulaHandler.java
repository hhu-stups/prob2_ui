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
	
	private void checkItem(SymbolicCheckingFormulaItem item, AbstractCommand cmd, final StateSpace stateSpace, boolean checkAll) {
		final CompletableFuture<AbstractCommand> future = cliExecutor.submit(() -> {
			stateSpace.execute(cmd);
			return cmd;
		});
		future.whenComplete((r, e) -> {
			if (e == null) {
				resultHandler.handleFormulaResult(item, r);
			} else {
				LOGGER.error("Exception during symbolic checking", e);
				resultHandler.handleFormulaResult(item, e);
			}
			if(!checkAll) {
				updateTrace(item);
			}
		});
	}
	
	private void checkItem(IModelCheckJob checker, SymbolicCheckingFormulaItem item, boolean checkAll) {
		final CompletableFuture<IModelCheckingResult> future = cliExecutor.submit(checker);
		future.whenComplete((r, e) -> {
			if (e == null) {
				resultHandler.handleFormulaResult(item, r);
			} else {
				LOGGER.error("Exception during symbolic checking", e);
				resultHandler.handleFormulaResult(item, e);
			}
			if(!checkAll) {
				updateTrace(item);
			}
		});
	}
	
	public void handleInvariant(SymbolicCheckingFormulaItem item, boolean checkAll) {
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
		checkItem(checker, item, checkAll);
	}
		
	public void handleRefinement(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedRefinementCheckCommand cmd = new ConstraintBasedRefinementCheckCommand();
		checkItem(item, cmd, stateSpace, checkAll);
	}
	
	public void handleStaticAssertions(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(ConstraintBasedAssertionCheckCommand.CheckingType.STATIC, stateSpace);
		checkItem(item, cmd, stateSpace, checkAll);
	}

	public void handleDynamicAssertions(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(ConstraintBasedAssertionCheckCommand.CheckingType.DYNAMIC, stateSpace);
		checkItem(item, cmd, stateSpace, checkAll);
	}

	public void handleWellDefinedness(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		CheckWellDefinednessCommand cmd = new CheckWellDefinednessCommand();
		checkItem(item, cmd, stateSpace, checkAll);
	}
	
	public void handleSymbolic(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		final SymbolicModelcheckCommand.Algorithm algorithm = SymbolicModelcheckCommand.Algorithm.valueOf(item.getCode());
		SymbolicModelcheckCommand cmd = new SymbolicModelcheckCommand(algorithm);
		checkItem(item, cmd, stateSpace, checkAll);
		
	}
	
	public void handleDeadlock(SymbolicCheckingFormulaItem item, boolean checkAll) {
		IEvalElement constraint = new ClassicalB(item.getCode(), FormulaExpand.EXPAND);
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace(), constraint);
		checkItem(checker, item, checkAll);
	}
	
	public void findRedundantInvariants(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		GetRedundantInvariantsCommand cmd = new GetRedundantInvariantsCommand();
		checkItem(item, cmd, stateSpace, checkAll);
	}
	
	@Override
	public void handleItem(SymbolicCheckingFormulaItem item, boolean checkAll) {
		if(!item.selected()) {
			return;
		}
		switch(item.getType()) {
			case INVARIANT:
				handleInvariant(item, checkAll);
				break;
			case CHECK_REFINEMENT:
				handleRefinement(item, checkAll);
				break;
			case CHECK_STATIC_ASSERTIONS:
				handleStaticAssertions(item, checkAll);
				break;
			case CHECK_DYNAMIC_ASSERTIONS:
				handleDynamicAssertions(item, checkAll);
				break;
			case CHECK_WELL_DEFINEDNESS:
				handleWellDefinedness(item, checkAll);
				break;
			case DEADLOCK:
				handleDeadlock(item, checkAll);
				break;
			case FIND_REDUNDANT_INVARIANTS:
				findRedundantInvariants(item, checkAll);
				break;	
			case SYMBOLIC_MODEL_CHECK:
				handleSymbolic(item, checkAll);
				break;
			default:
				throw new AssertionError("Unhandled symbolic checking type: " + item.getType());
		}
	}
	
	@Override
	public void handleMachine(Machine machine) {
		machine.getSymbolicCheckingFormulas().forEach(item -> handleItem(item, true));
	}
		
}
