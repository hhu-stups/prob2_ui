package de.prob2.ui.verifications.modelchecking;

import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.GetStatisticsCommand;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.stats.StatsView;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Modelchecker {

	private static final Logger LOGGER = LoggerFactory.getLogger(Modelchecker.class);

	private final SetProperty<Future<Void>> currentTasks;
	private final ExecutorService executor;

	private final StageManager stageManager;

	private final CurrentTrace currentTrace;

	private final StatsView statsView;

	private final Injector injector;

	@Inject
	private Modelchecker(final StageManager stageManager, final CurrentTrace currentTrace,
						 final StopActions stopActions, final StatsView statsView, final Injector injector) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.statsView = statsView;
		this.injector = injector;
		this.currentTasks = new SimpleSetProperty<>(this, "currentTasks", FXCollections.observableSet(new CopyOnWriteArraySet<>()));
		this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Model Checker"));
		stopActions.add(this.executor::shutdownNow);
	}

	public void checkItem(ModelCheckingItem item, boolean recheckExisting, boolean checkAll) {
		if(!item.selected()) {
			return;
		}

		final FutureTask<Void> task = new FutureTask<Void>(() -> {
			startModelchecking(item, recheckExisting, checkAll);
			return null;
		}) {
			@Override
			protected void done() {
				super.done();
				currentTasks.remove(this);
			}
		};
		this.currentTasks.add(task);
		this.executor.submit(task);
	}

	public BooleanExpression runningProperty() {
		return this.currentTasks.emptyProperty().not();
	}

	public boolean isRunning() {
		return this.runningProperty().get();
	}

	private void startModelchecking(ModelCheckingItem item, boolean recheckExisting, boolean checkAll) {
		final StateSpace stateSpace = currentTrace.getStateSpace();
		final ModelcheckingView modelcheckingView = injector.getInstance(ModelcheckingView.class);
		final IModelCheckListener listener = new IModelCheckListener() {
			@Override
			public void updateStats(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
				// Command must be executed outside of Platform.runLater to avoid blocking the UI thread!
				GetStatisticsCommand cmd = new GetStatisticsCommand(GetStatisticsCommand.StatisticsOption.MEMORY_USED);
				stateSpace.execute(cmd);
				Platform.runLater(() -> modelcheckingView.showStats(timeElapsed, stats, cmd.getResult()));
				if (stats != null) {
					statsView.updateSimpleStats(stats);
				}
			}

			@Override
			public void isFinished(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
				// Command must be executed outside of Platform.runLater to avoid blocking the UI thread!
				GetStatisticsCommand cmd = new GetStatisticsCommand(GetStatisticsCommand.StatisticsOption.MEMORY_USED);
				stateSpace.execute(cmd);
				Platform.runLater(() -> modelcheckingView.showStats(timeElapsed, stats, cmd.getResult()));
				if (stats != null) {
					statsView.updateSimpleStats(stats);
				}
				// TODO Store memory usage in ModelCheckingJobItem
				final ModelCheckingJobItem jobItem = new ModelCheckingJobItem(item.getItems().size() + 1, result, timeElapsed, stats, stateSpace);
				if (!checkAll && jobItem.getResult() instanceof ITraceDescription) {
					currentTrace.set(jobItem.getTrace());
				}
				Platform.runLater(() -> showResult(item, jobItem));
			}
		};
		final ModelCheckingOptions options = item.getFullOptions(stateSpace.getModel()).recheckExisting(recheckExisting);
		IModelCheckJob job = new ConsistencyChecker(stateSpace, options, listener);

		try {
			job.call();
		} catch (Exception e) {
			LOGGER.error("Exception while running model check job", e);
			Platform.runLater(() -> stageManager.makeExceptionAlert(e, "verifications.modelchecking.modelchecker.alerts.exceptionWhileRunningJob.content").show());
		}
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
