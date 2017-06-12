package de.prob2.ui.verifications.ltl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

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
import de.prob2.ui.verifications.ltl.LTLResultHandler.Checked;
import javafx.scene.control.Alert.AlertType;

public class LTLFormulaChecker {
			
	private static final Logger logger = LoggerFactory.getLogger(LTLFormulaChecker.class);
	
	private final CurrentTrace currentTrace;
	
	private final Injector injector;
	
	private final LTLResultHandler resultHandler;
	
	@Inject
	private LTLFormulaChecker(final CurrentTrace currentTrace, final Injector injector, final LTLResultHandler resultHandler) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.resultHandler = resultHandler;
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
		LTLResultHandler.LTLResultItem resultItem = null;
		Trace trace = null;
		try {
			formula = new LTL(item.getFormula());
			if (currentTrace != null) {
				State stateid = currentTrace.getCurrentState();
				EvaluationCommand lcc = formula.getCommand(stateid);
				currentTrace.getStateSpace().execute(lcc);
				AbstractEvalResult result = lcc.getValue();
				if(result instanceof LTLOk) {
					resultItem = new LTLResultHandler.LTLResultItem(AlertType.INFORMATION, Checked.SUCCESS, "LTL Check succeeded", "Success");
				} else if(result instanceof LTLCounterExample) {
					trace = ((LTLCounterExample) result).getTrace(stateid.getStateSpace());
					resultItem = new LTLResultHandler.LTLResultItem(AlertType.ERROR, Checked.FAIL, "LTL Counter Example has been found", 
													"Counter Example Found");
				} else if(result instanceof LTLError) {
					resultItem = new LTLResultHandler.LTLResultItem(AlertType.ERROR, Checked.FAIL, ((LTLError) result).getMessage(), 
													"Error while executing formula");
				}
			}
		} catch (LtlParseException e) {
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				e.printStackTrace(pw);
			}
			resultItem = new LTLResultHandler.LTLResultItem(AlertType.ERROR, Checked.FAIL, "Message: ", "Could not parse formula", 
											sw.toString());
			logger.error("Could not parse LTL formula", e);
		}
		resultHandler.showResult(resultItem, item, trace);
		injector.getInstance(LTLView.class).refreshFormula();
		if(resultItem != null) {
			return resultItem.getChecked();
		}
		return Checked.FAIL;
	}
		


}
