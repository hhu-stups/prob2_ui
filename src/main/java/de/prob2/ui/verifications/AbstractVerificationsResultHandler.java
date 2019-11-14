package de.prob2.ui.verifications;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import de.prob.check.IModelCheckingResult;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.StageManager;

public abstract class AbstractVerificationsResultHandler extends AbstractResultHandler {
	
	protected CheckingType type;
	protected ArrayList<Class<?>> success;
	protected ArrayList<Class<?>> counterExample;
	protected ArrayList<Class<?>> error;
	protected ArrayList<Class<?>> interrupted;
	protected ArrayList<Class<?>> parseErrors;
	
	protected AbstractVerificationsResultHandler(final StageManager stageManager, final ResourceBundle bundle) {
		super(stageManager, bundle);
		success = new ArrayList<>();
		counterExample = new ArrayList<>();
		error = new ArrayList<>();
		interrupted = new ArrayList<>();
		parseErrors = new ArrayList<>();
	}
	
	public CheckingResultItem handleFormulaResult(Object result, State stateid, List<Trace> traces) {
		if(success.contains(result.getClass())) {
			return new CheckingResultItem(Checked.SUCCESS, "verifications.result.succeeded.header",
				"verifications.result.succeeded.message", bundle.getString(type.getKey()));
		} else if(counterExample.contains(result.getClass())) {
			traces.addAll(handleCounterExample(result, stateid));
			return new CheckingResultItem(Checked.FAIL, "verifications.result.counterExampleFound.header",
				"verifications.result.counterExampleFound.message", bundle.getString(type.getKey()));
		} else if(error.contains(result.getClass())) {
			return new CheckingResultItem(Checked.FAIL, "common.result.error.header",
				"common.result.message", ((IModelCheckingResult) result).getMessage());
		} else if(result instanceof Throwable || parseErrors.contains(result.getClass())) {
			return new CheckingResultItem(Checked.PARSE_ERROR, "common.result.couldNotParseFormula.header",
				"common.result.message", result);
		} else if(interrupted.contains(result.getClass())) {
			return new CheckingResultItem(Checked.INTERRUPTED, "common.result.interrupted.header",
				"common.result.message", ((IModelCheckingResult) result).getMessage());
		} else {
			return null;
		}
	}
	
	protected abstract List<Trace> handleCounterExample(Object result, State stateid);
	
}
