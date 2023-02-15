package de.prob2.ui.verifications.modelchecking;

import java.math.BigInteger;

import de.prob.animator.command.GetStatisticsCommand;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.NotYetFinished;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.StateSpace;

import javafx.application.Platform;

public final class Modelchecker {
	private Modelchecker() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Execute model checking using the given configuration.
	 * If a result (success or error) has already been found using the configuration,
	 * return that result directly instead of restarting the check.
	 * 
	 * @param item the model checking configuration to run
	 * @return result of the model check
	 */
	public static ModelCheckingStep executeIfNeeded(final ModelCheckingItem item, final StateSpace stateSpace) {
		if (item.getSteps().isEmpty()) {
			return Modelchecker.execute(item, stateSpace);
		} else {
			return item.getSteps().get(0);
		}
	}

	public static ModelCheckingStep execute(ModelCheckingItem item, StateSpace stateSpace) {
		// The options must be calculated before adding the ModelCheckingStep,
		// so that the recheckExisting/INSPECT_EXISTING_NODES option is set correctly,
		// which depends on whether any steps were already added.
		final ModelCheckingOptions options = item.getFullOptions(stateSpace.getModel());
		
		final int stepIndex = item.getSteps().size();
		final ModelCheckingStep initialStep = new ModelCheckingStep(new NotYetFinished("Starting model check...", Integer.MAX_VALUE), 0, null, BigInteger.ZERO, stateSpace);
		item.getSteps().add(initialStep);
		
		final IModelCheckListener listener = new IModelCheckListener() {
			@Override
			public void updateStats(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
				// Command must be executed outside of Platform.runLater to avoid blocking the UI thread!
				GetStatisticsCommand cmd = new GetStatisticsCommand(GetStatisticsCommand.StatisticsOption.MEMORY_USED);
				stateSpace.execute(cmd);
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

		try {
			item.setCurrentStep(initialStep);
			checker.call();
			return item.getCurrentStep();
		} finally {
			item.setCurrentStep(null);
		}
	}
}
