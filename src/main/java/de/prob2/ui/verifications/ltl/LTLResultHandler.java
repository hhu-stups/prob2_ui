package de.prob2.ui.verifications.ltl;

import com.google.inject.Singleton;

import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLOk;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.formula.LTLParseError;
import javafx.scene.control.Alert.AlertType;

import java.util.ArrayList;
import java.util.Arrays;

@Singleton
public class LTLResultHandler extends AbstractResultHandler {
		
	public LTLResultHandler() {
		this.type = CheckingType.LTL;
		this.success.addAll(Arrays.asList(LTLOk.class));
		this.counterExample.addAll(Arrays.asList(LTLCounterExample.class));
		this.error.addAll(Arrays.asList(LTLError.class));
		this.exception.addAll(Arrays.asList(LTLParseError.class, ProBError.class));
	}
			
	public void showResult(CheckingResultItem resultItem, AbstractCheckableItem item, ArrayList<Trace> traces) {
		super.showResult(resultItem, item);
		if(!traces.isEmpty()) {
			((LTLFormulaItem) item).setCounterExample(traces.get(0));
		}
	}
	
	public Checked handleFormulaResult(LTLFormulaItem item, Object result, State stateid) {
		ArrayList<Trace> traces = new ArrayList<>();
		CheckingResultItem resultItem = handleFormulaResult(result, stateid, traces);
		this.showResult(resultItem, item, traces);
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
			StringBuilder msg = new StringBuilder();
			for (LTLMarker marker: parseListener.getErrorMarkers()) {
				msg.append(marker.getMsg()+ "\n");
			}
			resultItem = new CheckingResultItem(AlertType.ERROR, Checked.EXCEPTION, "Message: ", "Could not parse pattern", msg.toString());
			item.setCheckedFailed();
		}
		if(!byInit) {
			this.showResult(resultItem, item, null);
		}
	}

	@Override
	protected Trace handleCounterExample(Object result, State stateid) {
		return ((LTLCounterExample) result).getTrace(stateid.getStateSpace());
	}
}