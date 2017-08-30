package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.inject.Singleton;

import de.prob.check.CBCDeadlockFound;
import de.prob.check.CBCInvariantViolationFound;
import de.prob.check.CheckError;
import de.prob.check.ModelCheckOk;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;

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

}
