package de.prob2.ui.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Variant of {@link ExecutorService} that returns {@link CompletableFuture}s instead of plain {@link Future}s.
 */
public interface CompletableExecutorService extends ExecutorService {
	@Override
	<T> CompletableFuture<T> submit(Callable<T> task);
	
	@Override
	<T> CompletableFuture<T> submit(Runnable task, T result);
	
	@Override
	CompletableFuture<?> submit(Runnable task);
}
