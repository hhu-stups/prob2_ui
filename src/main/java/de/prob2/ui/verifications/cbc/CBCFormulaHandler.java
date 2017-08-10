package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.CBCDeadlockChecker;
import de.prob.check.CBCInvariantChecker;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckOk;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.cbc.CBCFormulaItem.CBCType;

public class CBCFormulaHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CBCFormulaHandler.class);
	
	private final CurrentTrace currentTrace;
	
	private final Injector injector;
	
	@Inject
	public CBCFormulaHandler(final CurrentTrace currentTrace, final Injector injector) {
		this.currentTrace = currentTrace;
		this.injector = injector;
	}
	
	public void checkInvariant(String name) {
		ArrayList<String> event = new ArrayList<>();
		event.add(name);
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), event);
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		currentMachine.getCBCFormulas()
				.stream()
				.filter(current -> name.equals(current.getName()))
				.findFirst()
				.ifPresent(item -> checkItem(checker, item));
		updateMachine(currentMachine);
	}
	
	public void checkDeadlock(String code) {
		IEvalElement constraint = new EventB(code); 
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace(), constraint);
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		currentMachine.getCBCFormulas()
				.stream()
				.filter(current -> code.equals(current.getCode()))
				.findFirst()
				.ifPresent(item -> checkItem(checker, item));
		updateMachine(currentMachine);
	}
	
	public void checkSequence(String sequence) {
		List<String> events = Arrays.asList(sequence.split(";"));
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), events);
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		currentMachine.getCBCFormulas()
				.stream()
				.filter(current -> sequence.equals(current.getName()))
				.findFirst()
				.ifPresent(item -> checkItem(checker, item));
		updateMachine(currentMachine);
	}
	
	private void updateMachine(Machine machine) {
		CBCView cbcView = injector.getInstance(CBCView.class);
		cbcView.updateMachineStatus(machine);
		cbcView.refresh();
	}
	
	public void addFormula(String name, String code, CBCType type) {
		CBCFormulaItem formula = new CBCFormulaItem(name, code, type);
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		if (currentMachine != null && !currentMachine.getCBCFormulas().contains(formula)) {
			currentMachine.addCBCFormula(formula);
			injector.getInstance(CBCView.class).updateProject();
		}
	}
	
	private void checkItem(IModelCheckJob checker, CBCFormulaItem item) {
		try {
			IModelCheckingResult result = checker.call();
			if(result instanceof ModelCheckOk) {
				item.setCheckedSuccessful();
				item.setChecked(Checked.SUCCESS);
			} else {
				item.setCheckedFailed();
				item.setChecked(Checked.FAIL);
			}
		} catch (Exception e) {
			LOGGER.error("Could not check CBC Deadlock: ", e.getMessage());
			item.setCheckedFailed();
			item.setChecked(Checked.FAIL);
		}
	}
	

}
