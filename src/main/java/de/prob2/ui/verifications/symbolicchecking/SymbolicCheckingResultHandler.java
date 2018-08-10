package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.AbstractCommand;
import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.animator.command.SymbolicModelcheckCommand;
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
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;

@Singleton
public class SymbolicCheckingResultHandler extends AbstractResultHandler {
	
	private final CurrentTrace currentTrace;
	
	@Inject
	public SymbolicCheckingResultHandler(final StageManager stageManager, final ResourceBundle bundle, 
										final CurrentTrace currentTrace) {
		super(stageManager, bundle);
		this.currentTrace = currentTrace;
		this.type = CheckingType.SYMBOLIC;
		this.success.addAll(Arrays.asList(ModelCheckOk.class));
		this.counterExample.addAll(Arrays.asList(CBCInvariantViolationFound.class, CBCDeadlockFound.class,
												RefinementCheckCounterExample.class));
		this.error.addAll(Arrays.asList(CBCDeadlockFound.class, CheckError.class));
		this.interrupted.addAll(Arrays.asList(NotYetFinished.class, CheckInterrupted.class));
	}
	
	public void handleFormulaResult(SymbolicCheckingFormulaItem item, Object result, State stateid) {
		Class<?> clazz = result.getClass();
		if(success.contains(clazz)) {
			handleItem(item, Checked.SUCCESS);
		} else if(error.contains(clazz) || counterExample.contains(clazz) || result instanceof Throwable) {
			handleItem(item, Checked.FAIL);
		} else {
			handleItem(item, Checked.INTERRUPTED);
		}
		ArrayList<Trace> traces = new ArrayList<>();
		CheckingResultItem resultItem = handleFormulaResult(result, stateid, traces);
		item.setResultItem(resultItem);
		item.getCounterExamples().clear();
		for(Trace trace: traces) {
			item.getCounterExamples().add(trace);
		}
	}
	
