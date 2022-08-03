package de.prob2.ui.symbolic;

import com.google.inject.Injector;

import de.prob.animator.command.AbstractCommand;
import de.prob.check.IModelCheckJob;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.binding.BooleanExpression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SymbolicExecutor<T extends SymbolicItem<?>> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicExecutor.class);
	
	protected final CurrentTrace currentTrace;
	
	protected final ISymbolicResultHandler<T> resultHandler;
	
	private final CliTaskExecutor cliExecutor;
	
	
	public SymbolicExecutor(final CurrentTrace currentTrace, final ISymbolicResultHandler<T> resultHandler, final Injector injector) {
		this.currentTrace = currentTrace;
		this.resultHandler = resultHandler;
		this.cliExecutor = injector.getInstance(CliTaskExecutor.class);
		injector.getInstance(DisablePropertyController.class).addDisableExpression(this.runningProperty());
	}
	
	public void interrupt() {
		cliExecutor.interruptAll();
		currentTrace.getStateSpace().sendInterrupt();
	}
	
	public BooleanExpression runningProperty() {
		return cliExecutor.runningProperty();
	}
	
	public boolean isRunning() {
		return this.runningProperty().get();
	}
	
	public void checkItem(T item, AbstractCommand cmd, final StateSpace stateSpace, boolean checkAll) {
		cliExecutor.submit(() -> {
			RuntimeException exception = null;
			try {
				stateSpace.execute(cmd);
			} catch (RuntimeException e) {
				LOGGER.error("Exception during symbolic checking", e);
				exception = e;
			}
			if (exception == null) {
				resultHandler.handleFormulaResult(item, cmd);
			} else {
				resultHandler.handleFormulaResult(item, exception);
			}
			if(!checkAll) {
				updateTrace(item);
			}
		});
	}
	
	public void checkItem(IModelCheckJob checker, T item, boolean checkAll) {
		cliExecutor.submit(() -> {
			Object result;
			try {
				result = checker.call();
			} catch (Exception e) {
				LOGGER.error("Could not check CBC Deadlock", e);
				result = e;
			}
			resultHandler.handleFormulaResult(item, result);
			if(!checkAll) {
				updateTrace(item);
			}
		});
	}
	
	protected abstract void updateTrace(T item);
}
