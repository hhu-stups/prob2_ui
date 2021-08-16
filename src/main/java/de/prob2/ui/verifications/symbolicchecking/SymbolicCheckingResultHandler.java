package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.command.AbstractCommand;
import de.prob.animator.command.CheckWellDefinednessCommand;
import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.check.CBCDeadlockFound;
import de.prob.check.CBCInvariantViolationFound;
import de.prob.check.CheckError;
import de.prob.check.CheckInterrupted;
import de.prob.check.ModelCheckOk;
import de.prob.check.NotYetFinished;
import de.prob.check.RefinementCheckCounterExample;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.ISymbolicResultHandler;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.verifications.AbstractVerificationsResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;

@Singleton
public class SymbolicCheckingResultHandler extends AbstractVerificationsResultHandler implements ISymbolicResultHandler<SymbolicCheckingFormulaItem> {
	
	private final CurrentTrace currentTrace;
	
	@Inject
	public SymbolicCheckingResultHandler(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace) {
		super(stageManager, bundle);
		this.currentTrace = currentTrace;
		this.type = CheckingType.SYMBOLIC_CHECKING;
	}
	
	@Override
	protected boolean isSuccess(final Object result) {
		return result instanceof ModelCheckOk;
	}
	
	@Override
	protected boolean isCounterExample(final Object result) {
		return result instanceof CBCInvariantViolationFound || result instanceof CBCDeadlockFound || result instanceof RefinementCheckCounterExample;
	}
	
	@Override
	protected boolean isInterrupted(final Object result) {
		return result instanceof NotYetFinished || result instanceof CheckInterrupted || result instanceof CommandInterruptedException;
	}
	
	@Override
	protected boolean isParseError(final Object result) {
		return !this.isInterrupted(result) && (result instanceof Throwable || result instanceof CheckError);
	}
	
	@Override
	public void handleFormulaResult(SymbolicCheckingFormulaItem item, Object result) {
		item.setResultItem(handleFormulaResult(result));
		
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
	
	@Override
	public void handleFormulaResult(SymbolicCheckingFormulaItem item, AbstractCommand cmd) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		if(item.getType() == SymbolicExecutionType.SYMBOLIC_MODEL_CHECK) {
			handleSymbolicChecking(item, (SymbolicModelcheckCommand) cmd);
		} else if(item.getType() == SymbolicExecutionType.CHECK_STATIC_ASSERTIONS || item.getType() == SymbolicExecutionType.CHECK_DYNAMIC_ASSERTIONS) {
			handleAssertionChecking(item, (ConstraintBasedAssertionCheckCommand) cmd, stateSpace);
		} else if (item.getType() == SymbolicExecutionType.CHECK_WELL_DEFINEDNESS) {
			handleWellDefinednessChecking(item, (CheckWellDefinednessCommand)cmd);
		} else if(item.getType() == SymbolicExecutionType.CHECK_REFINEMENT) {
			handleRefinementChecking(item, (ConstraintBasedRefinementCheckCommand) cmd);
		} else if(item.getType() == SymbolicExecutionType.FIND_REDUNDANT_INVARIANTS) {
			handleFindRedundantInvariants(item, (GetRedundantInvariantsCommand) cmd);
		}
	}
	
	public void handleFindRedundantInvariants(SymbolicCheckingFormulaItem item, GetRedundantInvariantsCommand cmd) {
		List<String> result = cmd.getRedundantInvariants();
		if (result.isEmpty()) {
			showCheckingResult(item, "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.notFound", Checked.SUCCESS);
		} else {
			final String header = cmd.isTimeout() ? "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.timeout" : "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.found";
			showCheckingResult(item, header, "common.literal", Checked.FAIL, String.join("\n", result));
		}
	}
	
