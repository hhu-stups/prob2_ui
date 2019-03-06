package de.prob2.ui.symbolic;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import de.prob.animator.command.AbstractCommand;
import de.prob.check.IModelCheckJob;
import de.prob.statespace.StateSpace;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingResultHandler;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public abstract class SymbolicExecutor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicExecutor.class);
	
	protected final CurrentTrace currentTrace;
	
	protected final CurrentProject currentProject;
	
	protected final Injector injector;
	
	private final ISymbolicResultHandler resultHandler;
	
	private final List<IModelCheckJob> currentJobs;
	
	private final ListProperty<Thread> currentJobThreads;
	
	protected List<? extends SymbolicFormulaItem> items;
	
	public SymbolicExecutor(final CurrentTrace currentTrace, final CurrentProject currentProject,
							final ISymbolicResultHandler resultHandler, final Injector injector) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
		this.injector = injector;
		this.currentJobs = new ArrayList<>();
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
	}
	
	public void executeCheckingItem(IModelCheckJob checker, String code, SymbolicExecutionType type, boolean checkAll) {
		getItems().stream()
			.filter(current -> current.getCode().equals(code) && current.getType().equals(type))
			.findFirst()
			.ifPresent(item -> checkItem(checker, item, checkAll));
	}

	public void interrupt() {
		List<Thread> removedThreads = new ArrayList<>();
		for (Thread thread : currentJobThreads) {
			thread.interrupt();
			removedThreads.add(thread);
		}
		List<IModelCheckJob> removedJobs = new ArrayList<>();
		removedJobs.addAll(currentJobs);
		currentTrace.getStateSpace().sendInterrupt();
		currentJobThreads.removeAll(removedThreads);
		currentJobs.removeAll(removedJobs);
	}
	
	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}
	
	public void checkItem(SymbolicFormulaItem item, AbstractCommand cmd, final StateSpace stateSpace, boolean checkAll) {
		final SymbolicFormulaItem currentItem = getItemIfAlreadyExists(item);
		Thread checkingThread = new Thread(() -> {
			RuntimeException exception = null;
			try {
				stateSpace.execute(cmd);
			} catch (RuntimeException e) {
				LOGGER.error("Exception during symbolic checking", e);
				exception = e;
			}
			Thread currentThread = Thread.currentThread();
			final RuntimeException finalException = exception;
			Platform.runLater(() -> {
				injector.getInstance(StatsView.class).update(currentTrace.get());
				if (finalException == null) {
					resultHandler.handleFormulaResult(currentItem, cmd);
				} else {
					resultHandler.handleFormulaResult(currentItem, finalException);
				}
				updateMachine(currentProject.getCurrentMachine());
				currentJobThreads.remove(currentThread);
				if(!checkAll) {
					updateTrace(item);
				}
			});
		}, "Symbolic Formula Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	public void checkItem(IModelCheckJob checker, SymbolicFormulaItem item, boolean checkAll) {
		Thread checkingThread = new Thread(() -> {
			currentJobs.add(checker);
			Object result;
			try {
				result = checker.call();
			} catch (Exception e) {
				LOGGER.error("Could not check CBC Deadlock", e);
				result = e;
			}
			final Object finalResult = result;
			Platform.runLater(() -> {
				injector.getInstance(StatsView.class).update(currentTrace.get());
				resultHandler.handleFormulaResult(item, finalResult);
				updateMachine(currentProject.getCurrentMachine());
				if(!checkAll) {
					updateTrace(item);
				}
			});
			currentJobs.remove(checker);
			currentJobThreads.remove(Thread.currentThread());
		}, "Symbolic Formula Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	protected abstract void updateTrace(SymbolicFormulaItem item);
	
	protected abstract void updateMachine(Machine machine);
	
	private SymbolicFormulaItem getItemIfAlreadyExists(SymbolicFormulaItem item) {
		List<? extends SymbolicFormulaItem> formulas = getItems();
		int index = formulas.indexOf(item);
		if(index > -1) {
			item = formulas.get(index);
		}
		return item;
	}
	
	private List<? extends SymbolicFormulaItem> getItems() {
		Machine currentMachine = currentProject.getCurrentMachine();
		List<? extends SymbolicFormulaItem> formulas;
		if(resultHandler instanceof SymbolicCheckingResultHandler) {
			formulas = currentMachine.getSymbolicCheckingFormulas();
		} else {
			formulas = currentMachine.getSymbolicAnimationFormulas();
		}
		return formulas;
	}
}
