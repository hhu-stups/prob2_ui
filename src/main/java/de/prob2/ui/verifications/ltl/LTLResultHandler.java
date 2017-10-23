package de.prob2.ui.verifications.ltl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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
import javafx.application.Platform;
import javafx.scene.control.Alert;

@Singleton
public class LTLResultHandler extends AbstractResultHandler {
	@Inject
	public LTLResultHandler(final StageManager stageManager, final ResourceBundle bundle) {
		super(stageManager, bundle);
		
		this.type = CheckingType.LTL;
		this.success.addAll(Arrays.asList(LTLOk.class));
		this.counterExample.addAll(Arrays.asList(LTLCounterExample.class));
		this.error.addAll(Arrays.asList(LTLError.class));
		this.exception.addAll(Arrays.asList(LTLParseError.class, ProBError.class));
		this.interrupted.addAll(Arrays.asList(LTLNotYetFinished.class));
	}
	
	public Checked handleFormulaResult(LTLFormulaItem item, Object result, State stateid) {
		Class<?> clazz = result.getClass();
		if(success.contains(clazz)) {
			handleItem(item, Checked.SUCCESS);
		} else if(error.contains(clazz) || counterExample.contains(clazz) || exception.contains(clazz)) {
			handleItem(item, Checked.FAIL);
		} else {
			handleItem(item, Checked.INTERRUPTED);
		}
		ArrayList<Trace> traces = new ArrayList<>();
		CheckingResultItem resultItem = handleFormulaResult(result, stateid, traces);
		Platform.runLater(() -> {
			item.setResultItem(resultItem);	
			if(traces != null && !traces.isEmpty()) {
				((LTLFormulaItem) item).setCounterExample(traces.get(0));
			}
		});
		if(resultItem != null) {
			return resultItem.getChecked();
		}
		return Checked.FAIL;
	}
	
	public void handlePatternResult(LTLParseListener parseListener, AbstractCheckableItem item, boolean byInit) {
		CheckingResultItem resultItem = null;
		if(parseListener.getErrorMarkers().isEmpty()) {
			item.setCheckedSuccessful();
		} else {
			final String msg = parseListener.getErrorMarkers().stream().map(LTLMarker::getMsg).collect(Collectors.joining("\n"));
			resultItem = new CheckingResultItem(Alert.AlertType.ERROR, Checked.EXCEPTION, bundle.getString("verifications.result.couldNotParsePattern.message"), bundle.getString("verifications.result.couldNotParsePattern.header"), msg);
			item.setCheckedFailed();
		}
		if(!byInit) {
			item.setResultItem(resultItem);
		}
	}

	@Override
	protected List<Trace> handleCounterExample(Object result, State stateid) {
		ArrayList<Trace> counterExamples = new ArrayList<>();
		counterExamples.add(((LTLCounterExample) result).getTrace(stateid.getStateSpace()));
		return counterExamples;
	}
}
