package de.prob2.ui.verifications.ltl;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

import de.be4.ltl.core.parser.LtlParseException;
import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLOk;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;

@Singleton
public class LTLResultHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLResultHandler.class);
	
	public enum Checked {
		SUCCESS, FAIL;
	}
	
	public static class LTLResultItem {
	
		private AlertType type;
		private Checked checked;
		private String message;
		private String header;
		private String exceptionText;
		private boolean isParseError;
		
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
			this.isParseError = true;
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
		if(resultItem.isParseError) {
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
			resultItem = new LTLResultHandler.LTLResultItem(AlertType.INFORMATION, Checked.SUCCESS, "LTL Check succeeded", "Success");
		} else if(result instanceof LTLCounterExample) {
			trace = ((LTLCounterExample) result).getTrace(stateid.getStateSpace());
			resultItem = new LTLResultHandler.LTLResultItem(AlertType.ERROR, Checked.FAIL, "LTL Counter Example has been found", 
											"Counter Example Found");
		} else if(result instanceof LTLError) {
			resultItem = new LTLResultHandler.LTLResultItem(AlertType.ERROR, Checked.FAIL, ((LTLError) result).getMessage(), 
											"Error while executing formula");
		} else if(result instanceof LtlParseException) {
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				((Throwable) result).printStackTrace(pw);
			}
			resultItem = new LTLResultHandler.LTLResultItem(AlertType.ERROR, Checked.FAIL, "Message: ", "Could not parse formula", 
											sw.toString());
			logger.error("Could not parse LTL formula", result);
		}
		
		this.showResult(resultItem, item, trace);
		if(resultItem != null) {
			return resultItem.getChecked();
		}
		return Checked.FAIL;
	}
}