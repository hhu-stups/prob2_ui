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
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;

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
			showResultForSearchingValidState("Error when searching valid state for predicate", false);
		} else if(result == ResultType.STATE_FOUND) {
			showResultForSearchingValidState("State found", true);
			item.setExample(cmd.getTrace(stateSpace));
		} else if(result == ResultType.NO_STATE_FOUND) {
			showResultForSearchingValidState("State not found", false);
		} else if(result == ResultType.INTERRUPTED) {
			showResultForSearchingValidState("Searching valid state for predicate is interrupted", false);
		} else {
			showResultForSearchingValidState("Error when searching valid state for predicate", false);
		}
	}
	
	public void handleRefinementChecking(ConstraintBasedRefinementCheckCommand cmd) {
		ConstraintBasedRefinementCheckCommand.ResultType result = cmd.getResult();
		String msg = cmd.getResultsString();
		if(result == null) {
			showRefinementCheckingResult("Refinement checking failed", "Not a refinement machine", false);
		} else if(result == ConstraintBasedRefinementCheckCommand.ResultType.NO_VIOLATION_FOUND) {
			showRefinementCheckingResult("Violation not found", msg, true);
		} else if(result == ConstraintBasedRefinementCheckCommand.ResultType.VIOLATION_FOUND) {
			showRefinementCheckingResult("Violation found", msg, false);
		} else {
			showRefinementCheckingResult("Refinement checking is interrupted", msg, false);
		}
	}
	
	public void handleAssertionChecking(ConstraintBasedAssertionCheckCommand cmd) {
		ConstraintBasedAssertionCheckCommand.ResultType result = cmd.getResult();
		if(result == ConstraintBasedAssertionCheckCommand.ResultType.NO_COUNTER_EXAMPLE_EXISTS) {
			showAssertionCheckingResult("No counter-example exists", true);
		} else if(result == ConstraintBasedAssertionCheckCommand.ResultType.NO_COUNTER_EXAMPLE_FOUND) {
			showAssertionCheckingResult("No counter-example found", true);
		} else if(result == ConstraintBasedAssertionCheckCommand.ResultType.COUNTER_EXAMPLE) {
			showAssertionCheckingResult("Counter-example found", false);
		} else {
			showAssertionCheckingResult("Assertion checking is interrupted", false);
		}
	}
	
	public void showRefinementCheckingResult(String header, String msg, boolean successful) {
		showAlert("Constraint Based Refinement Checking", header, msg, successful);
	}
	
	public void showAssertionCheckingResult(String msg, boolean successful) {
		showAlert("Checking assertions", msg, msg, successful);
	}
		
	public void showResultForSearchingValidState(String msg, boolean found) {
		showAlert("Find Valid State Satisfying Predicate", msg, msg, found);
	}
	
	private void showAlert(String title, String header, String msg, boolean successful) {
		Alert alert;
		if(successful) {
			alert = new Alert(AlertType.INFORMATION);
		} else {
			alert = new Alert(AlertType.ERROR);
		}
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(msg.length() > 0 ? msg : header);
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
	}


}
