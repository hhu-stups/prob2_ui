package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.CBCDeadlockChecker;
import de.prob.check.CBCInvariantChecker;
import de.prob.check.IModelCheckJob;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.verifications.MachineTableView;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.Checked;

import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CBCFormulaHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CBCFormulaHandler.class);
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final Injector injector;
	
	private final CBCResultHandler resultHandler;
	
	private final ResourceBundle bundle;

	
	@Inject
	public CBCFormulaHandler(final CurrentTrace currentTrace, final CurrentProject currentProject, final CBCResultHandler resultHandler, final Injector injector, final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
		this.injector = injector;
		this.bundle = bundle;
	}
	
	public void checkInvariant(String code) {
		ArrayList<String> event = new ArrayList<>();
		event.add(code);
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), event);
		executeCheckingItem(checker, code, CBCFormulaItem.CBCType.INVARIANT);
	}
	
	public void checkDeadlock(String code) {
		IEvalElement constraint = new EventB(code); 
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace(), constraint);
		executeCheckingItem(checker, code, CBCFormulaItem.CBCType.DEADLOCK);
	}
	
	public void findDeadlock() {
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace());
		executeCheckingItem(checker, "FIND DEADLOCK", CBCFormulaItem.CBCType.FIND_DEADLOCK);
	}
	
	public void checkSequence(String sequence) {
		List<String> events = Arrays.asList(sequence.replaceAll(" ", "").split(";"));
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), events);
		executeCheckingItem(checker, sequence, CBCFormulaItem.CBCType.SEQUENCE);
	}
	
	public void findRedundantInvariants(CBCFormulaItem item) {
		final CBCFormulaItem currentItem = getItemIfAlreadyExists(item);
		StateSpace stateSpace = currentTrace.getStateSpace();
		GetRedundantInvariantsCommand cmd = new GetRedundantInvariantsCommand();
		Thread checkingThread = new Thread(() -> {
			stateSpace.execute(cmd);
			injector.getInstance(StatsView.class).update(currentTrace.get());
			Platform.runLater(() -> {
				resultHandler.handleFindRedundantInvariants(currentItem, cmd);
				updateMachine(currentProject.getCurrentMachine());
			});
		});
		checkingThread.start();
	}
		
	public void checkRefinement(CBCFormulaItem item) {
		final CBCFormulaItem currentItem = getItemIfAlreadyExists(item);
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedRefinementCheckCommand command = new ConstraintBasedRefinementCheckCommand();
		Thread checkingThread = new Thread(() -> {
			try {
				stateSpace.execute(command);
				injector.getInstance(StatsView.class).update(currentTrace.get());
			} catch (Exception e){
				LOGGER.error(e.getMessage());
			}
			Platform.runLater(() -> {
				resultHandler.handleRefinementChecking(currentItem, command);
				updateMachine(currentProject.getCurrentMachine());
			});
		});
		checkingThread.start();
	}
	

		
	public void checkAssertions(CBCFormulaItem item) {
		final CBCFormulaItem currentItem = getItemIfAlreadyExists(item);
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedAssertionCheckCommand command = new ConstraintBasedAssertionCheckCommand(stateSpace);
		Thread checkingThread = new Thread(() -> {
			stateSpace.execute(command);
			Platform.runLater(() -> {
				injector.getInstance(StatsView.class).update(currentTrace.get());
				resultHandler.handleAssertionChecking(currentItem, command, stateSpace);
				updateMachine(currentProject.getCurrentMachine());
			});
		});
		checkingThread.start();
	}
	
	private CBCFormulaItem getItemIfAlreadyExists(CBCFormulaItem item) {
		Machine currentMachine = currentProject.getCurrentMachine();
		int index = currentMachine.getCBCFormulas().indexOf(item);
		if(index > -1) {
			item = currentMachine.getCBCFormulas().get(index);
		}
		return item;
	}
	

	public void findValidState(CBCFormulaItem item) {
		final CBCFormulaItem currentItem = getItemIfAlreadyExists(item);
		StateSpace stateSpace = currentTrace.getStateSpace();
		FindStateCommand cmd = new FindStateCommand(stateSpace, new EventB(item.getCode()), true);
		Thread checkingThread = new Thread(() -> {
			try {
				stateSpace.execute(cmd);
			} catch (ProBError | EvaluationException e){
				LOGGER.error(e.getMessage());
			}
			Platform.runLater(() -> {
				injector.getInstance(StatsView.class).update(currentTrace.get());
				resultHandler.handleFindValidState(currentItem, cmd, stateSpace);
				updateMachine(currentProject.getCurrentMachine());
			});
		});
		checkingThread.start();
	}
	

	
	public void executeCheckingItem(IModelCheckJob checker, String code, CBCFormulaItem.CBCType type) {
		Machine currentMachine = currentProject.getCurrentMachine();
		currentMachine.getCBCFormulas()
			.stream()
			.filter(current -> current.getCode().equals(code) && current.getType().equals(type))
			.findFirst()
			.ifPresent(item -> checkItem(checker, item));
	}
		
	public void checkMachine(Machine machine) {
		machine.getCBCFormulas().forEach(this::checkItem);
	}
	
	public void checkItem(CBCFormulaItem item) {
		switch(item.getType()) {
			case INVARIANT:
				checkInvariant(item.getCode());
				break;
			case DEADLOCK:
				checkDeadlock(item.getCode());
				break;
			case SEQUENCE:
				checkSequence(item.getCode());
				break;
			case FIND_VALID_STATE:
				findValidState(item);
				break;
			case FIND_DEADLOCK:
				findDeadlock();
				break;
			case REFINEMENT:
				checkRefinement(item);
				break;
			case ASSERTIONS:
				checkAssertions(item);
				break;
			case FIND_REDUNDANT_INVARIANTS:
				findRedundantInvariants(item);
				break;
			default:
				break;
		}
	}
	
	public void updateMachineStatus(Machine machine) {
		for(CBCFormulaItem formula : machine.getCBCFormulas()) {
			if(formula.getChecked() == Checked.FAIL) {
				machine.setCBCCheckedFailed();
				injector.getInstance(MachineTableView.class).refresh();
				injector.getInstance(StatusBar.class).setCbcStatus(StatusBar.CBCStatus.ERROR);
				return;
			}
		}
		machine.setCBCCheckedSuccessful();
		injector.getInstance(MachineTableView.class).refresh();
		injector.getInstance(StatusBar.class).setCbcStatus(StatusBar.CBCStatus.SUCCESSFUL);
	}
		
	private void updateMachine(Machine machine) {
		final CBCView cbcView = injector.getInstance(CBCView.class);
		updateMachineStatus(machine);
		cbcView.refresh();
	}
	
	public void addFormula(String name, String code, CBCFormulaItem.CBCType type, boolean checking) {
		CBCFormulaItem formula = new CBCFormulaItem(name, code, type);
		addFormula(formula,checking);
	}
	
	public void addFormula(CBCFormulaItem formula, boolean checking) {
		Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			if(!currentMachine.getCBCFormulas().contains(formula)) {
				currentMachine.addCBCFormula(formula);
				injector.getInstance(CBCView.class).updateProject();
			} else if(!checking) {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
		}
	}
	
	private void checkItem(IModelCheckJob checker, CBCFormulaItem item) {
		Thread checkingThread = new Thread(() -> {
			State stateid = currentTrace.getCurrentState();
			ArrayList<Object> result = new ArrayList<>();
			result.add(null);
			try {
				result.set(0, checker.call());
			} catch (Exception e) {
				LOGGER.error("Could not check CBC Deadlock", e);
				result.set(0, new CBCParseError(String.format(bundle.getString("verifications.cbc.couldNotCheckCBCDeadlock"), e.getMessage())));
			}
			Platform.runLater(() -> {
				Machine currentMachine = currentProject.getCurrentMachine();
				resultHandler.handleFormulaResult(item, result.get(0), stateid);
				updateMachine(currentMachine);
				injector.getInstance(StatsView.class).update(currentTrace.get());
			});
		});
		checkingThread.start();
	}
		

}
