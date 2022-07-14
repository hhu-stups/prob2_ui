package de.prob2.ui.internal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Variant of {@link ThreadPoolExecutor} that returns {@link CompletableFuture}s instead of plain {@link Future}s.
 * The {@link CompletableFuture}s returned by this executor support cancellation properly,
 * unlike the standard {@link CompletableFuture} class.
 */
public final class CompletableThreadPoolExecutor extends ThreadPoolExecutor implements CompletableExecutorService {
	public CompletableThreadPoolExecutor(
		final int corePoolSize,
		final int maximumPoolSize,
		final long keepAliveTime,
		final TimeUnit unit,
		final BlockingQueue<Runnable> workQueue
	) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}
	
	public CompletableThreadPoolExecutor(
		final int corePoolSize,
		final int maximumPoolSize,
		final long keepAliveTime,
		final TimeUnit unit,
		final BlockingQueue<Runnable> workQueue,
		final ThreadFactory threadFactory
	) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
	}
	
	public CompletableThreadPoolExecutor(
		final int corePoolSize,
		final int maximumPoolSize,
		final long keepAliveTime,
		final TimeUnit unit,
		final BlockingQueue<Runnable> workQueue,
		final RejectedExecutionHandler handler
	) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}
	
	public CompletableThreadPoolExecutor(
		final int corePoolSize,
		final int maximumPoolSize,
		final long keepAliveTime,
		final TimeUnit unit,
		final BlockingQueue<Runnable> workQueue,
		final ThreadFactory threadFactory,
		final RejectedExecutionHandler handler
	) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}
	
	public static CompletableThreadPoolExecutor newSingleThreadedExecutor(final ThreadFactory threadFactory) {
		return new CompletableThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory);
	}
	
	public static CompletableThreadPoolExecutor newSingleThreadedExecutor() {
		return newSingleThreadedExecutor(Executors.defaultThreadFactory());
	}
	
	@Override
	protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
		return this.newTaskFor(Executors.callable(runnable, value));
	}
	
	@Override
	protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable) {
		return new CompletableFutureTask<>(callable);
	}
	
	@Override
	public CompletableFuture<?> submit(final Runnable task) {
		return (CompletableFuture<?>)super.submit(task);
	}
	
	@Override
	public <T> CompletableFuture<T> submit(final Runnable task, final T result) {
		return (CompletableFuture<T>)super.submit(task, result);
	}
	
	@Override
	public <T> CompletableFuture<T> submit(final Callable<T> task) {
		return (CompletableFuture<T>)super.submit(task);
	}
}
