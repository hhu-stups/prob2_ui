package de.prob2.ui.verifications;

import java.util.concurrent.CompletableFuture;

import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.BooleanProperty;

public interface IExecutableItem extends IValidationTask {
	boolean selected();
	BooleanProperty selectedProperty();
	void setSelected(boolean selected);
	void execute(ExecutionContext context);
	
	@Override
	default CompletableFuture<?> execute(CheckingExecutors executors, ExecutionContext context) {
		return executors.cliExecutor().submit(() -> this.execute(context));
	}
}
