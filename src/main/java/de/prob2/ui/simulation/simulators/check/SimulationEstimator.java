package de.prob2.ui.simulation.simulators.check;

import com.google.inject.Injector;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class SimulationEstimator extends SimulationMonteCarloChecker {

	public enum EstimationType {
		MINIMUM("Minimum estimator"),
		MAXIMUM("Maximum estimator"),
		MEAN("Mean estimator");

		private final String name;

		EstimationType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	private final EstimationType estimationType;

	private final double desiredValue;

	private final double epsilon;

	public SimulationEstimator(final Injector injector, final CurrentTrace currentTrace, int numberExecutions, int maxStepsBeforeProperty, SimulationCheckingType type,
			final EstimationType estimationType, final double desiredValue, final double epsilon, Map<String, Object> additionalInformation) {
		super(injector, currentTrace, numberExecutions, maxStepsBeforeProperty, type, additionalInformation);
		this.estimationType = estimationType;
		this.desiredValue = desiredValue;
		this.epsilon = epsilon;
	}

	private void checkMinimum() {
		int n = resultingTraces.size();
		double ratio = (double) numberSuccess / n;
		if(ratio >= desiredValue - epsilon) {
			this.result = MonteCarloCheckResult.SUCCESS;
		} else {
			this.result = MonteCarloCheckResult.FAIL;
		}
	}

	private void checkMaximum() {
		int n = resultingTraces.size();
		double ratio = (double) numberSuccess / n;
		if(ratio <= desiredValue + epsilon) {
			this.result = MonteCarloCheckResult.SUCCESS;
		} else {
			this.result = MonteCarloCheckResult.FAIL;
		}
	}

	private void checkMean() {
		int n = resultingTraces.size();
		double ratio = (double) numberSuccess / n;
		if(ratio >= desiredValue - epsilon && ratio <= desiredValue + epsilon) {
			this.result = MonteCarloCheckResult.SUCCESS;
		} else {
			this.result = MonteCarloCheckResult.FAIL;
		}
	}

	@Override
	public void check() {
		switch (estimationType) {
			case MINIMUM:
				checkMinimum();
				break;
			case MAXIMUM:
				checkMaximum();
				break;
			case MEAN:
				checkMean();
				break;
			default:
				break;
		}

	}

	@Override
	protected void calculateStatistics(long time) {
		double wallTime = new BigDecimal(time / 1000.0f).setScale(3, RoundingMode.HALF_UP).doubleValue();
		int n = resultingTraces.size();
		double ratio = (double) numberSuccess / n;
		this.stats = new SimulationStats(n, numberSuccess, ratio, wallTime, calculateExtendedStats());
	}

}
