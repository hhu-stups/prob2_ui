package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.AbstractCommand;
import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.CBCDeadlockChecker;
import de.prob.check.CBCInvariantChecker;
import de.prob.check.IModelCheckJob;
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
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SymbolicCheckingFormulaHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicCheckingFormulaHandler.class);
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final Injector injector;
	
	private final SymbolicCheckingResultHandler resultHandler;
	
	private final ResourceBundle bundle;
	
	private final ListProperty<IModelCheckJob> currentJobs;
	
	private final ListProperty<Thread> currentJobThreads;

	
	@Inject
	public SymbolicCheckingFormulaHandler(final CurrentTrace currentTrace, final CurrentProject currentProject, 
							final SymbolicCheckingResultHandler resultHandler, final Injector injector, final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
		this.injector = injector;
		this.bundle = bundle;
		this.currentJobs = new SimpleListProperty<>(this, "currentJobs", FXCollections.observableArrayList());
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
	}
	
	public void checkInvariant(String code) {
		ArrayList<String> event = new ArrayList<>();
		event.add(code);
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), event);
		executeCheckingItem(checker, code, SymbolicCheckingType.INVARIANT);
	}
	
	public void checkDeadlock(String code) {
		IEvalElement constraint = new EventB(code); 
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace(), constraint);
		executeCheckingItem(checker, code, SymbolicCheckingType.DEADLOCK);
	}
	
	public void findDeadlock() {
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace());
		executeCheckingItem(checker, "FIND DEADLOCK", SymbolicCheckingType.FIND_DEADLOCK);
	}
	
	public void checkSequence(String sequence) {
		List<String> events = Arrays.asList(sequence.replaceAll(" ", "").split(";"));
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), events);
		executeCheckingItem(checker, sequence, SymbolicCheckingType.SEQUENCE);
	}
	
	public void findRedundantInvariants(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		GetRedundantInvariantsCommand cmd = new GetRedundantInvariantsCommand();
		checkItem(item, cmd, stateSpace);
	}
		
	public void checkRefinement(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedRefinementCheckCommand cmd = new ConstraintBasedRefinementCheckCommand();
		checkItem(item, cmd, stateSpace);
	}
	
	public void checkAssertions(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(stateSpace);
		checkItem(item, cmd, stateSpace);
	}
	
	public void checkSymbolic(SymbolicCheckingFormulaItem item, SymbolicModelcheckCommand.Algorithm algorithm) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		SymbolicModelcheckCommand cmd = new SymbolicModelcheckCommand(algorithm);
		checkItem(item, cmd, stateSpace);
		
	}
	
	public void findValidState(SymbolicCheckingFormulaItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		FindStateCommand cmd = new FindStateCommand(stateSpace, new EventB(item.getCode()), true);
		checkItem(item, cmd, stateSpace);
	}
	
	private void handleResult(SymbolicCheckingFormulaItem item, AbstractCommand cmd) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		if(item.getType() == SymbolicCheckingType.FIND_VALID_STATE) {
			resultHandler.handleFindValidState(item, (FindStateCommand) cmd, stateSpace);
		} else if(item.getType() == SymbolicCheckingType.TINDUCTION || item.getType() == SymbolicCheckingType.KINDUCTION ||
					item.getType() == SymbolicCheckingType.BMC || item.getType() == SymbolicCheckingType.IC3) {
			resultHandler.handleSymbolicChecking(item, (SymbolicModelcheckCommand) cmd);
		} else if(item.getType() == SymbolicCheckingType.CHECK_ASSERTIONS) {
			resultHandler.handleAssertionChecking(item, (ConstraintBasedAssertionCheckCommand) cmd, stateSpace);
		} else if(item.getType() == SymbolicCheckingType.CHECK_REFINEMENT) {
			resultHandler.handleRefinementChecking(item, (ConstraintBasedRefinementCheckCommand) cmd);
		} else if(item.getType() == SymbolicCheckingType.FIND_REDUNDANT_INVARIANTS) {
			resultHandler.handleFindRedundantInvariants(item, (GetRedundantInvariantsCommand) cmd);
		}
	}
	 
	private SymbolicCheckingFormulaItem getItemIfAlreadyExists(SymbolicCheckingFormulaItem item) {
		Machine currentMachine = currentProject.getCurrentMachine();
		int index = currentMachine.getSymbolicCheckingFormulas().indexOf(item);
		if(index > -1) {
			item = currentMachine.getSymbolicCheckingFormulas().get(index);
		}
		return item;
	}
	
	public void executeCheckingItem(IModelCheckJob checker, String code, SymbolicCheckingType type) {
		Machine currentMachine = currentProject.getCurrentMachine();
		currentMachine.getSymbolicCheckingFormulas()
			.stream()
			.filter(current -> current.getCode().equals(code) && current.getType().equals(type))
			.findFirst()
			.ifPresent(item -> checkItem(checker, item));
	}
		
	public void checkMachine(Machine machine) {
		machine.getSymbolicCheckingFormulas().forEach(this::checkItem);
	}
	
	public void checkItem(SymbolicCheckingFormulaItem item) {
		if(!item.shouldExecute()) {
			return;
		}
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
			case CHECK_REFINEMENT:
				checkRefinement(item);
				break;
			case CHECK_ASSERTIONS:
				checkAssertions(item);
				break;
			case FIND_REDUNDANT_INVARIANTS:
				findRedundantInvariants(item);
				break;
			case IC3:
				checkSymbolic(item, SymbolicModelcheckCommand.Algorithm.IC3);
				break;
			case TINDUCTION:
				checkSymbolic(item, SymbolicModelcheckCommand.Algorithm.TINDUCTION);
				break;
			case KINDUCTION:
				checkSymbolic(item, SymbolicModelcheckCommand.Algorithm.KINDUCTION);
				break;
			case BMC:
				checkSymbolic(item, SymbolicModelcheckCommand.Algorithm.BMC);
				break;
			default:
				break;
		}
	}
	
	public void updateMachineStatus(Machine machine) {
		for(SymbolicCheckingFormulaItem formula : machine.getSymbolicCheckingFormulas()) {
			if(!formula.shouldExecute()) {
				continue;
			}
			if(formula.getChecked() == Checked.FAIL) {
				machine.setSymbolicCheckedFailed();
				injector.getInstance(MachineTableView.class).refresh();
				injector.getInstance(StatusBar.class).setCbcStatus(StatusBar.CBCStatus.ERROR);
				return;
			}
		}
		machine.setSymbolicCheckedSuccessful();
		injector.getInstance(MachineTableView.class).refresh();
		injector.getInstance(StatusBar.class).setCbcStatus(StatusBar.CBCStatus.SUCCESSFUL);
	}
		
	private void updateMachine(Machine machine) {
		final SymbolicCheckingView cbcView = injector.getInstance(SymbolicCheckingView.class);
		updateMachineStatus(machine);
		cbcView.refresh();
	}
	
	public void addFormula(String name, String code, SymbolicCheckingType type, boolean checking) {
		SymbolicCheckingFormulaItem formula = new SymbolicCheckingFormulaItem(name, code, type);
		addFormula(formula,checking);
	}
	
	public void addFormula(SymbolicCheckingFormulaItem formula, boolean checking) {
		Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			if(!currentMachine.getSymbolicCheckingFormulas().contains(formula)) {
				currentMachine.addSymbolicCheckingFormula(formula);
				injector.getInstance(SymbolicCheckingView.class).updateProject();
			} else if(!checking) {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
		}
	}
	
	private void checkItem(SymbolicCheckingFormulaItem item, AbstractCommand cmd, final StateSpace stateSpace) {
		final SymbolicCheckingFormulaItem currentItem = getItemIfAlreadyExists(item);
		Thread checkingThread = new Thread(() -> {
			try {
				stateSpace.execute(cmd);
			} catch (Exception e){
				LOGGER.error(e.getMessage());
			}
			Thread currentThread = Thread.currentThread();
			Platform.runLater(() -> {
				injector.getInstance(StatsView.class).update(currentTrace.get());
				handleResult(currentItem, cmd);
				updateMachine(currentProject.getCurrentMachine());
				currentJobThreads.remove(currentThread);
			});
		});
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	private void checkItem(IModelCheckJob checker, SymbolicCheckingFormulaItem item) {
		Thread checkingThread = new Thread(() -> {
			State stateid = currentTrace.getCurrentState();
			ArrayList<Object> result = new ArrayList<>();
			result.add(null);
			currentJobs.add(checker);
			try {
				result.set(0, checker.call());
			} catch (Exception e) {
				LOGGER.error("Could not check CBC Deadlock", e);
				result.set(0, new SymbolicCheckingParseError(String.format(bundle.getString("verifications.symbolic.couldNotCheckDeadlock"), e.getMessage())));
			}
			Thread currentThread = Thread.currentThread();
			Platform.runLater(() -> {
				Machine currentMachine = currentProject.getCurrentMachine();
				resultHandler.handleFormulaResult(item, result.get(0), stateid);
				updateMachine(currentMachine);
				injector.getInstance(StatsView.class).update(currentTrace.get());
				currentJobs.remove(checker);
				currentJobThreads.remove(currentThread);
			});
		});
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	public void interrupt() {
		currentJobThreads.forEach(Thread::interrupt);
		currentJobs.forEach(job -> job.getStateSpace().sendInterrupt());
	}
	
	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}
	
}