	public void handleFormulaResult(SymbolicCheckingFormulaItem item, AbstractCommand cmd) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		if(item.getType() == SymbolicCheckingType.FIND_VALID_STATE) {
			handleFindValidState(item, (FindStateCommand) cmd, stateSpace);
		} else if(item.getType() == SymbolicCheckingType.TINDUCTION || item.getType() == SymbolicCheckingType.KINDUCTION ||
					item.getType() == SymbolicCheckingType.BMC || item.getType() == SymbolicCheckingType.IC3) {
			handleSymbolicChecking(item, (SymbolicModelcheckCommand) cmd);
		} else if(item.getType() == SymbolicCheckingType.CHECK_ASSERTIONS) {
			handleAssertionChecking(item, (ConstraintBasedAssertionCheckCommand) cmd, stateSpace);
		} else if(item.getType() == SymbolicCheckingType.CHECK_REFINEMENT) {
			handleRefinementChecking(item, (ConstraintBasedRefinementCheckCommand) cmd);
		} else if(item.getType() == SymbolicCheckingType.FIND_REDUNDANT_INVARIANTS) {
			handleFindRedundantInvariants(item, (GetRedundantInvariantsCommand) cmd);
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
	
	public void handleFindValidState(SymbolicCheckingFormulaItem item, FindStateCommand cmd, StateSpace stateSpace) {
		FindStateCommand.ResultType result = cmd.getResult();
		item.setExample(null);
		// noinspection IfCanBeSwitch // Do not replace with switch, because result can be null
		if (result == FindStateCommand.ResultType.STATE_FOUND) {
			showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.findValidState.result.found"), Checked.SUCCESS);
			item.setExample(cmd.getTrace(stateSpace));
		} else if (result == FindStateCommand.ResultType.NO_STATE_FOUND) {
			showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.findValidState.result.notFound"), Checked.FAIL);
		} else if (result == FindStateCommand.ResultType.INTERRUPTED) {
			showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.findValidState.result.interrupted"), Checked.INTERRUPTED);
		} else {
			showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.findValidState.result.error"), Checked.FAIL);
		}
	}
	
	public void handleFindRedundantInvariants(SymbolicCheckingFormulaItem item, GetRedundantInvariantsCommand cmd) {
		List<String> result = cmd.getRedundantInvariants();
		if(cmd.isInterrupted()) {
			showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.interrupted"), Checked.INTERRUPTED);
		} else if (result.isEmpty()) {
			showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.notFound"), Checked.SUCCESS);
		} else {
			final String header = bundle.getString(cmd.isTimeout() ? "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.timeout" : "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.found");
			showCheckingResult(item, String.join("\n", result), header, Checked.FAIL);
		}
	}
	
	public void handleRefinementChecking(SymbolicCheckingFormulaItem item, ConstraintBasedRefinementCheckCommand cmd) {
		ConstraintBasedRefinementCheckCommand.ResultType result = cmd.getResult();
		String msg = cmd.getResultsString();
		if (result == null) {
			showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.refinementChecking.result.notARefinementMachine.message"), bundle.getString("verifications.symbolicchecking.resultHandler.refinementChecking.result.notARefinementMachine.header"), Checked.FAIL);
		} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.NO_VIOLATION_FOUND) {
			showCheckingResult(item, msg, bundle.getString("verifications.symbolicchecking.resultHandler.refinementChecking.result.noViolationFound"), Checked.SUCCESS);
		} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.VIOLATION_FOUND) {
			showCheckingResult(item, msg, bundle.getString("verifications.symbolicchecking.resultHandler.refinementChecking.result.violationFound"), Checked.FAIL);
		} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.INTERRUPTED) {
			showCheckingResult(item, msg, bundle.getString("verifications.symbolicchecking.resultHandler.refinementChecking.result.interrupted"), Checked.INTERRUPTED);
		}
	}
	
	public void handleAssertionChecking(SymbolicCheckingFormulaItem item, ConstraintBasedAssertionCheckCommand cmd, StateSpace stateSpace) {
		ConstraintBasedAssertionCheckCommand.ResultType result = cmd.getResult();
		switch(result) {
			case NO_COUNTER_EXAMPLE_EXISTS:
				showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.assertionChecking.result.noCounterExampleExists"), Checked.SUCCESS);
				break;
			case NO_COUNTER_EXAMPLE_FOUND:
				showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.assertionChecking.result.noCounterExampleFound"), Checked.SUCCESS);
				break;
			case COUNTER_EXAMPLE:
				item.getCounterExamples().add(cmd.getTrace(stateSpace));
				showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.assertionChecking.result.counterExampleFound"), Checked.FAIL);
				break;
			case INTERRUPTED:
				showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.assertionChecking.result.interrupted"), Checked.INTERRUPTED);
				break;
			default:
				break;
		}
	}
	
	public void handleSymbolicChecking(SymbolicCheckingFormulaItem item, SymbolicModelcheckCommand cmd) {
		SymbolicModelcheckCommand.ResultType result = cmd.getResult();
		if(cmd.isInterrupted()) {
			showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.symbolicChecking.result.interrupted"), Checked.INTERRUPTED);
			return;
		}
		switch(result) {
			case SUCCESSFUL:
				showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.symbolicChecking.result.success"), Checked.SUCCESS);
				break;
			case COUNTER_EXAMPLE:
				showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.symbolicChecking.result.counterExample"), Checked.FAIL);
				break;
			case TIMEOUT:
				showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.symbolicChecking.result.timeout"), Checked.TIMEOUT);
				break;
			case INTERRUPTED:
				showCheckingResult(item, bundle.getString("verifications.symbolicchecking.resultHandler.symbolicChecking.result.interrupted"), Checked.INTERRUPTED);
				break;
			default:
				break;
		}
	}
		
	private void showCheckingResult(SymbolicCheckingFormulaItem item, String msg, String header, Checked checked) {
		ArrayList<Trace> traces = new ArrayList<>();
		item.setResultItem(new CheckingResultItem(checked, msg, header));
		item.getCounterExamples().clear();
		for(Trace trace: traces) {
			item.getCounterExamples().add(trace);
		}
		handleItem(item, checked);
	}
	
	private void showCheckingResult(SymbolicCheckingFormulaItem item, String msg, Checked checked) {
		showCheckingResult(item, msg, msg, checked);
	}
	

}
