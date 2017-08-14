package de.prob2.ui.verifications;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.check.IModelCheckingResult;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;

public abstract class AbstractResultHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractResultHandler.class);
	
	protected CheckingType type;
	protected ArrayList<Class<?>> success;
	protected ArrayList<Class<?>> counterExample;
	protected ArrayList<Class<?>> error;
	protected ArrayList<Class<?>> exception;
	
	public AbstractResultHandler() {
		success = new ArrayList<>();
		counterExample = new ArrayList<>();
		error = new ArrayList<>();
		exception = new ArrayList<>();
	}
	
	public void showResult(CheckingResultItem resultItem, AbstractCheckableItem item) {
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
	}
	
	public CheckingResultItem handleFormulaResult(Object result, State stateid, ArrayList<Trace> traces) {
		CheckingResultItem resultItem = null;
		if(success.contains(result.getClass())) {
			resultItem = new CheckingResultItem(AlertType.INFORMATION, Checked.SUCCESS, type.name().concat(" Formula Check succeeded"), "Success");
		} else if(counterExample.contains(result.getClass())) {
			traces.add(handleCounterExample(result, stateid));
			System.out.println(traces.get(0));
			resultItem = new CheckingResultItem(AlertType.ERROR, Checked.FAIL, type.name().concat(" Counter Example has been found"), 
											"Counter Example Found");
		} else if(error.contains(result.getClass())) {
			resultItem = new CheckingResultItem(AlertType.ERROR, Checked.FAIL, ((IModelCheckingResult) result).getMessage(), 
											"Error while executing formula");
		} else if(exception.contains(result.getClass())) {
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				((Throwable) result).printStackTrace(pw);
			}
			resultItem = new CheckingResultItem(AlertType.ERROR, Checked.EXCEPTION, "Message: ", "Could not parse formula", 
											sw.toString());
			logger.error("Could not parse ".concat(type.name().concat(" formula")), result);			
		}
		return resultItem;
	}
	
	protected abstract Trace handleCounterExample(Object result, State stateid);

}
