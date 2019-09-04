package de.prob2.ui.verifications.ltl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.be4.ltl.core.parser.LtlParseException;
import de.prob.animator.command.EvaluationCommand;
import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLNotYetFinished;
import de.prob.check.LTLOk;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.formula.LTLParseError;

@Singleton
public class LTLResultHandler extends AbstractResultHandler {
	
	@Inject
	public LTLResultHandler(final StageManager stageManager, final ResourceBundle bundle) {
		super(stageManager, bundle);	
		this.type = CheckingType.LTL;
		this.success.addAll(Arrays.asList(LTLOk.class));
		this.counterExample.addAll(Arrays.asList(LTLCounterExample.class));
		this.interrupted.addAll(Arrays.asList(LTLNotYetFinished.class));
		this.parseErrors.addAll(Arrays.asList(LTLParseError.class, LtlParseException.class, ProBError.class, LTLError.class));
	}
	
	public Checked handleFormulaResult(LTLFormulaItem item, List<LTLMarker> errorMarkers, Object result, State stateid) {
		if(result instanceof EvaluationCommand)  {
			if (((EvaluationCommand) result).isInterrupted()) {
				item.setChecked(Checked.INTERRUPTED);
				return Checked.INTERRUPTED;
			} else {
				result = ((EvaluationCommand) result).getValue();
			}
		}
		Class<?> clazz = result.getClass();
		if(success.contains(clazz)) {
			item.setChecked(Checked.SUCCESS);
		} else if(parseErrors.contains(clazz)) {
			item.setChecked(Checked.PARSE_ERROR);
		} else if(error.contains(clazz) || counterExample.contains(clazz)) {
			item.setChecked(Checked.FAIL);
		} else {
			item.setChecked(Checked.INTERRUPTED);
		}
		ArrayList<Trace> traces = new ArrayList<>();
		CheckingResultItem resultItem = handleFormulaResult(result, stateid, traces);

		if(!traces.isEmpty()) {
			item.setCounterExample(traces.get(0));
		} else {
			item.setCounterExample(null);
		}
		if(resultItem != null) {
			item.setResultItem(new LTLCheckingResultItem(resultItem.getChecked(), errorMarkers, resultItem.getHeaderBundleKey(), resultItem.getMessageBundleKey(), resultItem.getMessageParams()));
			return resultItem.getChecked();
		}
		return Checked.FAIL;
	}
	
	public void handlePatternResult(LTLParseListener parseListener, AbstractCheckableItem item) {
		CheckingResultItem resultItem = null;
		if(parseListener.getErrorMarkers().isEmpty()) {
			item.setChecked(Checked.SUCCESS);
		} else {
			List<LTLMarker> errorMarkers = parseListener.getErrorMarkers();
			final String msg = parseListener.getErrorMarkers().stream().map(LTLMarker::getMsg).collect(Collectors.joining("\n"));
			resultItem = new LTLCheckingResultItem(Checked.PARSE_ERROR, errorMarkers, "verifications.result.couldNotParsePattern.header",
					"common.result.message", msg);
			item.setChecked(Checked.PARSE_ERROR);
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
