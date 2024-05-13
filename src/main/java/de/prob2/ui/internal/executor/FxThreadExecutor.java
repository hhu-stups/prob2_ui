package de.prob2.ui.internal.executor;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.application.Platform;

/**
 * Executor service that runs tasks on the JavaFX application thread using {@link Platform#runLater(Runnable)}.
 */
@Singleton
public final class FxThreadExecutor extends AbstractExecutorService implements CompletableExecutorService {
	@Inject
	public FxThreadExecutor() {}
	
	@Override
	public void shutdown() {
		throw new UnsupportedOperationException("Cannot shut down the JavaFX application thread executor");
	}
	
	@Override
	public List<Runnable> shutdownNow() {
		throw new UnsupportedOperationException("Cannot shut down the JavaFX application thread executor");
	}
	
	@Override
	public boolean isShutdown() {
		return false;
	}
	
	@Override
	public boolean isTerminated() {
		return false;
	}
	
	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return false;
	}
	
	@Override
	protected <T> CompletableFutureTask<T> newTaskFor(Runnable runnable, T value) {
		return this.newTaskFor(Executors.callable(runnable, value));
	}
	
	@Override
	protected <T> CompletableFutureTask<T> newTaskFor(Callable<T> callable) {
		return new CompletableFutureTask<>(callable);
	}
	
	@Override
	public void execute(Runnable command) {
		Platform.runLater(command);
	}
	
	@Override
	public CompletableFuture<?> submit(Runnable task) {
		return (CompletableFuture<?>)super.submit(task);
	}
	
	@Override
	public <T> CompletableFuture<T> submit(Runnable task, T result) {
		return (CompletableFuture<T>)super.submit(task, result);
	}
	
	@Override
	public <T> CompletableFuture<T> submit(Callable<T> task) {
		return (CompletableFuture<T>)super.submit(task);
	}
}