	public void handleWellDefinednessChecking(final SymbolicCheckingFormulaItem item, final CheckWellDefinednessCommand cmd) {
		if (cmd.getDischargedCount().equals(cmd.getTotalCount())) {
			showCheckingResult(item, "verifications.symbolicchecking.resultHandler.wellDefinednessChecking.result.allDischarged.header", "verifications.symbolicchecking.resultHandler.wellDefinednessChecking.result.allDischarged.message", Checked.SUCCESS, cmd.getTotalCount());
		} else {
			showCheckingResult(item, "verifications.symbolicchecking.resultHandler.wellDefinednessChecking.result.undischarged.header", "verifications.symbolicchecking.resultHandler.wellDefinednessChecking.result.undischarged.message", Checked.FAIL, cmd.getDischargedCount(), cmd.getTotalCount(), cmd.getTotalCount().subtract(cmd.getDischargedCount()));
		}
	}
	
	public void handleRefinementChecking(SymbolicCheckingFormulaItem item, ConstraintBasedRefinementCheckCommand cmd) {
		ConstraintBasedRefinementCheckCommand.ResultType result = cmd.getResult();
		String msg = cmd.getResultsString();
		if (result == null) {
			showCheckingResult(item, "verifications.symbolicchecking.resultHandler.refinementChecking.result.notARefinementMachine.message", "verifications.symbolicchecking.resultHandler.refinementChecking.result.notARefinementMachine.header", Checked.FAIL);
		} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.NO_VIOLATION_FOUND) {
			showCheckingResult(item, "verifications.symbolicchecking.resultHandler.refinementChecking.result.noViolationFound", "common.literal", Checked.SUCCESS, msg);
		} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.VIOLATION_FOUND) {
			showCheckingResult(item, "verifications.symbolicchecking.resultHandler.refinementChecking.result.violationFound", "common.literal", Checked.FAIL, msg);
		} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.INTERRUPTED) {
			showCheckingResult(item, "verifications.symbolicchecking.resultHandler.refinementChecking.result.interrupted", "common.literal", Checked.INTERRUPTED, msg);
		}
	}
	
	public void handleAssertionChecking(SymbolicCheckingFormulaItem item, ConstraintBasedAssertionCheckCommand cmd, StateSpace stateSpace) {
		ConstraintBasedAssertionCheckCommand.ResultType result = cmd.getResult();
		switch(result) {
			case NO_COUNTER_EXAMPLE_EXISTS:
				showCheckingResult(item, "verifications.symbolicchecking.resultHandler.assertionChecking.result.noCounterExampleExists", Checked.SUCCESS);
				break;
			case NO_COUNTER_EXAMPLE_FOUND:
				showCheckingResult(item, "verifications.symbolicchecking.resultHandler.assertionChecking.result.noCounterExampleFound", Checked.SUCCESS);
				break;
			case COUNTER_EXAMPLE:
				item.getCounterExamples().add(cmd.getTrace(stateSpace));
				showCheckingResult(item, "verifications.symbolicchecking.resultHandler.assertionChecking.result.counterExampleFound", Checked.FAIL);
				break;
			case INTERRUPTED:
				showCheckingResult(item, "verifications.symbolicchecking.resultHandler.assertionChecking.result.interrupted", Checked.INTERRUPTED);
				break;
			default:
				break;
		}
	}
	
	public void handleSymbolicChecking(SymbolicCheckingFormulaItem item, SymbolicModelcheckCommand cmd) {
		SymbolicModelcheckCommand.ResultType result = cmd.getResult();
		switch(result) {
			case SUCCESSFUL:
				showCheckingResult(item, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.success", Checked.SUCCESS);
				break;
			case COUNTER_EXAMPLE:
				showCheckingResult(item, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.counterExample", Checked.FAIL);
				break;
			case TIMEOUT:
				showCheckingResult(item, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.timeout", Checked.TIMEOUT);
				break;
			case INTERRUPTED:
				showCheckingResult(item, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.interrupted", Checked.INTERRUPTED);
				break;
			case LIMIT_REACHED:
				showCheckingResult(item, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.limitReached", Checked.LIMIT_REACHED);
				break;
			default:
				break;
		}
	}
		
	private void showCheckingResult(SymbolicCheckingFormulaItem item, String header, String msg, Checked checked, Object... messageParams) {
		item.setResultItem(new CheckingResultItem(checked, header, msg, messageParams));
	}
	
	private void showCheckingResult(SymbolicCheckingFormulaItem item, String msg, Checked checked) {
		showCheckingResult(item, msg, msg, checked);
	}
}
