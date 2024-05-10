package de.prob2.ui.internal.executor;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>An executor that runs tasks in a single background thread, and automatically cancels any previous tasks when a new task is submitted.</p>
 *
 * <p>This class is useful for running view update code on a background thread, to avoid blocking the JavaFX application thread if the update code takes a long time to complete. The automatic cancelling of previous tasks avoids redundant updates: for example, when an update task is running and two further update tasks are submitted, the first two tasks are cancelled, because their changes to the UI state would be overwritten by the third task anyway.</p>
 *
 * <p>Note: This automatic cancelling behavior is not useful for all kinds of UI updates. If you do not need this special behavior, use a regular single-thread executor ({@link Executors#newSingleThreadExecutor()}) instead of this class.</p>
 */
public final class BackgroundUpdater implements Executor {
	private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundUpdater.class);
	private static final FutureCallback<Object> THROW_EXCEPTIONS_CALLBACK = new FutureCallback<>() {
		@Override
		public void onSuccess(final Object result) {
		}

		@Override
		public void onFailure(final Throwable t) {
			if (t instanceof CancellationException) {
				LOGGER.trace("Background update thread cancelled (this is not an error)", t);
			} else {
				final Thread thread = Thread.currentThread();
				thread.getUncaughtExceptionHandler().uncaughtException(thread, t);
			}
		}
	};

	private final ListeningExecutorService executor;
	private final Object lock;
	private ListenableFuture<Void> lastFuture;
	private final BooleanProperty running;

	public BackgroundUpdater(final String threadName) {
		super();

		this.executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(r -> new Thread(r, threadName)));
		this.lock = new Object();
		this.lastFuture = Futures.immediateFuture(null);
		this.running = new SimpleBooleanProperty(this, "running", false);
	}

	public ReadOnlyBooleanProperty runningProperty() {
		return this.running;
	}

	public boolean isRunning() {
		return this.runningProperty().get();
	}

	@Override
	public void execute(final Runnable command) {
		this.execute(command, false);
	}

	public void execute(final Runnable command, final boolean mayInterruptIfRunning) {
		synchronized (this.lock) {
			this.cancel(mayInterruptIfRunning);
			this.lastFuture = this.executor.submit(() -> {
				try {
					Platform.runLater(() -> this.running.set(true));
					command.run();
				} finally {
					Platform.runLater(() -> this.running.set(false));
				}
			}, null);
			Futures.addCallback(this.lastFuture, THROW_EXCEPTIONS_CALLBACK, MoreExecutors.directExecutor());
		}
	}

	public void cancel(final boolean mayInterruptIfRunning) {
		synchronized (this.lock) {
			this.lastFuture.cancel(mayInterruptIfRunning);
		}
	}

	public void shutdown() {
		this.executor.shutdown();
	}

	public List<Runnable> shutdownNow() {
		return this.executor.shutdownNow();
	}
}
