package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.inject.Singleton;

import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.command.FindStateCommand.ResultType;
import de.prob.check.CBCDeadlockFound;
import de.prob.check.CBCInvariantViolationFound;
import de.prob.check.CheckError;
import de.prob.check.ModelCheckOk;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;
import javafx.scene.control.Alert.AlertType;

@Singleton
public class CBCResultHandler extends AbstractResultHandler {
	
	public CBCResultHandler() {
		this.type = CheckingType.CBC;
		this.success.addAll(Arrays.asList(ModelCheckOk.class));
		this.counterExample.addAll(Arrays.asList(CBCInvariantViolationFound.class, CBCDeadlockFound.class));
		this.error.addAll(Arrays.asList(CBCDeadlockFound.class, CheckError.class));
		this.exception.addAll(Arrays.asList(CBCParseError.class));
	}
	
	public void showResult(CheckingResultItem resultItem, AbstractCheckableItem item, List<Trace> traces) {
		super.showResult(resultItem, item);
		((CBCFormulaItem) item).getCounterExamples().clear();
		for(Trace trace: traces) {
			((CBCFormulaItem) item).getCounterExamples().add(trace);
		}
	}
	
	public void handleFormulaResult(CBCFormulaItem item, Object result, State stateid) {
		if(result instanceof ModelCheckOk) {
			handleItem(item, true);
		} else {
			handleItem(item, false);
		}
		ArrayList<Trace> traces = new ArrayList<>();
		CheckingResultItem resultItem = handleFormulaResult(result, stateid, traces);
		this.showResult(resultItem, item, traces);
	}

	@Override
	protected List<Trace> handleCounterExample(Object result, State stateid) {
		if(result instanceof CBCInvariantViolationFound) {
			return handleInvariantCounterExamples(result, stateid);
		}
		return handleDeadlockCounterExample(result, stateid);
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
	
	public void handleFindValidState(CBCFormulaItem item, FindStateCommand cmd, StateSpace stateSpace) {
		ResultType result = cmd.getResult();
		item.setExample(null);
		if(result == null) {
			showCheckingResult(item, "Error when searching valid state for predicate", false);
		} else if(result == ResultType.STATE_FOUND) {
			showCheckingResult(item, "State found", true);
			item.setExample(cmd.getTrace(stateSpace));
		} else if(result == ResultType.NO_STATE_FOUND) {
			showCheckingResult(item, "State not found", false);
		} else if(result == ResultType.INTERRUPTED) {
			showCheckingResult(item, "Searching valid state for predicate is interrupted", false);
		} else {
			showCheckingResult(item, "Error when searching valid state for predicate", false);
		}
	}
	
	public void handleRefinementChecking(CBCFormulaItem item, ConstraintBasedRefinementCheckCommand cmd) {
		ConstraintBasedRefinementCheckCommand.ResultType result = cmd.getResult();
		String msg = cmd.getResultsString();
		if(result == null) {
			showCheckingResult(item, "Not a refinement machine", "Refinement checking failed", false);
		} else if(result == ConstraintBasedRefinementCheckCommand.ResultType.NO_VIOLATION_FOUND) {
			showCheckingResult(item, msg, "Violation not found", true);
		} else if(result == ConstraintBasedRefinementCheckCommand.ResultType.VIOLATION_FOUND) {
			showCheckingResult(item, msg, "Violation found", false);
		} else {
			showCheckingResult(item, msg, "Refinement checking is interrupted", false);
		}
	}
	
	public void handleAssertionChecking(CBCFormulaItem item, ConstraintBasedAssertionCheckCommand cmd) {
		ConstraintBasedAssertionCheckCommand.ResultType result = cmd.getResult();
		if(result == ConstraintBasedAssertionCheckCommand.ResultType.NO_COUNTER_EXAMPLE_EXISTS) {
			showCheckingResult(item, "No counter-example exists", true);
		} else if(result == ConstraintBasedAssertionCheckCommand.ResultType.NO_COUNTER_EXAMPLE_FOUND) {
			showCheckingResult(item, "No counter-example found", true);
		} else if(result == ConstraintBasedAssertionCheckCommand.ResultType.COUNTER_EXAMPLE) {
			showCheckingResult(item, "Counter-example found", false);
		} else {
			showCheckingResult(item, "Assertion checking is interrupted", false);
		}
	}
		
	private void showCheckingResult(CBCFormulaItem item, String msg, String header, boolean successful) {
		AlertType alertType = successful ? AlertType.INFORMATION: AlertType.ERROR;
		Checked checked = successful ? Checked.SUCCESS: Checked.FAIL;
		CheckingResultItem resultItem = new CheckingResultItem(alertType , checked, msg, header);
		super.showResult(resultItem, item);
		handleItem(item, successful);
	}
	
	private void showCheckingResult(CBCFormulaItem item, String msg, boolean successful) {
		showCheckingResult(item, msg, msg, successful);
	}
	
	private void handleItem(CBCFormulaItem item, boolean successful) {
		if(successful) {
			item.setCheckedSuccessful();
			item.setChecked(Checked.SUCCESS);
		} else {
			item.setCheckedFailed();
			item.setChecked(Checked.FAIL);
		}
	}


}
