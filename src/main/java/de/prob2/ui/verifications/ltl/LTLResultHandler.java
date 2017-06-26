package de.prob2.ui.verifications.ltl;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLOk;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.formula.LTLParseError;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;

@Singleton
public class LTLResultHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLResultHandler.class);
	
	public enum Checked {
		SUCCESS, FAIL, EXCEPTION;
	}
	
	public static class LTLResultItem {
	
		private AlertType type;
		private Checked checked;
		private String message;
		private String header;
		private String exceptionText;
		
		public LTLResultItem(AlertType type, Checked checked, String message, String header) {
			this.type = type;
			this.checked = checked;
			this.message = message;
			this.header = header;
		}
		
		public LTLResultItem(AlertType type, Checked checked, String message, String header, 
								String exceptionText) {
			this(type, checked, message, header);
			this.exceptionText = exceptionText;
		}
		
		public Checked getChecked() {
			return checked;
		}
	
	}
	
	public void showResult(LTLResultItem resultItem, LTLCheckableItem item, @Nullable Trace trace) {
		if(resultItem == null) {
			return;
		}
		Alert alert = new Alert(resultItem.type, resultItem.message);
		alert.setTitle(item.getName());
		alert.setHeaderText(resultItem.header);
		if(resultItem.checked == Checked.EXCEPTION) {
			alert.getDialogPane().getStylesheets().add(getClass().getResource("/prob.css").toExternalForm());
			TextArea exceptionText = new TextArea(resultItem.exceptionText);
			exceptionText.setEditable(false);
			exceptionText.getStyleClass().add("text-area-error");
			StackPane pane = new StackPane(exceptionText);
			pane.setPrefSize(320, 120);
			alert.getDialogPane().setExpandableContent(pane);
			alert.getDialogPane().setExpanded(true);
		}
		alert.showAndWait();
		if(resultItem.type != AlertType.ERROR) {
			item.setCheckedSuccessful();
		} else {
			item.setCheckedFailed();
		}
		if(item instanceof LTLFormulaItem) {
			((LTLFormulaItem) item).setCounterExample(trace);
		}
	}
	
	public Checked handleFormulaResult(LTLFormulaItem item, Object result, State stateid) {
		LTLResultItem resultItem = null;
		Trace trace = null;
		if(result instanceof LTLOk) {
			resultItem = new LTLResultItem(AlertType.INFORMATION, Checked.SUCCESS, "LTL Formula Check succeeded", "Success");
		} else if(result instanceof LTLCounterExample) {
			trace = ((LTLCounterExample) result).getTrace(stateid.getStateSpace());
			resultItem = new LTLResultItem(AlertType.ERROR, Checked.FAIL, "LTL Counter Example has been found", 
											"Counter Example Found");
		} else if(result instanceof LTLError) {
			resultItem = new LTLResultItem(AlertType.ERROR, Checked.FAIL, ((LTLError) result).getMessage(), 
											"Error while executing formula");
		} else if(result instanceof LTLParseError) {
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				((Throwable) result).printStackTrace(pw);
			}
			resultItem = new LTLResultItem(AlertType.ERROR, Checked.EXCEPTION, "Message: ", "Could not parse formula", 
											sw.toString());
			logger.error("Could not parse LTL formula", result);
		}
		
		this.showResult(resultItem, item, trace);
		if(resultItem != null) {
			return resultItem.getChecked();
		}
		return Checked.FAIL;
	}
	
	public void handlePatternResult(LTLParseListener parseListener, LTLCheckableItem item, boolean byInit) {
		LTLResultItem resultItem = null;
		if(parseListener.getErrorMarkers().size() == 0) {
			resultItem = new LTLResultItem(AlertType.INFORMATION, Checked.SUCCESS, "Parsing LTL Pattern succeeded", "Success");
			item.setCheckedSuccessful();
		} else {
			StringBuilder msg = new StringBuilder();
			for (LTLMarker marker: parseListener.getErrorMarkers()) {
				msg.append(marker.getMsg()+ "\n");
			}
			resultItem = new LTLResultItem(AlertType.ERROR, Checked.EXCEPTION, "Message: ", "Could not parse pattern", msg.toString());
			item.setCheckedFailed();
		}
		if(!byInit) {
			this.showResult(resultItem, item, null);
		}
	}
}