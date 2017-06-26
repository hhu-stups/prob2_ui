package de.prob2.ui.verifications.ltl.formula;

import java.util.ArrayList;

import javax.inject.Inject;
import com.google.inject.Injector;

import de.be4.classicalb.core.parser.ClassicalBParser;
import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.LTL;
import de.prob.ltl.parser.LtlParser;
import de.prob.statespace.State;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.LTLMarker;
import de.prob2.ui.verifications.ltl.LTLParseListener;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import de.prob2.ui.verifications.ltl.LTLView;
import de.prob2.ui.verifications.ltl.LTLResultHandler.Checked;

public class LTLFormulaChecker {
				
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
			Checked result = this.checkFormula(item, machine);
			if(result == Checked.FAIL || result == Checked.EXCEPTION) {
				machine.setCheckedFailed();
				success.set(0, false);
			}
		});
		if(success.get(0)) {
			machine.setCheckedSuccessful();
		}
	}
	
	public Checked checkFormula(LTLFormulaItem item, Machine machine) {
		State stateid = currentTrace.getCurrentState();
		LtlParser parser = new LtlParser(item.getCode());
		parser.setPatternManager(machine.getPatternManager());
		Checked checked = resultHandler.handleFormulaResult(item, getResult(parser, item), stateid);
		injector.getInstance(LTLView.class).refreshFormula();
		return checked;
	}
	
	private Object getResult(LtlParser parser, LTLFormulaItem item) {
		State stateid = currentTrace.getCurrentState();
		LTLParseListener parseListener = parseFormula(parser);
		if(parseListener.getErrorMarkers().size() > 0) {
			return getFailedResult(parseListener);
		}
		LTL formula = new LTL(item.getCode(), new ClassicalBParser(), parser);
		EvaluationCommand lcc = formula.getCommand(stateid);
		currentTrace.getStateSpace().execute(lcc);
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
