package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.command.CheckWellDefinednessCommand;
import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.check.CBCDeadlockFound;
import de.prob.check.CBCInvariantViolationFound;
import de.prob.check.CheckError;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckOk;
import de.prob.check.NotYetFinished;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;

@Singleton
public class SymbolicCheckingResultHandler {
	private final CurrentTrace currentTrace;
	
	@Inject
	public SymbolicCheckingResultHandler(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
	}
	
	public void handleFormulaResult(SymbolicCheckingFormulaItem item, IModelCheckingResult result) {
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
	
	public void handleFormulaException(SymbolicCheckingFormulaItem item, Throwable result) {
		CheckingResultItem res;
		if (result instanceof CommandInterruptedException) {
			res = new CheckingResultItem(Checked.INTERRUPTED, "common.result.message", result.getMessage());
		} else {
			res = new CheckingResultItem(Checked.PARSE_ERROR, "common.result.message", result.getMessage());
		}
		item.setResultItem(res);
	}
	
	public void handleFindRedundantInvariants(SymbolicCheckingFormulaItem item, GetRedundantInvariantsCommand cmd) {
		List<String> result = cmd.getRedundantInvariants();
		if (result.isEmpty()) {
			item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.notFound"));
		} else {
			item.setResultItem(new CheckingResultItem(cmd.isTimeout() ? Checked.TIMEOUT : Checked.FAIL, "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.found", String.join("\n", result)));
		}
	}
	
	public void handleWellDefinednessChecking(final SymbolicCheckingFormulaItem item, final CheckWellDefinednessCommand cmd) {
		if (cmd.getDischargedCount().equals(cmd.getTotalCount())) {
			item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.wellDefinednessChecking.result.allDischarged.message", cmd.getTotalCount()));
		} else {
			item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.symbolicchecking.resultHandler.wellDefinednessChecking.result.undischarged.message", cmd.getDischargedCount(), cmd.getTotalCount(), cmd.getTotalCount().subtract(cmd.getDischargedCount())));
		}
	}
	
	public void handleRefinementChecking(SymbolicCheckingFormulaItem item, ConstraintBasedRefinementCheckCommand cmd) {
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
	
	public void handleAssertionChecking(SymbolicCheckingFormulaItem item, ConstraintBasedAssertionCheckCommand cmd, StateSpace stateSpace) {
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
	
	public void handleSymbolicChecking(SymbolicCheckingFormulaItem item, SymbolicModelcheckCommand cmd) {
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
	}
}
