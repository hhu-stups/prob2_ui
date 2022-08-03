package de.prob2.ui.symbolic;

import java.util.concurrent.CompletableFuture;

import com.google.inject.Injector;

import de.prob.animator.command.AbstractCommand;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckingResult;
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
		final CompletableFuture<AbstractCommand> future = cliExecutor.submit(() -> {
			stateSpace.execute(cmd);
			return cmd;
		});
		future.whenComplete((r, e) -> {
			if (e == null) {
				resultHandler.handleFormulaResult(item, r);
			} else {
				LOGGER.error("Exception during symbolic checking", e);
				resultHandler.handleFormulaResult(item, e);
			}
			if(!checkAll) {
				updateTrace(item);
			}
		});
	}
	
	public void checkItem(IModelCheckJob checker, T item, boolean checkAll) {
		final CompletableFuture<IModelCheckingResult> future = cliExecutor.submit(checker);
		future.whenComplete((r, e) -> {
			if (e == null) {
				resultHandler.handleFormulaResult(item, r);
			} else {
				LOGGER.error("Exception during symbolic checking", e);
				resultHandler.handleFormulaResult(item, e);
			}
			if(!checkAll) {
				updateTrace(item);
			}
		});
	}
	
	protected abstract void updateTrace(T item);
}
