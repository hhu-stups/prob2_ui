package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.AbstractCommand;
import de.prob.check.IModelCheckJob;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
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
	
	private final List<IModelCheckJob> currentJobs;
	
	private final ListProperty<Thread> currentJobThreads;
	
	@Inject
	public SymbolicFormulaChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
							final SymbolicCheckingResultHandler resultHandler, final Injector injector) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
		this.injector = injector;
		this.currentJobs = new ArrayList<>();
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
	}
	
	public void executeCheckingItem(IModelCheckJob checker, String code, SymbolicCheckingType type, boolean checkAll) {
		Machine currentMachine = currentProject.getCurrentMachine();
		currentMachine.getSymbolicCheckingFormulas()
			.stream()
			.filter(current -> current.getCode().equals(code) && current.getType().equals(type))
			.findFirst()
			.ifPresent(item -> checkItem(checker, item, checkAll));
	}
	
	public void checkItem(SymbolicCheckingFormulaItem item, AbstractCommand cmd, final StateSpace stateSpace, boolean checkAll) {
		final SymbolicCheckingFormulaItem currentItem = getItemIfAlreadyExists(item);
		Thread checkingThread = new Thread(() -> {
			RuntimeException exception = null;
			try {
				stateSpace.execute(cmd);
			} catch (RuntimeException e) {
				LOGGER.error("Exception during symbolic checking", e);
				exception = e;
			}
			injector.getInstance(StatsView.class).update(currentTrace.get());
			Thread currentThread = Thread.currentThread();
			final RuntimeException finalException = exception;
			Platform.runLater(() -> {
				if (finalException == null) {
					resultHandler.handleFormulaResult(currentItem, cmd);
				} else {
					resultHandler.handleFormulaResult(currentItem, finalException, null);
				}
				updateMachine(currentProject.getCurrentMachine());
				currentJobThreads.remove(currentThread);
				if(!checkAll) {
					List<Trace> counterExamples = item.getCounterExamples();
					Trace example = item.getExample();
					if(!counterExamples.isEmpty()) {
						currentTrace.set(counterExamples.get(0));
					} else if(example != null) {
						currentTrace.set(example);
					}
				}
			});
		}, "Symbolic Formula Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	public void checkItem(IModelCheckJob checker, SymbolicCheckingFormulaItem item, boolean checkAll) {
		Thread checkingThread = new Thread(() -> {
			State stateid = currentTrace.getCurrentState();
			currentJobs.add(checker);
			Object result;
			try {
				result = checker.call();
			} catch (Exception e) {
				LOGGER.error("Could not check CBC Deadlock", e);
				result = e;
			}
			injector.getInstance(StatsView.class).update(currentTrace.get());
			final Object finalResult = result;
			Platform.runLater(() -> {
				resultHandler.handleFormulaResult(item, finalResult, stateid);
				updateMachine(currentProject.getCurrentMachine());
				if(!checkAll) {
					List<Trace> counterExamples = item.getCounterExamples();
					Trace example = item.getExample();
					if(!counterExamples.isEmpty()) {
						currentTrace.set(counterExamples.get(0));
					} else if(example != null) {
						currentTrace.set(example);
					}
				}
			});
			currentJobs.remove(checker);
			currentJobThreads.remove(Thread.currentThread());
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
		List<Thread> removedThreads = new ArrayList<>();
		for(Iterator<Thread> iterator = currentJobThreads.iterator(); iterator.hasNext();) {
			Thread thread = iterator.next();
			thread.interrupt();
			removedThreads.add(thread);
		}
		List<IModelCheckJob> removedJobs = new ArrayList<>();
		for(Iterator<IModelCheckJob> iterator = currentJobs.iterator(); iterator.hasNext();) {
			IModelCheckJob job = iterator.next();
			removedJobs.add(job);
		}
		currentTrace.getStateSpace().sendInterrupt();
		currentJobThreads.removeAll(removedThreads);
		currentJobs.removeAll(removedJobs);
	}
	
	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}

}
