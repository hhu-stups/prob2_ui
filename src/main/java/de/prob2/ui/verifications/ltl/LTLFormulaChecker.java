package de.prob2.ui.verifications.ltl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import de.be4.ltl.core.parser.LtlParseException;
import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.LTL;
import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLOk;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;

public class LTLFormulaChecker {
	
	public enum Checked {
		SUCCESS, FAIL;
	}
	
	public class LTLResultItem {
		
		private AlertType type;
		private Checked checked;
		private String message;
		private String header;
		private String exceptionText;
		private boolean isParseError;
		
		private LTLResultItem(AlertType type, Checked checked, String message, String header) {
			this.type = type;
			this.checked = checked;
			this.message = message;
			this.header = header;
		}
		
		private LTLResultItem(AlertType type, Checked checked, String message, String header, 
								String exceptionText) {
			this(type, checked, message, header);
			this.exceptionText = exceptionText;
			this.isParseError = true;
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(LTLFormulaChecker.class);
	
	private final CurrentTrace currentTrace;
	
	private final Injector injector;
	
	@Inject
	private LTLFormulaChecker(final CurrentTrace currentTrace, final Injector injector) {
		this.currentTrace = currentTrace;
		this.injector = injector;
	}
	
	public void checkMachine(Machine machine) {
		ArrayList<Boolean> success = new ArrayList<>();
		success.add(true);
		machine.getFormulas().forEach(item-> {
			if(this.checkFormula(item) == Checked.FAIL) {
				machine.setCheckedFailed();
				success.set(0, false);
			}
		});
		if(success.get(0)) {
			machine.setCheckedSuccessful();
		}
	}
	
	public Checked checkFormula(LTLFormulaItem item) {
		LTL formula = null;
		LTLResultItem resultItem = null;
		Trace trace = null;
		try {
			formula = new LTL(item.getFormula());
			if (currentTrace != null) {
				State stateid = currentTrace.getCurrentState();
				EvaluationCommand lcc = formula.getCommand(stateid);
				currentTrace.getStateSpace().execute(lcc);
				AbstractEvalResult result = lcc.getValue();
				if(result instanceof LTLOk) {
					resultItem = new LTLResultItem(AlertType.INFORMATION, Checked.SUCCESS, "LTL Check succeeded", "Success");
				} else if(result instanceof LTLCounterExample) {
					trace = ((LTLCounterExample) result).getTrace(stateid.getStateSpace());
					resultItem = new LTLResultItem(AlertType.ERROR, Checked.FAIL, "LTL Counter Example has been found", 
													"Counter Example Found");
				} else if(result instanceof LTLError) {
					resultItem = new LTLResultItem(AlertType.ERROR, Checked.FAIL, ((LTLError) result).getMessage(), 
													"Error while executing formula");
				}
			}
		} catch (LtlParseException e) {
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				e.printStackTrace(pw);
			}
			resultItem = new LTLResultItem(AlertType.ERROR, Checked.FAIL, "Message: ", "Could not parse formula", 
											sw.toString());
			logger.error("Could not parse LTL formula", e);
		}
		showResult(resultItem, item, trace);
		injector.getInstance(LTLView.class).refreshFormula();
		if(resultItem != null) {
			return resultItem.checked;
		}
		return Checked.FAIL;
	}
		
	private void showResult(LTLResultItem resultItem, LTLFormulaItem item, @Nullable Trace trace) {
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
		item.setCounterExample(trace);
	}

}
