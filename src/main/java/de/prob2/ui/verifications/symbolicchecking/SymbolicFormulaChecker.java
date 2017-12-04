package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.AbstractCommand;
import de.prob.check.IModelCheckJob;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SymbolicFormulaChecker {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicFormulaChecker.class);
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final Injector injector;
	
	private final SymbolicCheckingResultHandler resultHandler;
	
	private final ResourceBundle bundle;
	
	private final ListProperty<IModelCheckJob> currentJobs;
	
	private final ListProperty<Thread> currentJobThreads;
	
	@Inject
	public SymbolicFormulaChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
							final SymbolicCheckingResultHandler resultHandler, final Injector injector, final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
		this.injector = injector;
		this.bundle = bundle;
		this.currentJobs = new SimpleListProperty<>(this, "currentJobs", FXCollections.observableArrayList());
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
	}
	
	public void executeCheckingItem(IModelCheckJob checker, String code, SymbolicCheckingType type) {
		Machine currentMachine = currentProject.getCurrentMachine();
		currentMachine.getSymbolicCheckingFormulas()
			.stream()
			.filter(current -> current.getCode().equals(code) && current.getType().equals(type))
			.findFirst()
			.ifPresent(item -> checkItem(checker, item));
	}
	
	public void checkItem(SymbolicCheckingFormulaItem item, AbstractCommand cmd, final StateSpace stateSpace) {
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
				resultHandler.handleFormulaResult(currentItem, cmd);
				updateMachine(currentProject.getCurrentMachine());
				currentJobThreads.remove(currentThread);
			});
		}, "Symbolic Formula Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	public void checkItem(IModelCheckJob checker, SymbolicCheckingFormulaItem item) {
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
				resultHandler.handleFormulaResult(item, result.get(0), stateid);
				updateMachine(currentProject.getCurrentMachine());
				injector.getInstance(StatsView.class).update(currentTrace.get());
				currentJobs.remove(checker);
				currentJobThreads.remove(currentThread);
			});
		}, "Symbolic Formula Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
		
	public void updateMachine(Machine machine) {
		final SymbolicCheckingView symbolicCheckingView = injector.getInstance(SymbolicCheckingView.class);
		injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.SYMBOLIC);
		symbolicCheckingView.refresh();
	}
	
	private SymbolicCheckingFormulaItem getItemIfAlreadyExists(SymbolicCheckingFormulaItem item) {
		Machine currentMachine = currentProject.getCurrentMachine();
		int index = currentMachine.getSymbolicCheckingFormulas().indexOf(item);
		if(index > -1) {
			item = currentMachine.getSymbolicCheckingFormulas().get(index);
		}
		return item;
	}
	
	public void interrupt() {
		currentJobThreads.forEach(Thread::interrupt);
		currentJobs.forEach(job -> job.getStateSpace().sendInterrupt());
	}
	
	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}

}
