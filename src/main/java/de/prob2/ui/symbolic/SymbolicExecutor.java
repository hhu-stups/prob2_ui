package de.prob2.ui.symbolic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.inject.Injector;

import de.prob.animator.command.AbstractCommand;
import de.prob.check.IModelCheckJob;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingResultHandler;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SymbolicExecutor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicExecutor.class);
	
	protected final CurrentTrace currentTrace;
	
	protected final CurrentProject currentProject;
	
	protected final ISymbolicResultHandler resultHandler;

	protected final List<IModelCheckJob> currentJobs;
	
	protected final ListProperty<Thread> currentJobThreads;
	
	
	public SymbolicExecutor(final CurrentTrace currentTrace, final CurrentProject currentProject,
							final ISymbolicResultHandler resultHandler, final Injector injector) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
		this.currentJobs = new ArrayList<>();
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
		injector.getInstance(DisablePropertyController.class).addDisableExpression(this.runningProperty());
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
		List<IModelCheckJob> removedJobs = new ArrayList<>(currentJobs);
		currentTrace.getStateSpace().sendInterrupt();
		currentJobThreads.removeAll(removedThreads);
		currentJobs.removeAll(removedJobs);
	}
	
	public BooleanExpression runningProperty() {
		return currentJobThreads.emptyProperty().not();
	}
	
	public boolean isRunning() {
		return this.runningProperty().get();
	}
	
	public void checkItem(SymbolicItem item, AbstractCommand cmd, final StateSpace stateSpace, boolean checkAll) {
		final SymbolicItem currentItem = getItemIfAlreadyExists(item);
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
				if (finalException == null) {
					resultHandler.handleFormulaResult(currentItem, cmd);
				} else {
					resultHandler.handleFormulaResult(currentItem, finalException);
				}
				if(!checkAll) {
					updateTrace(currentItem);
				}
			});
			currentJobThreads.remove(currentThread);
		}, "Symbolic Formula Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	public void checkItem(IModelCheckJob checker, SymbolicItem item, boolean checkAll) {
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
				resultHandler.handleFormulaResult(item, finalResult);
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
	
	protected abstract void updateTrace(SymbolicItem item);
	
	protected SymbolicItem getItemIfAlreadyExists(SymbolicItem item) {
		final Optional<? extends SymbolicItem> existing = getItems()
			.stream()
			.filter(item::settingsEqual)
			.findAny();
		// Cannot use existing.orElse(item) here,
		// because of generic type problems with "? extends SymbolicItem".
		return existing.isPresent() ? existing.get() : item;
	}
	
	private List<? extends SymbolicItem> getItems() {
		Machine currentMachine = currentProject.getCurrentMachine();
		List<? extends SymbolicItem> formulas;
		if(resultHandler instanceof SymbolicCheckingResultHandler) {
			formulas = currentMachine.getSymbolicCheckingFormulas();
		} else {
			formulas = currentMachine.getSymbolicAnimationFormulas();
		}
		return formulas;
	}
}
