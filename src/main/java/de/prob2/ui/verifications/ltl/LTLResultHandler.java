package de.prob2.ui.verifications.ltl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.check.CheckInterrupted;
import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLNotYetFinished;
import de.prob.check.LTLOk;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.AbstractVerificationsResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Singleton
public class LTLResultHandler extends AbstractVerificationsResultHandler {
	
	@Inject
	public LTLResultHandler(final StageManager stageManager, final ResourceBundle bundle) {
		super(stageManager, bundle);	
		this.type = CheckingType.LTL;
	}
	
	@Override
	protected boolean isSuccess(final Object result) {
		return result instanceof LTLOk;
	}
	
	@Override
	protected boolean isCounterExample(final Object result) {
		return result instanceof LTLCounterExample;
	}
	
	@Override
	protected boolean isInterrupted(final Object result) {
		return result instanceof LTLNotYetFinished || result instanceof CheckInterrupted;
	}
	
	@Override
	protected boolean isParseError(final Object result) {
		return result instanceof Throwable || result instanceof LTLError;
	}
	
	public void handleFormulaResult(LTLFormulaItem item, List<LTLMarker> errorMarkers, Object result, StateSpace stateSpace) {
		CheckingResultItem resultItem = handleFormulaResult(result);

		if (result instanceof ITraceDescription) {
			item.setCounterExample(((ITraceDescription)result).getTrace(stateSpace));
		} else {
			item.setCounterExample(null);
		}
		if(isParseError(result)) {
			//errorMarkers contains errors, resultItem only contains errors from the exception
			String errorMessage = errorMarkers.stream().map(LTLMarker::getMsg).collect(Collectors.joining("\n"));
			if(errorMessage.isEmpty()) {
				errorMessage = "Parse Error in typed formula";
			}
			item.setResultItem(new LTLCheckingResultItem(resultItem.getChecked(), errorMarkers, resultItem.getHeaderBundleKey(), resultItem.getMessageBundleKey(), errorMessage));
		} else {
			item.setResultItem(resultItem);
		}
	}
	
	public void handlePatternResult(LTLParseListener parseListener, AbstractCheckableItem item) {
		CheckingResultItem resultItem;
		// Empty Patterns do not have parse errors which is a little bit confusing
		if(parseListener.getErrorMarkers().isEmpty() && !item.getCode().isEmpty()) {
			resultItem = new LTLCheckingResultItem(Checked.SUCCESS, parseListener.getErrorMarkers(), "verifications.result.patternParsedSuccessfully", "verifications.result.patternParsedSuccessfully");
		} else {
			String msg;
			List<LTLMarker> errorMarkers = parseListener.getErrorMarkers();
			if(item.getCode().isEmpty()) {
				msg = bundle.getString("verifications.ltl.pattern.empty");
			} else {
				msg = parseListener.getErrorMarkers().stream().map(LTLMarker::getMsg).collect(Collectors.joining("\n"));
			}
			resultItem = new LTLCheckingResultItem(Checked.PARSE_ERROR, errorMarkers, "verifications.result.couldNotParsePattern.header",
					"common.result.message", msg);
		}
		item.setResultItem(resultItem);
	}
}
