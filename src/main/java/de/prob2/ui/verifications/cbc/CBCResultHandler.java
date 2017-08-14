package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;
import java.util.Arrays;

import de.prob.check.CBCDeadlockFound;
import de.prob.check.CBCInvariantViolationFound;
import de.prob.check.ModelCheckOk;
import de.prob.check.CheckError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;

public class CBCResultHandler extends AbstractResultHandler {
	
	public CBCResultHandler() {
		this.type = CheckingType.CBC;
		this.success.addAll(Arrays.asList(ModelCheckOk.class));
		this.counterExample.addAll(Arrays.asList(CBCInvariantViolationFound.class));
		this.error.addAll(Arrays.asList(CBCDeadlockFound.class, CheckError.class));
		this.exception.addAll(Arrays.asList(CBCParseError.class));
	}
	
	public void showResult(CheckingResultItem resultItem, AbstractCheckableItem item, ArrayList<Trace> traces) {
		super.showResult(resultItem, item);
		//TODO: counter examples
	}
	
	public void handleFormulaResult(CBCFormulaItem item, Object result, State stateid) {
		ArrayList<Trace> traces = new ArrayList<>();
		CheckingResultItem resultItem = handleFormulaResult(result, stateid, traces);
		this.showResult(resultItem, item, traces);
	}

	@Override
	protected Trace handleCounterExample(Object result, State stateid) {
		// TODO Auto-generated method stub
		return null;
	}

}
