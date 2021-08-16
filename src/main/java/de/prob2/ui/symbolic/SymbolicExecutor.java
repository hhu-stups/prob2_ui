package de.prob2.ui.symbolic;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Injector;

import de.prob.animator.command.AbstractCommand;
import de.prob.check.IModelCheckJob;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SymbolicExecutor<T extends SymbolicItem<?>> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicExecutor.class);
	
	protected final CurrentTrace currentTrace;
	
	protected final ISymbolicResultHandler<T> resultHandler;

	protected final List<IModelCheckJob> currentJobs;
	
	protected final ListProperty<Thread> currentJobThreads;
	
	
	public SymbolicExecutor(final CurrentTrace currentTrace, final ISymbolicResultHandler<T> resultHandler, final Injector injector) {
		this.currentTrace = currentTrace;
		this.resultHandler = resultHandler;
		this.currentJobs = new ArrayList<>();
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
		injector.getInstance(DisablePropertyController.class).addDisableExpression(this.runningProperty());
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
	
	public void checkItem(T item, AbstractCommand cmd, final StateSpace stateSpace, boolean checkAll) {
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
					resultHandler.handleFormulaResult(item, cmd);
				} else {
					resultHandler.handleFormulaResult(item, finalException);
				}
				if(!checkAll) {
					updateTrace(item);
				}
			});
			currentJobThreads.remove(currentThread);
		}, "Symbolic Formula Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	public void checkItem(IModelCheckJob checker, T item, boolean checkAll) {
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
	
	protected abstract void updateTrace(T item);
}
