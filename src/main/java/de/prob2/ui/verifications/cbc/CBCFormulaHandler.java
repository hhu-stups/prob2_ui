package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import de.prob.animator.command.FindStateCommand;
import de.prob.animator.command.FindStateCommand.ResultType;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.CBCDeadlockChecker;
import de.prob.check.CBCInvariantChecker;
import de.prob.check.IModelCheckJob;
import de.prob.check.ModelCheckOk;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.cbc.CBCFormulaItem.CBCType;
import javafx.application.Platform;

public class CBCFormulaHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CBCFormulaHandler.class);
	
	private final CurrentTrace currentTrace;
	
	private final Injector injector;
	
	private final CBCResultHandler resultHandler;
	
	@Inject
	public CBCFormulaHandler(final CurrentTrace currentTrace, final CBCResultHandler resultHandler,
								final Injector injector) {
		this.currentTrace = currentTrace;
		this.resultHandler = resultHandler;
		this.injector = injector;
	}
	
	public void checkInvariant(String code) {
		ArrayList<String> event = new ArrayList<>();
		event.add(code);
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), event);
		executeCheckingItem(checker, code, CBCType.INVARIANT);
	}
	
	public void checkDeadlock(String code) {
		IEvalElement constraint = new EventB(code); 
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace(), constraint);
		executeCheckingItem(checker, code, CBCType.DEADLOCK);
	}
	
	public void findDeadlock() {
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace());
		executeCheckingItem(checker, "FIND DEADLOCK", CBCType.FIND_DEADLOCK);
	}
	
	public void checkSequence(String sequence) {
		List<String> events = Arrays.asList(sequence.replaceAll(" ", "").split(";"));
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), events);
		executeCheckingItem(checker, sequence, CBCType.SEQUENCE);
	}
	
	public void findRedundantInvariants() {
		StateSpace stateSpace = currentTrace.getStateSpace();
		GetRedundantInvariantsCommand cmd = new GetRedundantInvariantsCommand();
		stateSpace.execute(cmd);
		//TODO: continue
	}
	
	public void findValidState(CBCFormulaFindStateItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		FindStateCommand cmd = new FindStateCommand(stateSpace, new EventB(item.getCode()), true);
		ResultType result = null;
		try {
			stateSpace.execute(cmd);
			result = cmd.getResult();
			if(result == ResultType.STATE_FOUND) {
				item.setCheckedSuccessful();
				item.setChecked(Checked.SUCCESS);
			} else {
				item.setCheckedFailed();
				item.setChecked(Checked.FAIL);
			}
		} catch (ProBError | EvaluationException e){
			item.setCheckedFailed();
			item.setChecked(Checked.FAIL);
			LOGGER.error(e.getMessage());
		}
		resultHandler.handleFindValidState(item, cmd, stateSpace);
		updateMachine(injector.getInstance(CBCView.class).getCurrentMachine());
	}
	

	
	public void executeCheckingItem(IModelCheckJob checker, String code, CBCType type) {
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		Thread executionThread = new Thread(() -> 
			Platform.runLater(() -> 
				currentMachine.getCBCFormulas()
					.stream()
					.filter(current -> current.getCode().equals(code) && current.getType().equals(type))
					.findFirst()
					.ifPresent(item -> checkItem(checker, item))
			)
		);
		Thread updatingThread = new Thread(() -> 
			Platform.runLater(() -> 
				updateMachine(currentMachine)
			)
		);
		executionThread.start();
		updatingThread.start();
	}
		
	public void checkMachine(Machine machine) {
		machine.getCBCFormulas().forEach(this::checkItem);
	}
	
	public void checkItem(CBCFormulaItem item) {
		if(item.getType() == CBCType.INVARIANT) {
			checkInvariant(item.getCode());
		} else if(item.getType() == CBCType.DEADLOCK) {
			checkDeadlock(item.getCode());
		} else if(item.getType() == CBCType.SEQUENCE) {
			checkSequence(item.getCode());
		} else if(item.getType() == CBCType.FIND_VALID_STATE) {
			findValidState((CBCFormulaFindStateItem) item);
		} else {
			findDeadlock();
		}
	}
	
	public void updateMachineStatus(Machine machine) {
		for(CBCFormulaItem formula : machine.getCBCFormulas()) {
			if(formula.getChecked() == Checked.FAIL) {
				machine.setCBCCheckedFailed();
				injector.getInstance(StatusBar.class).setCbcStatus(StatusBar.CBCStatus.ERROR);
				return;
			}
		}
		machine.setCBCCheckedSuccessful();
		injector.getInstance(StatusBar.class).setCbcStatus(StatusBar.CBCStatus.SUCCESSFUL);
	}
		
	private void updateMachine(Machine machine) {
		CBCView cbcView = injector.getInstance(CBCView.class);
		updateMachineStatus(machine);
		cbcView.refresh();
	}
	
	public void addFormula(String name, String code, CBCType type, boolean checking) {
		CBCFormulaItem formula = new CBCFormulaItem(name, code, type);
		addFormula(formula,checking);
	}
	
	public void addFormula(CBCFormulaItem formula, boolean checking) {
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		if (currentMachine != null) {
			if(!currentMachine.getCBCFormulas().contains(formula)) {
				currentMachine.addCBCFormula(formula);
				injector.getInstance(CBCView.class).updateProject();
			} else if(!checking) {
				resultHandler.showAlreadyExists(CBCResultHandler.ItemType.Formula);
			}
		}
	}
	
	private void checkItem(IModelCheckJob checker, CBCFormulaItem item) {
		State stateid = currentTrace.getCurrentState();
		Object result = null;
		try {
			result = checker.call();
			if(result instanceof ModelCheckOk) {
				item.setCheckedSuccessful();
				item.setChecked(Checked.SUCCESS);
			} else {
				item.setCheckedFailed();
				item.setChecked(Checked.FAIL);
			}
		} catch (Exception e) {
			String message = "Could not check CBC Deadlock: ".concat(e.getMessage());
			LOGGER.error(message);
			item.setCheckedFailed();
			item.setChecked(Checked.FAIL);
			result = new CBCParseError(message);
		}
		resultHandler.handleFormulaResult(item, result, stateid);
	}
	

}
