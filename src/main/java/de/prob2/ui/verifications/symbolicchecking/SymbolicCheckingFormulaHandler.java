package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.command.CheckWellDefinednessCommand;
import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.animator.domainobjects.ClassicalB;
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
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SymbolicCheckingFormulaHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicCheckingFormulaHandler.class);
	
	private SymbolicCheckingFormulaHandler() {
		throw new AssertionError("Utility class");
	}
	
	private static void handleFormulaResult(SymbolicCheckingFormulaItem item, StateSpace stateSpace, IModelCheckingResult result) {
		CheckingResultItem res;
		if (result instanceof ModelCheckOk) {
			res = new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.success");
		} else if (result instanceof CBCInvariantViolationFound || result instanceof CBCDeadlockFound) {
			res = new CheckingResultItem(Checked.FAIL, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.counterExample");
		} else if (result instanceof NotYetFinished || result instanceof CheckInterrupted) {
			res = new CheckingResultItem(Checked.INTERRUPTED, "common.result.message", result.getMessage());
		} else if (result instanceof CheckError) {
			res = new CheckingResultItem(Checked.INVALID_TASK, "common.result.message", result.getMessage());
		} else {
			throw new AssertionError("Unhandled symbolic checking result type: " + result.getClass());
		}
		item.setResultItem(res);
		
		final List<Trace> counterExamples;
		if (result instanceof CBCInvariantViolationFound violation) {
			counterExamples = new ArrayList<>();
			final int size = violation.getCounterexamples().size();
			for (int i = 0; i < size; i++) {
				counterExamples.add(violation.getTrace(i, stateSpace));
			}
		} else if (result instanceof ITraceDescription) {
			counterExamples = Collections.singletonList(((ITraceDescription)result).getTrace(stateSpace));
		} else {
			counterExamples = Collections.emptyList();
		}
		item.getCounterExamples().setAll(counterExamples);
	}
	
	private static void handleInvariant(SymbolicCheckingFormulaItem item, StateSpace stateSpace) {
		final ArrayList<String> eventNames;
		if (item.getCode().isEmpty()) {
			// Check all operations/events
			eventNames = null;
		} else {
			// Check only one specific operation/event
			eventNames = new ArrayList<>();
			eventNames.add(item.getCode());
		}
		CBCInvariantChecker checker = new CBCInvariantChecker(stateSpace, eventNames);
		handleFormulaResult(item, stateSpace, checker.call());
	}
		
	private static void handleRefinement(SymbolicCheckingFormulaItem item, StateSpace stateSpace) {
		ConstraintBasedRefinementCheckCommand cmd = new ConstraintBasedRefinementCheckCommand();
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
	}
	
	private static void handleAssertions(SymbolicCheckingFormulaItem item, ConstraintBasedAssertionCheckCommand.CheckingType type, StateSpace stateSpace) {
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(type, stateSpace);
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
	}
	
	private static void handleWellDefinedness(SymbolicCheckingFormulaItem item, StateSpace stateSpace) {
		CheckWellDefinednessCommand cmd = new CheckWellDefinednessCommand();
		stateSpace.execute(cmd);
		if (cmd.getDischargedCount().equals(cmd.getTotalCount())) {
			item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.wellDefinednessChecking.result.allDischarged.message", cmd.getTotalCount()));
		} else {
			item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.symbolicchecking.resultHandler.wellDefinednessChecking.result.undischarged.message", cmd.getDischargedCount(), cmd.getTotalCount(), cmd.getTotalCount().subtract(cmd.getDischargedCount())));
		}
	}
	
	private static void handleSymbolic(SymbolicCheckingFormulaItem item, StateSpace stateSpace) {
		final SymbolicModelcheckCommand.Algorithm algorithm = SymbolicModelcheckCommand.Algorithm.valueOf(item.getCode());
		SymbolicModelcheckCommand cmd = new SymbolicModelcheckCommand(algorithm);
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
				item.setResultItem(new CheckingResultItem(Checked.TIMEOUT, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.limitReached"));
				break;
			default:
				break;
		}
	}
	
	private static void handleDeadlock(SymbolicCheckingFormulaItem item, StateSpace stateSpace) {
		IEvalElement constraint = new ClassicalB(item.getCode());
		CBCDeadlockChecker checker = new CBCDeadlockChecker(stateSpace, constraint);
		handleFormulaResult(item, stateSpace, checker.call());
	}
	
	private static void findRedundantInvariants(SymbolicCheckingFormulaItem item, StateSpace stateSpace) {
		GetRedundantInvariantsCommand cmd = new GetRedundantInvariantsCommand();
		stateSpace.execute(cmd);
		List<String> result = cmd.getRedundantInvariants();
		if (result.isEmpty()) {
			item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.notFound"));
		} else {
			item.setResultItem(new CheckingResultItem(cmd.isTimeout() ? Checked.TIMEOUT : Checked.FAIL, "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.found", String.join("\n", result)));
		}
	}
	
	private static void checkItemInternal(final SymbolicCheckingFormulaItem item, final StateSpace stateSpace) {
		switch(item.getType()) {
			case CBC_INVARIANT_PRESERVATION_CHECKING:
				handleInvariant(item, stateSpace);
				break;
			case CBC_REFINEMENT_CHECKING:
				handleRefinement(item, stateSpace);
				break;
			case CBC_STATIC_ASSERTION_CHECKING:
				handleAssertions(item, ConstraintBasedAssertionCheckCommand.CheckingType.STATIC, stateSpace);
				break;
			case CBC_DYNAMIC_ASSERTION_CHECKING:
				handleAssertions(item, ConstraintBasedAssertionCheckCommand.CheckingType.DYNAMIC, stateSpace);
				break;
			case WELL_DEFINEDNESS_CHECKING:
				handleWellDefinedness(item, stateSpace);
				break;
			case CBC_DEADLOCK_FREEDOM_CHECKING:
				handleDeadlock(item, stateSpace);
				break;
			case CBC_FIND_REDUNDANT_INVARIANTS:
				findRedundantInvariants(item, stateSpace);
				break;
			case SYMBOLIC_MODEL_CHECKING:
				handleSymbolic(item, stateSpace);
				break;
			default:
				throw new AssertionError("Unhandled symbolic checking type: " + item.getType());
		}
	}
	
	public static void checkItem(final SymbolicCheckingFormulaItem item, final StateSpace stateSpace) {
		try {
			checkItemInternal(item, stateSpace);
		} catch (CommandInterruptedException exc) {
			LOGGER.info("Symbolic checking interrupted by user", exc);
			item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "common.result.message", exc.getMessage()));
		} catch (RuntimeException exc) {
			LOGGER.error("Exception during symbolic checking", exc);
			item.setResultItem(new CheckingResultItem(Checked.INVALID_TASK, "common.result.message", exc.getMessage()));
		}
	}
}
