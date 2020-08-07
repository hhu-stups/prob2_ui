package de.prob2.ui.verifications.ltl;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLNotYetFinished;
import de.prob.check.LTLOk;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.AbstractVerificationsResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;

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
		return result instanceof LTLNotYetFinished;
	}
	
	@Override
	protected boolean isParseError(final Object result) {
		return result instanceof Throwable || result instanceof LTLError;
	}
	
	public void handleFormulaResult(LTLFormulaItem item, List<LTLMarker> errorMarkers, Object result, State stateid) {
		ArrayList<Trace> traces = new ArrayList<>();
		CheckingResultItem resultItem = handleFormulaResult(result, stateid, traces);

		if(!traces.isEmpty()) {
			item.setCounterExample(traces.get(0));
		} else {
			item.setCounterExample(null);
		}
		if(item.getChecked() == Checked.PARSE_ERROR) {
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
		if(parseListener.getErrorMarkers().isEmpty()) {
			resultItem = new LTLCheckingResultItem(Checked.SUCCESS, parseListener.getErrorMarkers(), "verifications.result.patternParsedSuccessfully", "verifications.result.patternParsedSuccessfully");
		} else {
			List<LTLMarker> errorMarkers = parseListener.getErrorMarkers();
			final String msg = parseListener.getErrorMarkers().stream().map(LTLMarker::getMsg).collect(Collectors.joining("\n"));
			resultItem = new LTLCheckingResultItem(Checked.PARSE_ERROR, errorMarkers, "verifications.result.couldNotParsePattern.header",
					"common.result.message", msg);
		}
		item.setResultItem(resultItem);
	}

	@Override
	protected List<Trace> handleCounterExample(Object result, State stateid) {
		ArrayList<Trace> counterExamples = new ArrayList<>();
		counterExamples.add(((LTLCounterExample) result).getTrace(stateid.getStateSpace()));
		return counterExamples;
	}
}
