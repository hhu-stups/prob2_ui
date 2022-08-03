package de.prob2.ui.internal.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.StopActions;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

/**
 * <p>
 * Shared executor for tasks that occupy probcli for a significant time.
 * This executor is single-threaded,
 * because probcli itself cannot execute multiple commands in parallel
 * and as a result such tasks cannot be parallelized anyway.
 * </p>
 * <p>
 * Do <em>not</em> use this for tasks that perform a lot of computation on the Java side!
 * Such tasks are better run on their own background executor
 * so that they can be parallelized and don't block other probcli-bound tasks.
 * </p>
 * <p>
 * Tasks running on this executor should be interruptable (via {@link Thread#interrupt()})
 * so that the user can abort hanging probcli commands.
 * </p>
 */
@Singleton
public final class CliTaskExecutor extends CompletableThreadPoolExecutor {
	private final SetProperty<Future<?>> currentTasks;

	@Inject
	private CliTaskExecutor(final DisablePropertyController disablePropertyController, final StopActions stopActions) {
		super(
				1, 1, 0, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(),
				r -> new Thread(r, "probcli command executor")
		);

		this.currentTasks = new SimpleSetProperty<>(this, "currentTasks", FXCollections.observableSet(new CopyOnWriteArraySet<>()));

		disablePropertyController.addDisableExpression(this.runningProperty());
		stopActions.add(this::shutdownNow);
	}

	@Override
	protected <T> CompletableFutureTask<T> newTaskFor(final Callable<T> callable) {
		final CompletableFutureTask<T> task = super.newTaskFor(callable);
		task.whenComplete((r, t) -> this.currentTasks.remove(task));
		this.currentTasks.add(task);
		return task;
	}

	/**
	 * Return whether any tasks are currently running on this executor,
	 * as an observable/bindable boolean expression.
	 *
	 * @return expression indicating whether any tasks are currently running
	 */
	public BooleanExpression runningProperty() {
		return this.currentTasks.emptyProperty().not();
	}

	/**
	 * Return whether any tasks are currently running on this executor.
	 *
	 * @return whether any tasks are currently running
	 */
	public boolean isRunning() {
		return this.runningProperty().get();
	}

	/**
	 * Cancel and interrupt all tasks currently running (or queued to run) on this executor.
	 */
	public void interruptAll() {
		this.currentTasks.forEach(task -> task.cancel(true));
	}
}
