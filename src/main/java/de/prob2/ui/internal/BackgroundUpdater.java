package de.prob2.ui.internal;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * <p>An executor that runs tasks in a single background thread, and automatically cancels any previous tasks when a new task is submitted.</p>
 * 
 * <p>This class is useful for running view update code on a background thread, to avoid blocking the JavaFX application thread if the update code takes a long time to complete. The automatic cancelling of previous tasks avoids redundant updates: for example, when an update task is running and two further update tasks are submitted, the first two tasks are cancelled, because their changes to the UI state would be overwritten by the third task anyway.</p>
 * 
 * <p>Note: This automatic cancelling behavior is not useful for all kinds of UI updates. If you do not need this special behavior, use a regular single-thread executor ({@link Executors#newSingleThreadExecutor()}) instead of this class.</p>
 */
public final class BackgroundUpdater implements Executor {
	private static final FutureCallback<Object> THROW_EXCEPTIONS_CALLBACK = new FutureCallback<Object>() {
		@Override
		public void onSuccess(final Object result) {}
		
		@Override
		public void onFailure(final Throwable t) {
			final Thread thread = Thread.currentThread();
			thread.getUncaughtExceptionHandler().uncaughtException(thread, t);
		}
	};
	
	private final ListeningExecutorService executor;
	private final Object lock;
	private ListenableFuture<Void> lastFuture;
	
	public BackgroundUpdater(final String threadName) {
		super();
		
		this.executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(r -> new Thread(r, threadName)));
		this.lock = new Object();
		this.lastFuture = Futures.immediateFuture(null);
	}
	
	@Override
	public void execute(final Runnable command) {
		synchronized (this.lock) {
			this.lastFuture.cancel(false);
			this.lastFuture = this.executor.submit(command, null);
			Futures.addCallback(this.lastFuture, THROW_EXCEPTIONS_CALLBACK, MoreExecutors.directExecutor());
		}
	}
	
	public void shutdown() {
		this.executor.shutdown();
	}
	
	public List<Runnable> shutdownNow() {
		return this.executor.shutdownNow();
	}
}
