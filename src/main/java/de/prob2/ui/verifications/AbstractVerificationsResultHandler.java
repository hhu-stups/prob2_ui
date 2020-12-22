package de.prob2.ui.verifications;

import de.prob.animator.CommandInterruptedException;
import de.prob.check.IModelCheckingResult;
import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.StageManager;

import java.util.ResourceBundle;

public abstract class AbstractVerificationsResultHandler extends AbstractResultHandler {
	
	protected CheckingType type;
	
	protected AbstractVerificationsResultHandler(final StageManager stageManager, final ResourceBundle bundle) {
		super(stageManager, bundle);
	}
	
	protected abstract boolean isSuccess(final Object result);
	
	protected abstract boolean isCounterExample(final Object result);
	
	protected abstract boolean isInterrupted(final Object result);
	
	protected abstract boolean isParseError(final Object result);
	
	public CheckingResultItem handleFormulaResult(Object result) {
		if(isSuccess(result)) {
			return new CheckingResultItem(Checked.SUCCESS, "verifications.result.succeeded.header",
				"verifications.result.succeeded.message", bundle.getString(type.getKey()));
		} else if(isCounterExample(result)) {
			return new CheckingResultItem(Checked.FAIL, "verifications.result.counterExampleFound.header",
				"verifications.result.counterExampleFound.message", bundle.getString(type.getKey()));
		} else if(isParseError(result)) {
			if(result instanceof Throwable) {
				return new CheckingResultItem(Checked.PARSE_ERROR, "common.result.couldNotParseFormula.header",
						"common.result.message", ((Throwable) result).getMessage());
			} else {
				return new CheckingResultItem(Checked.PARSE_ERROR, "common.result.couldNotParseFormula.header",
						"common.result.message", ((IModelCheckingResult) result).getMessage());
			}
		} else if(isInterrupted(result)) {
			if(result instanceof CommandInterruptedException) {
				return new CheckingResultItem(Checked.INTERRUPTED, "common.result.interrupted.header",
						"common.result.message", ((CommandInterruptedException) result).getMessage());
			} else {
				return new CheckingResultItem(Checked.INTERRUPTED, "common.result.interrupted.header",
						"common.result.message", ((IModelCheckingResult) result).getMessage());
			}

		} else {
			return null;
		}
	}
}
