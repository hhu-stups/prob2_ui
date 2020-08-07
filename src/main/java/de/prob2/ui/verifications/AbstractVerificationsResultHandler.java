package de.prob2.ui.verifications;

import java.util.List;
import java.util.ResourceBundle;

import de.prob.check.IModelCheckingResult;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.StageManager;

public abstract class AbstractVerificationsResultHandler extends AbstractResultHandler {
	
	protected CheckingType type;
	
	protected AbstractVerificationsResultHandler(final StageManager stageManager, final ResourceBundle bundle) {
		super(stageManager, bundle);
	}
	
	protected abstract boolean isSuccess(final Object result);
	
	protected abstract boolean isCounterExample(final Object result);
	
	protected abstract boolean isError(final Object result);
	
	protected abstract boolean isInterrupted(final Object result);
	
	protected abstract boolean isParseError(final Object result);
	
	public CheckingResultItem handleFormulaResult(Object result, State stateid, List<Trace> traces) {
		if(isSuccess(result)) {
			return new CheckingResultItem(Checked.SUCCESS, "verifications.result.succeeded.header",
				"verifications.result.succeeded.message", bundle.getString(type.getKey()));
		} else if(isCounterExample(result)) {
			traces.addAll(handleCounterExample(result, stateid));
			return new CheckingResultItem(Checked.FAIL, "verifications.result.counterExampleFound.header",
				"verifications.result.counterExampleFound.message", bundle.getString(type.getKey()));
		} else if(isError(result)) {
			return new CheckingResultItem(Checked.FAIL, "common.result.error.header",
				"common.result.message", ((IModelCheckingResult) result).getMessage());
		} else if(isParseError(result)) {
			if(result instanceof Throwable) {
				return new CheckingResultItem(Checked.PARSE_ERROR, "common.result.couldNotParseFormula.header",
						"common.result.message", ((Throwable) result).getMessage());
			} else {
				return new CheckingResultItem(Checked.PARSE_ERROR, "common.result.couldNotParseFormula.header",
						"common.result.message", ((IModelCheckingResult) result).getMessage());
			}
		} else if(isInterrupted(result)) {
			return new CheckingResultItem(Checked.INTERRUPTED, "common.result.interrupted.header",
				"common.result.message", ((IModelCheckingResult) result).getMessage());
		} else {
			return null;
		}
	}
	
	protected abstract List<Trace> handleCounterExample(Object result, State stateid);
	
}
