package de.prob2.ui.verifications.cbc;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.check.CBCDeadlockFound;
import de.prob.check.CBCInvariantViolationFound;
import de.prob.check.ModelCheckOk;
import de.prob.check.CheckError;
import de.prob.check.IModelCheckingResult;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class CBCResultHandler {
	
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
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
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
		/*if(item instanceof LTLFormulaItem) {
			((LTLFormulaItem) item).setCounterExample(trace);
		}*/
	}
	
	public void handleFormulaResult(CBCFormulaItem item, Object result, State stateid) {
		CheckingResultItem resultItem = null;
		Trace trace = null;
		if(result instanceof ModelCheckOk) {
			resultItem = new CheckingResultItem(AlertType.INFORMATION, Checked.SUCCESS, "CBC Formula Check succeeded", "Success");
		} else if(result instanceof CBCInvariantViolationFound) {
			//TODO: counter examples
			trace = ((CBCInvariantViolationFound) result).getTrace(stateid.getStateSpace());
			resultItem = new CheckingResultItem(AlertType.ERROR, Checked.FAIL, "CBC Invariant Counter Example has been found", 
											"Counter Example Found");
		} else if(result instanceof CBCDeadlockFound || result instanceof CheckError) {
			resultItem = new CheckingResultItem(AlertType.ERROR, Checked.FAIL, ((IModelCheckingResult) result).getMessage(), 
											"Error while executing formula");
		} else if(result instanceof CBCParseError) {
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				((Throwable) result).printStackTrace(pw);
			}
			resultItem = new CheckingResultItem(AlertType.ERROR, Checked.EXCEPTION, "Message: ", "Could not parse formula", 
											sw.toString());
			logger.error("Could not parse CBC formula", result);			
		}
		
		this.showResult(resultItem, item, trace);
	}

}
