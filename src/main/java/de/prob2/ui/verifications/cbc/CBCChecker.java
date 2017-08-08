package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.CBCDeadlockChecker;
import de.prob.check.CBCInvariantChecker;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckOk;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;

public class CBCChecker {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CBCChecker.class);
	
	private final CurrentTrace currentTrace;
	
	private final Injector injector;
	
	@Inject
	public CBCChecker(final CurrentTrace currentTrace, final Injector injector) {
		this.currentTrace = currentTrace;
		this.injector = injector;
	}
	
	public void checkInvariant(String name) {
		ArrayList<String> event = new ArrayList<>();
		event.add(name);
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), event);
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		CBCFormulaItem item = currentMachine.getCBCFormulas()
				.stream()
				.filter(current -> name.equals(current.getName()))
				.findFirst().get();
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
			LOGGER.error("Could not check CBC Invariant: ", e.getMessage());
			item.setCheckedFailed();
			item.setChecked(Checked.FAIL);
		}
		CBCView cbcView = injector.getInstance(CBCView.class);
		cbcView.updateMachineStatus(currentMachine);
		cbcView.refresh();
	}
	
	public void checkDeadlock(String code) {
		IEvalElement constraint = new EventB(code); 
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace(), constraint);
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		CBCFormulaItem item = currentMachine.getCBCFormulas()
				.stream()
				.filter(current -> code.equals(current.getCode()))
				.findFirst().get();
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
		CBCView cbcView = injector.getInstance(CBCView.class);
		cbcView.updateMachineStatus(currentMachine);
		cbcView.refresh();
	}

}
