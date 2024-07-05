package de.prob2.ui.internal.executor;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.StopActions;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

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
public final class CliTaskExecutor implements CompletableExecutorService {
	private final Set<Future<?>> currentTasks;
	private final BooleanProperty running;
	private final CompletableExecutorService internalExecutor;

	@Inject
	private CliTaskExecutor(final DisablePropertyController disablePropertyController, final StopActions stopActions) {
		this.currentTasks = new CopyOnWriteArraySet<>();
		this.running = new SimpleBooleanProperty(this, "running", false);
		this.internalExecutor = new CompletableThreadPoolExecutor(
			1, 1, 0, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(),
			r -> new Thread(r, "probcli command executor")
		) {
			@Override
			protected <T> CompletableFutureTask<T> newTaskFor(final Callable<T> callable) {
				CompletableFutureTask<T> task = super.newTaskFor(callable);
				task.whenComplete((r, t) -> {
					currentTasks.remove(task);
					updateRunning();
				});
				currentTasks.add(task);
				updateRunning();
				return task;
			}
		};

		disablePropertyController.addDisableExpression(this.runningProperty());
		stopActions.add(this::shutdownNow);
	}

	private void updateRunning() {
		// Update the running property on the UI thread so that it's safe to use in bindings for UI objects.
		Platform.runLater(() -> this.running.set(!this.currentTasks.isEmpty()));
	}

	/**
	 * Return whether any tasks are currently running on this executor,
	 * as an observable/bindable boolean expression.
	 *
	 * @return expression indicating whether any tasks are currently running
	 */
	public BooleanExpression runningProperty() {
		return this.running;
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

	@Override
	public void execute(Runnable command) {
		// Use submit instead of execute so that the Runnable is wrapped in a task.
		// Otherwise it won't be tracked in currentTasks and the running property won't be updated,
		// making it impossible for the user to interrupt the Runnable.
		this.submit(command).exceptionally(exc -> {
			Thread thread = Thread.currentThread();
			thread.getUncaughtExceptionHandler().uncaughtException(thread, exc);
			return null;
		});
	}

	// All other ExecutorService methods just delegate to this.internalExecutor.

	@Override
	public <T> CompletableFuture<T> submit(Callable<T> task) {
		return this.internalExecutor.submit(task);
	}

	@Override
	public <T> CompletableFuture<T> submit(Runnable task, T result) {
		return this.internalExecutor.submit(task, result);
	}

	@Override
	public CompletableFuture<?> submit(Runnable task) {
		return this.internalExecutor.submit(task);
	}

	@Override
	public void shutdown() {
		this.internalExecutor.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return this.internalExecutor.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return this.internalExecutor.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return this.internalExecutor.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return this.internalExecutor.awaitTermination(timeout, unit);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return this.internalExecutor.invokeAll(tasks);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		return this.internalExecutor.invokeAll(tasks, timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return this.internalExecutor.invokeAny(tasks);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return this.internalExecutor.invokeAny(tasks, timeout, unit);
	}
}
