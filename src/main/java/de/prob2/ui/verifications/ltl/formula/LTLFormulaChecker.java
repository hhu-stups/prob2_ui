package de.prob2.ui.verifications.ltl.formula;

import java.util.ArrayList;

import javax.inject.Inject;
import com.google.inject.Injector;

import de.be4.classicalb.core.parser.ClassicalBParser;
import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.LTL;
import de.prob.ltl.parser.LtlParser;
import de.prob.ltl.parser.pattern.PatternManager;
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
	
	public void checkMachine(Machine machine, PatternManager patternManager) {
		ArrayList<Boolean> success = new ArrayList<>();
		success.add(true);
		machine.getFormulas().forEach(item-> {
			Checked result = this.checkFormula(item, patternManager);
			if(result == Checked.FAIL || result == Checked.EXCEPTION) {
				machine.setCheckedFailed();
				success.set(0, false);
			}
		});
		if(success.get(0)) {
			machine.setCheckedSuccessful();
		}
	}
	
	public Checked checkFormula(LTLFormulaItem item, PatternManager patternManager) {
		State stateid = currentTrace.getCurrentState();
		LtlParser parser = new LtlParser(item.getFormula());
		parser.setPatternManager(patternManager);
		Checked checked = resultHandler.handleFormulaResult(item, getResult(parser, item), stateid);
		injector.getInstance(LTLView.class).refreshFormula();
		return checked;
	}
	
	private Object getResult(LtlParser parser, LTLFormulaItem item) {
		Object result = null;
		State stateid = currentTrace.getCurrentState();
		LTLParseListener parseListener = parseFormula(parser);
		if(parseListener.getErrorMarkers().size() > 0) {
			StringBuilder msg = new StringBuilder();
			for(LTLMarker error : parseListener.getErrorMarkers()) {
				msg.append(error.getMsg()+"\n");
			}
			result = new LTLParseError(msg.toString());
		} else {
			LTL formula = new LTL(item.getFormula(), new ClassicalBParser(), parser);
			EvaluationCommand lcc = formula.getCommand(stateid);
			currentTrace.getStateSpace().execute(lcc);
			result = lcc.getValue();
		}
		return result;
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
