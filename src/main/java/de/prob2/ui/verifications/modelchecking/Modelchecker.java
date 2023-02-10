package de.prob2.ui.verifications.modelchecking;

import java.math.BigInteger;
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
	public CompletableFuture<ModelCheckingStep> startCheckIfNeeded(final ModelCheckingItem item, final StateSpace stateSpace) {
		if (item.getSteps().isEmpty()) {
			return this.startNextCheckStep(item, stateSpace);
		} else {
			return CompletableFuture.completedFuture(item.getSteps().get(0));
		}
	}

	public CompletableFuture<ModelCheckingStep> startNextCheckStep(ModelCheckingItem item, StateSpace stateSpace) {
		// The options must be calculated before adding the ModelCheckingStep,
		// so that the recheckExisting/INSPECT_EXISTING_NODES option is set correctly,
		// which depends on whether any steps were already added.
		final ModelCheckingOptions options = item.getFullOptions(stateSpace.getModel());
		
		final int stepIndex = item.getSteps().size();
		final ModelCheckingStep initialStep = new ModelCheckingStep(new NotYetFinished("Starting model check...", Integer.MAX_VALUE), 0, null, BigInteger.ZERO, stateSpace);
		item.setCurrentStep(initialStep);
		item.getSteps().add(initialStep);
		injector.getInstance(ModelcheckingView.class).showCurrentStep(item);
		
		final IModelCheckListener listener = new IModelCheckListener() {
			@Override
			public void updateStats(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
				// Command must be executed outside of Platform.runLater to avoid blocking the UI thread!
				GetStatisticsCommand cmd = new GetStatisticsCommand(GetStatisticsCommand.StatisticsOption.MEMORY_USED);
				stateSpace.execute(cmd);
				if (stats != null) {
					statsView.updateSimpleStats(stats);
				}
				final ModelCheckingStep step = new ModelCheckingStep(result, timeElapsed, stats, cmd.getResult(), stateSpace);
				item.setCurrentStep(step);
				Platform.runLater(() -> item.getSteps().set(stepIndex, step));
			}

			@Override
			public void isFinished(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
				this.updateStats(jobId, timeElapsed, result, stats);
			}
		};
		final ConsistencyChecker checker = new ConsistencyChecker(stateSpace, options, listener);

		return this.executor.submit(() -> {
			try {
				checker.call();
				return item.getCurrentStep();
			} finally {
				item.setCurrentStep(null);
			}
		});
	}
}
