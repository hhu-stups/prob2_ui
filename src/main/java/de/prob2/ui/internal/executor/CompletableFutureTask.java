package de.prob2.ui.internal.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import com.google.common.util.concurrent.Futures;

/**
 * Combination of {@link FutureTask} and {@link CompletableFuture}.
 * Represents a concrete computation that can be run and cancelled,
 * but also supports all the usual {@link CompletableFuture} chaining methods.
 *
 * @param <T> Result type of the computation.
 */
public class CompletableFutureTask<T> extends CompletableFuture<T> implements RunnableFuture<T> {
	private final FutureTask<T> task;

	CompletableFutureTask(final Callable<T> callable) {
		this.task = new FutureTask<>(callable) {
			@Override
			protected void done() {
				if (this.isCancelled()) {
					CompletableFutureTask.super.cancel(true);
				} else {
					try {
						CompletableFutureTask.this.complete(Futures.getDone(this));
					} catch (ExecutionException e) {
						CompletableFutureTask.this.completeExceptionally(e.getCause());
					}
				}
			}
		};
	}

	@Override
	public void run() {
		this.task.run();
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		// super.cancel(true) will be called by task.done()
		return this.task.cancel(mayInterruptIfRunning);
	}
}
