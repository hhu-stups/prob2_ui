package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.Arrays;
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
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.check.CBCDeadlockFound;
import de.prob.check.CBCInvariantViolationFound;
import de.prob.check.CheckError;
import de.prob.check.CheckInterrupted;
import de.prob.check.ModelCheckOk;
import de.prob.check.NotYetFinished;
import de.prob.check.RefinementCheckCounterExample;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.ISymbolicResultHandler;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicItem;
import de.prob2.ui.verifications.AbstractVerificationsResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;

@Singleton
public class SymbolicCheckingResultHandler extends AbstractVerificationsResultHandler implements ISymbolicResultHandler {
	
	private final CurrentTrace currentTrace;
	
	@Inject
	public SymbolicCheckingResultHandler(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace) {
		super(stageManager, bundle);
		this.currentTrace = currentTrace;
		this.type = CheckingType.SYMBOLIC_CHECKING;
		this.success.addAll(Arrays.asList(ModelCheckOk.class));
		this.counterExample.addAll(Arrays.asList(CBCInvariantViolationFound.class, CBCDeadlockFound.class,
												RefinementCheckCounterExample.class));
		this.interrupted.addAll(Arrays.asList(NotYetFinished.class, CheckInterrupted.class, CommandInterruptedException.class));
		this.parseErrors.addAll(Arrays.asList(CheckError.class, EvaluationException.class));
	}
	
	public void handleFormulaResult(SymbolicItem item, Object result) {
		ArrayList<Trace> traces = new ArrayList<>();
		CheckingResultItem resultItem = handleFormulaResult(result, currentTrace.getCurrentState(), traces);
		item.setResultItem(resultItem);
		((SymbolicCheckingFormulaItem) item).getCounterExamples().clear();
		for(Trace trace: traces) {
			((SymbolicCheckingFormulaItem) item).getCounterExamples().add(trace);
		}
	}
	
	public void handleFormulaResult(SymbolicItem item, AbstractCommand cmd) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		if(item.getType() == SymbolicExecutionType.TINDUCTION || item.getType() == SymbolicExecutionType.KINDUCTION ||
					item.getType() == SymbolicExecutionType.BMC || item.getType() == SymbolicExecutionType.IC3) {
			handleSymbolicChecking((SymbolicCheckingFormulaItem) item, (SymbolicModelcheckCommand) cmd);
		} else if(item.getType() == SymbolicExecutionType.CHECK_STATIC_ASSERTIONS || item.getType() == SymbolicExecutionType.CHECK_DYNAMIC_ASSERTIONS) {
			handleAssertionChecking((SymbolicCheckingFormulaItem) item, (ConstraintBasedAssertionCheckCommand) cmd, stateSpace);
		} else if (item.getType() == SymbolicExecutionType.CHECK_WELL_DEFINEDNESS) {
			handleWellDefinednessChecking((SymbolicCheckingFormulaItem)item, (CheckWellDefinednessCommand)cmd);
		} else if(item.getType() == SymbolicExecutionType.CHECK_REFINEMENT) {
			handleRefinementChecking((SymbolicCheckingFormulaItem) item, (ConstraintBasedRefinementCheckCommand) cmd);
		} else if(item.getType() == SymbolicExecutionType.FIND_REDUNDANT_INVARIANTS) {
			handleFindRedundantInvariants((SymbolicCheckingFormulaItem) item, (GetRedundantInvariantsCommand) cmd);
		}
	}

	@Override
	protected List<Trace> handleCounterExample(Object result, State stateid) {
		if(result instanceof CBCInvariantViolationFound) {
			return handleInvariantCounterExamples(result, stateid);
		} else if(result instanceof CBCDeadlockFound) {
			return handleDeadlockCounterExample(result, stateid);
		}
		return handleRefinementCounterExample(result, stateid);
	}
	
	private List<Trace> handleInvariantCounterExamples(Object result, State stateid) {
		ArrayList<Trace> counterExamples = new ArrayList<>();
		CBCInvariantViolationFound violation = (CBCInvariantViolationFound) result;
		int size = violation.getCounterexamples().size();
		for(int i = 0; i < size; i++) {
			counterExamples.add(violation.getTrace(i, stateid.getStateSpace()));
		}
		return counterExamples;
	}
	
	private List<Trace> handleDeadlockCounterExample(Object result, State stateid) {
		ArrayList<Trace> counterExamples = new ArrayList<>();
		counterExamples.add(((CBCDeadlockFound) result).getTrace(stateid.getStateSpace()));
		return counterExamples;
	}
	
	private List<Trace> handleRefinementCounterExample(Object result, State stateid) {
		ArrayList<Trace> counterExamples = new ArrayList<>();
		counterExamples.add(((RefinementCheckCounterExample) result).getTrace(stateid.getStateSpace()));
		return counterExamples;
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
