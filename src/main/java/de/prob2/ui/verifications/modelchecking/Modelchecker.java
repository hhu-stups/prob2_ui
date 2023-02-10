package de.prob2.ui.verifications.modelchecking;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.stats.StatsView;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@Singleton
public class Modelchecker {
	private final CliTaskExecutor executor;

	private final StatsView statsView;

	private final Injector injector;

	@Inject
	private Modelchecker(final CliTaskExecutor executor, final StatsView statsView, final Injector injector) {
		this.executor = executor;
		this.statsView = statsView;
		this.injector = injector;
	}

	/**
	 * Start model checking using the given configuration.
	 * If a result (success or error) has already been found using the configuration,
	 * return that result directly instead of restarting the check.
	 * 
	 * @param item the model checking configuration to run
	 * @return result of the model check
	 */
	public CompletableFuture<ModelCheckingJobItem> startCheckIfNeeded(final ModelCheckingItem item, final StateSpace stateSpace) {
		if (item.getItems().isEmpty()) {
			return this.startNextCheckStep(item, stateSpace);
		} else {
			return CompletableFuture.completedFuture(item.getItems().get(0));
		}
	}

	public CompletableFuture<ModelCheckingJobItem> startNextCheckStep(ModelCheckingItem item, StateSpace stateSpace) {
		// The options must be calculated before adding the ModelCheckingJobItem,
		// so that the recheckExisting/INSPECT_EXISTING_NODES option is set correctly,
		// which depends on whether any job items were already added.
		final ModelCheckingOptions options = item.getFullOptions(stateSpace.getModel());
		
		final int jobItemListIndex = item.getItems().size();
		final ModelCheckingJobItem initialJobItem = new ModelCheckingJobItem(new NotYetFinished("Starting model check...", Integer.MAX_VALUE), 0, null, BigInteger.ZERO, stateSpace);
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
				final ModelCheckingJobItem jobItem = new ModelCheckingJobItem(result, timeElapsed, stats, cmd.getResult(), stateSpace);
				lastJobItem.set(jobItem);
				Platform.runLater(() -> item.getItems().set(jobItemListIndex, jobItem));
			}

			@Override
			public void isFinished(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
				this.updateStats(jobId, timeElapsed, result, stats);
			}
		};
		final ConsistencyChecker checker = new ConsistencyChecker(stateSpace, options, listener);

		return this.executor.submit(() -> {
			checker.call();
			return lastJobItem.get();
		});
	}

	private void showResult(ModelCheckingItem item, ModelCheckingJobItem jobItem) {
		ModelcheckingView modelCheckingView = injector.getInstance(ModelcheckingView.class);
		List<ModelCheckingJobItem> jobItems = item.getItems();
		jobItems.add(jobItem);
		modelCheckingView.selectItem(item);
		modelCheckingView.selectJobItem(jobItem);
	}
}
