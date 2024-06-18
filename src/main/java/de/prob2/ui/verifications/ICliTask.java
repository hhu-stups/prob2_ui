package de.prob2.ui.verifications;

import java.util.concurrent.CompletableFuture;

import de.prob2.ui.vomanager.IValidationTask;

/**
 * Helper/mixin interface for simple {@link IValidationTask}s that run entirely on the CLI executor.
 * This is only an implementation helper -
 * other code should not rely on the fact that a task class implements {@link ICliTask}.
 * To execute a generic {@link IValidationTask},
 * call {@link IValidationTask#execute(CheckingExecutors, ExecutionContext)},
 * not the {@link #execute(ExecutionContext)} method from this interface!
 */
public interface ICliTask extends IValidationTask {
	/**
	 * Execute this task synchronously on the current thread
	 * (which will always be the CLI executor thread).
	 * 
	 * @param context the project/animator context in which to execute this task
	 */
	void execute(ExecutionContext context);
	
	@Override
	default CompletableFuture<?> execute(CheckingExecutors executors, ExecutionContext context) {
		return executors.cliExecutor().submit(() -> this.execute(context));
	}
}
