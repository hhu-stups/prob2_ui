package de.prob2.ui.verifications.ltl;

import java.util.ArrayList;

import javax.inject.Inject;
import com.google.inject.Injector;

import de.be4.ltl.core.parser.LtlParseException;
import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.LTL;
import de.prob.statespace.State;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
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
		Object result = null;
		State stateid = currentTrace.getCurrentState();
		try {
			formula = new LTL(item.getFormula());
			if (currentTrace != null) {
				EvaluationCommand lcc = formula.getCommand(stateid);
				currentTrace.getStateSpace().execute(lcc);
				result = lcc.getValue();
			}
		} catch (LtlParseException e) {
			result = e;
		}
		Checked checked = resultHandler.handleFormulaResult(item, result, stateid);
		injector.getInstance(LTLView.class).refreshFormula();
		return checked;
	}
		
}
