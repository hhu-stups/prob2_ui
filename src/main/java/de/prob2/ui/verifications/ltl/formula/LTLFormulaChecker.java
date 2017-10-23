package de.prob2.ui.verifications.ltl.formula;

import java.util.ArrayList;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.be4.classicalb.core.parser.ClassicalBParser;

import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.LTL;
import de.prob.exception.ProBError;
import de.prob.ltl.parser.LtlParser;
import de.prob.statespace.State;

import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.verifications.MachineTableView;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.ltl.LTLMarker;
import de.prob2.ui.verifications.ltl.LTLParseListener;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LTLFormulaChecker {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLFormulaChecker.class);
				
	private final CurrentTrace currentTrace;
	
	private final LTLResultHandler resultHandler;
	
	private final Injector injector;
	
	@Inject
	private LTLFormulaChecker(final CurrentTrace currentTrace, final LTLResultHandler resultHandler,
								final Injector injector) {
		this.currentTrace = currentTrace;
		this.resultHandler = resultHandler;
		this.injector = injector;
	}
	
	public void checkMachine(Machine machine) {
		final ArrayList<Boolean> failed = new ArrayList<>();
		failed.add(false);
		for (LTLFormulaItem item : machine.getLTLFormulas()) {
			Checked result = this.checkFormula(item, machine);
			if(result == Checked.FAIL || result == Checked.EXCEPTION) {
				failed.set(0, true);
				machine.setLTLCheckedFailed();
			}
			item.setChecked(result);
		}
		Platform.runLater(() -> injector.getInstance(StatusBar.class).setLtlStatus(failed.get(0) ? StatusBar.LTLStatus.ERROR : StatusBar.LTLStatus.SUCCESSFUL));
	
	}
	
	public void checkMachineStatus(Machine machine) {
		for(LTLFormulaItem item : machine.getLTLFormulas()) {
			if(!item.shouldExecute()) {
				continue;
			}
			Checked checked = item.getChecked();
			if(checked == Checked.FAIL || checked == Checked.EXCEPTION) {
				machine.setLTLCheckedFailed();
				injector.getInstance(MachineTableView.class).refresh();
				injector.getInstance(StatusBar.class).setLtlStatus(StatusBar.LTLStatus.ERROR);
				return;
			}
		}
		machine.setLTLCheckedSuccessful();
		injector.getInstance(MachineTableView.class).refresh();
		injector.getInstance(StatusBar.class).setLtlStatus(StatusBar.LTLStatus.SUCCESSFUL);
	}
	
	public Checked checkFormula(LTLFormulaItem item, Machine machine) {
		State stateid = currentTrace.getCurrentState();
		LtlParser parser = new LtlParser(item.getCode());
		parser.setPatternManager(machine.getPatternManager());
		return resultHandler.handleFormulaResult(item, getResult(parser, item), stateid);
	}
	
	private Object getResult(LtlParser parser, LTLFormulaItem item) {
		State stateid = currentTrace.getCurrentState();
		LTLParseListener parseListener = parseFormula(parser);
		if(!parseListener.getErrorMarkers().isEmpty()) {
			return getFailedResult(parseListener);
		}
		EvaluationCommand lcc = null;
		try {
			LTL formula = new LTL(item.getCode(), new ClassicalBParser(), parser);
			lcc = formula.getCommand(stateid);
			currentTrace.getStateSpace().execute(lcc);
			injector.getInstance(StatsView.class).update(currentTrace.get());
		} catch (ProBError error) {
			logger.error("Could not parse LTL formula", error.getMessage());
			return error;
		}
		return lcc.getValue();
	}
	
	private Object getFailedResult(LTLParseListener parseListener) {
		StringBuilder msg = new StringBuilder();
		for(LTLMarker error : parseListener.getErrorMarkers()) {
			msg.append(error.getMsg()+"\n");
		}
		return new LTLParseError(msg.toString());
	}
	
	private LTLParseListener parseFormula(LtlParser parser) {
		LTLParseListener parseListener = new LTLParseListener();
		parser.removeErrorListeners();
		parser.addErrorListener(parseListener);
		parser.addWarningListener(parseListener);
		parser.parse();
		return parseListener;
	}
		
}
