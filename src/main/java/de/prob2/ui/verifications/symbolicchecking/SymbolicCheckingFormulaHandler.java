package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.command.CheckWellDefinednessCommand;
import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.CBCDeadlockChecker;
import de.prob.check.CBCDeadlockFound;
import de.prob.check.CBCInvariantChecker;
import de.prob.check.CBCInvariantViolationFound;
import de.prob.check.CheckError;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckOk;
import de.prob.check.NotYetFinished;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class SymbolicCheckingFormulaHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicCheckingFormulaHandler.class);
	
	private final CurrentTrace currentTrace;
	private final CliTaskExecutor cliExecutor;
	
	@Inject
	public SymbolicCheckingFormulaHandler(final CurrentTrace currentTrace, final CliTaskExecutor cliExecutor) {
		this.currentTrace = currentTrace;
		this.cliExecutor = cliExecutor;
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
			CheckingResultItem res;
			if (e instanceof CommandInterruptedException) {
				res = new CheckingResultItem(Checked.INTERRUPTED, "common.result.message", e.getMessage());
			} else {
				res = new CheckingResultItem(Checked.PARSE_ERROR, "common.result.message", e.getMessage());
			}
			item.setResultItem(res);
			return item;
		});
	}
	
	private void handleFormulaResult(SymbolicCheckingFormulaItem item, IModelCheckingResult result) {
		CheckingResultItem res;
		if (result instanceof ModelCheckOk) {
			res = new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.success");
		} else if (result instanceof CBCInvariantViolationFound || result instanceof CBCDeadlockFound) {
			res = new CheckingResultItem(Checked.FAIL, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.counterExample");
		} else if (result instanceof NotYetFinished || result instanceof CheckInterrupted) {
			res = new CheckingResultItem(Checked.INTERRUPTED, "common.result.message", result.getMessage());
		} else if (result instanceof CheckError) {
			res = new CheckingResultItem(Checked.PARSE_ERROR, "common.result.message", result.getMessage());
		} else {
			throw new AssertionError("Unhandled symbolic checking result type: " + result.getClass());
		}
		item.setResultItem(res);
		
		final List<Trace> counterExamples;
		if (result instanceof CBCInvariantViolationFound) {
			counterExamples = new ArrayList<>();
			final CBCInvariantViolationFound violation = (CBCInvariantViolationFound)result;
			final int size = violation.getCounterexamples().size();
			for (int i = 0; i < size; i++) {
				counterExamples.add(violation.getTrace(i, currentTrace.getStateSpace()));
			}
		} else if (result instanceof ITraceDescription) {
			counterExamples = Collections.singletonList(((ITraceDescription)result).getTrace(currentTrace.getStateSpace()));
		} else {
			counterExamples = Collections.emptyList();
		}
		item.getCounterExamples().setAll(counterExamples);
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
		return checkItem(item, () -> handleFormulaResult(item, checker.call()));
	}
		
	public CompletableFuture<SymbolicCheckingFormulaItem> handleRefinement(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedRefinementCheckCommand cmd = new ConstraintBasedRefinementCheckCommand();
		return checkItem(item, () -> {
			stateSpace.execute(cmd);
			ConstraintBasedRefinementCheckCommand.ResultType result = cmd.getResult();
			String msg = cmd.getResultsString();
			if (result == null) {
				item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.symbolicchecking.resultHandler.refinementChecking.result.notARefinementMachine.message"));
			} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.NO_VIOLATION_FOUND) {
				item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.refinementChecking.result.noViolationFound", msg));
			} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.VIOLATION_FOUND) {
				item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.symbolicchecking.resultHandler.refinementChecking.result.violationFound", msg));
			} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.INTERRUPTED) {
				item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "verifications.symbolicchecking.resultHandler.refinementChecking.result.interrupted", msg));
			}
		});
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleAssertions(SymbolicCheckingFormulaItem item, ConstraintBasedAssertionCheckCommand.CheckingType type) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(type, stateSpace);
		return checkItem(item, () -> {
			stateSpace.execute(cmd);
			ConstraintBasedAssertionCheckCommand.ResultType result = cmd.getResult();
			switch(result) {
				case NO_COUNTER_EXAMPLE_EXISTS:
					item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.assertionChecking.result.noCounterExampleExists"));
					break;
				case NO_COUNTER_EXAMPLE_FOUND:
					item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.assertionChecking.result.noCounterExampleFound"));
					break;
				case COUNTER_EXAMPLE:
					item.getCounterExamples().add(cmd.getTrace(stateSpace));
					item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.symbolicchecking.resultHandler.assertionChecking.result.counterExampleFound"));
					break;
				case INTERRUPTED:
					item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "verifications.symbolicchecking.resultHandler.assertionChecking.result.interrupted"));
					break;
				default:
					break;
			}
		});
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleWellDefinedness(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		CheckWellDefinednessCommand cmd = new CheckWellDefinednessCommand();
		return checkItem(item, () -> {
			stateSpace.execute(cmd);
			if (cmd.getDischargedCount().equals(cmd.getTotalCount())) {
				item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.wellDefinednessChecking.result.allDischarged.message", cmd.getTotalCount()));
			} else {
				item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.symbolicchecking.resultHandler.wellDefinednessChecking.result.undischarged.message", cmd.getDischargedCount(), cmd.getTotalCount(), cmd.getTotalCount().subtract(cmd.getDischargedCount())));
			}
		});
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleSymbolic(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		final SymbolicModelcheckCommand.Algorithm algorithm = SymbolicModelcheckCommand.Algorithm.valueOf(item.getCode());
		SymbolicModelcheckCommand cmd = new SymbolicModelcheckCommand(algorithm);
		return checkItem(item, () -> {
			stateSpace.execute(cmd);
			SymbolicModelcheckCommand.ResultType result = cmd.getResult();
			switch(result) {
				case SUCCESSFUL:
					item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.success"));
					break;
				case COUNTER_EXAMPLE:
					item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.counterExample"));
					break;
				case TIMEOUT:
					item.setResultItem(new CheckingResultItem(Checked.TIMEOUT, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.timeout"));
					break;
				case INTERRUPTED:
					item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.interrupted"));
					break;
				case LIMIT_REACHED:
					item.setResultItem(new CheckingResultItem(Checked.LIMIT_REACHED, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.limitReached"));
					break;
				default:
					break;
			}
		});
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleDeadlock(SymbolicCheckingFormulaItem item) {
		IEvalElement constraint = new ClassicalB(item.getCode(), FormulaExpand.EXPAND);
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace(), constraint);
		return checkItem(item, () -> handleFormulaResult(item, checker.call()));
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> findRedundantInvariants(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		GetRedundantInvariantsCommand cmd = new GetRedundantInvariantsCommand();
		return checkItem(item, () -> {
			stateSpace.execute(cmd);
			List<String> result = cmd.getRedundantInvariants();
			if (result.isEmpty()) {
				item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.notFound"));
			} else {
				item.setResultItem(new CheckingResultItem(cmd.isTimeout() ? Checked.TIMEOUT : Checked.FAIL, "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.found", String.join("\n", result)));
			}
		});
	}
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleItemNoninteractive(final SymbolicCheckingFormulaItem item) {
		switch(item.getType()) {
			case INVARIANT:
				return handleInvariant(item);
			case CHECK_REFINEMENT:
				return handleRefinement(item);
			case CHECK_STATIC_ASSERTIONS:
				return handleAssertions(item, ConstraintBasedAssertionCheckCommand.CheckingType.STATIC);
			case CHECK_DYNAMIC_ASSERTIONS:
				return handleAssertions(item, ConstraintBasedAssertionCheckCommand.CheckingType.DYNAMIC);
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
	
	public CompletableFuture<SymbolicCheckingFormulaItem> handleItem(SymbolicCheckingFormulaItem item, boolean checkAll) {
		return handleItemNoninteractive(item).thenApply(r -> {
			if(!checkAll) {
				updateTrace(item);
			}
			return r;
		});
	}
}
