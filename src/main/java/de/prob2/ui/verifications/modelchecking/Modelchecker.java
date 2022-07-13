package de.prob2.ui.verifications.modelchecking;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.GetStatisticsCommand;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.NotYetFinished;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.stats.StatsView;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

@Singleton
public class Modelchecker {
	private final SetProperty<Future<?>> currentTasks;
	private final ListeningExecutorService executor;

	private final CurrentTrace currentTrace;

	private final StatsView statsView;

	private final Injector injector;

	@Inject
	private Modelchecker(final CurrentTrace currentTrace,
						 final StopActions stopActions, final StatsView statsView, final Injector injector) {
		this.currentTrace = currentTrace;
		this.statsView = statsView;
		this.injector = injector;
		this.currentTasks = new SimpleSetProperty<>(this, "currentTasks", FXCollections.observableSet(new CopyOnWriteArraySet<>()));
		this.executor = MoreExecutors.listeningDecorator(
			Executors.newSingleThreadExecutor(r -> new Thread(r, "Model Checker"))
		);
		stopActions.add(this.executor::shutdownNow);
	}

	public BooleanExpression runningProperty() {
		return this.currentTasks.emptyProperty().not();
	}

	public boolean isRunning() {
		return this.runningProperty().get();
	}

	public ListenableFuture<ModelCheckingJobItem> startModelchecking(ModelCheckingItem item) {
		final StateSpace stateSpace = currentTrace.getStateSpace();
		final int jobItemListIndex = item.getItems().size();
		final int jobItemDisplayIndex = jobItemListIndex + 1;
		final ModelCheckingJobItem initialJobItem = new ModelCheckingJobItem(jobItemDisplayIndex, new NotYetFinished("Starting model check...", Integer.MAX_VALUE), 0, null, BigInteger.ZERO, stateSpace);
		final ObjectProperty<ModelCheckingJobItem> lastJobItem = new SimpleObjectProperty<>(initialJobItem);
		showResult(item, initialJobItem);
		
		final IModelCheckListener listener = new IModelCheckListener() {
			@Override
			public void updateStats(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
				// Command must be executed outside of Platform.runLater to avoid blocking the UI thread!
				GetStatisticsCommand cmd = new GetStatisticsCommand(GetStatisticsCommand.StatisticsOption.MEMORY_USED);
				stateSpace.execute(cmd);
				if (stats != null) {
					statsView.updateSimpleStats(stats);
				}
				final ModelCheckingJobItem jobItem = new ModelCheckingJobItem(jobItemDisplayIndex, result, timeElapsed, stats, cmd.getResult(), stateSpace);
				lastJobItem.set(jobItem);
				Platform.runLater(() -> item.getItems().set(jobItemListIndex, jobItem));
			}

			@Override
			public void isFinished(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
				this.updateStats(jobId, timeElapsed, result, stats);
			}
		};
		final ModelCheckingOptions options = item.getFullOptions(stateSpace.getModel());
		final ConsistencyChecker checker = new ConsistencyChecker(stateSpace, options, listener);

		final ListenableFuture<ModelCheckingJobItem> future = this.executor.submit(() -> {
			checker.call();
			return lastJobItem.get();
		});
		this.currentTasks.add(future);
		future.addListener(() -> this.currentTasks.remove(future), MoreExecutors.directExecutor());
		return future;
	}

	public void cancelModelcheck() {
		this.currentTasks.forEach(task -> task.cancel(true));
		currentTrace.getStateSpace().sendInterrupt();
	}

	private void showResult(ModelCheckingItem item, ModelCheckingJobItem jobItem) {
		ModelcheckingView modelCheckingView = injector.getInstance(ModelcheckingView.class);
		List<ModelCheckingJobItem> jobItems = item.getItems();
		jobItems.add(jobItem);
		modelCheckingView.selectItem(item);
		modelCheckingView.selectJobItem(jobItem);
	}
}
