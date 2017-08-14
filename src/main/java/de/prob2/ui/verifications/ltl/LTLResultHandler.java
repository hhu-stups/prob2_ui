package de.prob2.ui.verifications.ltl;

import com.google.inject.Singleton;
import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLOk;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.formula.LTLParseError;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;

@Singleton
public class LTLResultHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLResultHandler.class);
			
	public void showResult(CheckingResultItem resultItem, AbstractCheckableItem item, @Nullable Trace trace) {
		if(resultItem == null) {
			return;
		}
		if(resultItem.getType() != AlertType.ERROR) {
			item.setCheckedSuccessful();
			return;
		} else {
			item.setCheckedFailed();
		}
		Alert alert = new Alert(resultItem.getType(), resultItem.getMessage());
		alert.setTitle(item.getName());
		alert.setHeaderText(resultItem.getHeader());
		if(resultItem.getChecked() == Checked.EXCEPTION) {
			alert.getDialogPane().getStylesheets().add(getClass().getResource("/prob.css").toExternalForm());
			TextArea exceptionText = new TextArea(resultItem.getExceptionText());
			exceptionText.setEditable(false);
			exceptionText.getStyleClass().add("text-area-error");
			StackPane pane = new StackPane(exceptionText);
			pane.setPrefSize(320, 120);
			alert.getDialogPane().setExpandableContent(pane);
			alert.getDialogPane().setExpanded(true);
		}
		alert.showAndWait();
		if(item instanceof LTLFormulaItem) {
			((LTLFormulaItem) item).setCounterExample(trace);
		}
	}
	
	public Checked handleFormulaResult(LTLFormulaItem item, Object result, State stateid) {
		CheckingResultItem resultItem = null;
		Trace trace = null;
		if(result instanceof LTLOk) {
			resultItem = new CheckingResultItem(AlertType.INFORMATION, Checked.SUCCESS, "LTL Formula Check succeeded", "Success");
		} else if(result instanceof LTLCounterExample) {
			trace = ((LTLCounterExample) result).getTrace(stateid.getStateSpace());
			resultItem = new CheckingResultItem(AlertType.ERROR, Checked.FAIL, "LTL Counter Example has been found", 
											"Counter Example Found");
		} else if(result instanceof LTLError) {
			resultItem = new CheckingResultItem(AlertType.ERROR, Checked.FAIL, ((LTLError) result).getMessage(), 
											"Error while executing formula");
		} else if(result instanceof LTLParseError || result instanceof ProBError) {
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				((Throwable) result).printStackTrace(pw);
			}
			resultItem = new CheckingResultItem(AlertType.ERROR, Checked.EXCEPTION, "Message: ", "Could not parse formula", 
											sw.toString());
			logger.error("Could not parse LTL formula", result);			
		}
		
		this.showResult(resultItem, item, trace);
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
}