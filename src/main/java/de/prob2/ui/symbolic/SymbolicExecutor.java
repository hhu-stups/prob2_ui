package de.prob2.ui.symbolic;

import com.google.inject.Injector;

import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.binding.BooleanExpression;

public abstract class SymbolicExecutor {
	protected final CurrentTrace currentTrace;
	
	private final CliTaskExecutor cliExecutor;
	
	
	protected SymbolicExecutor(final CurrentTrace currentTrace, final Injector injector) {
		this.currentTrace = currentTrace;
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
}
