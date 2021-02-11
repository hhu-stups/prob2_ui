package de.prob2.ui.simulation.simulators.check;

import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import org.apache.commons.math3.special.Erf;

import java.util.Map;

public class SimulationHypothesisChecker extends AbstractSimulationMonteCarlo {

	public static class DistributionFunction {

		private double mu;

		private double sigma;

		public DistributionFunction(double mu, double sigma) {
			this.mu = mu;
			this.sigma = sigma;
		}

		public double value(double x) {
			return 0.5 * (1 + Erf.erf((x - mu)/(Math.sqrt(2*sigma*sigma))));
		}
	}

	public enum HypothesisCheckingType {
		LEFT_TAILED("Left-tailed hypothesis test"),
		RIGHT_TAILED("Right-tailed hypothesis test"),
		TWO_TAILED("Two-tailed hypothesis test");

		private String name;

		HypothesisCheckingType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

    public enum HypothesisCheckResult {
        NOT_FINISHED, SUCCESS, FAIL
    }

	private final HypothesisCheckingType hypothesisCheckingType;

	private final double probability;

	private final double faultTolerance;

    private HypothesisCheckResult result;

    public SimulationHypothesisChecker(final CurrentTrace currentTrace, final Trace trace, final int numberExecutions, final SimulationCheckingType type,
									   final HypothesisCheckingType hypothesisCheckingType, final double probability, final double faultTolerance, final Map<String, Object> additionalInformation) {
        super(currentTrace, trace, numberExecutions, type, additionalInformation);
		this.hypothesisCheckingType = hypothesisCheckingType;
		this.probability = probability;
		this.faultTolerance = faultTolerance;
		this.result = HypothesisCheckResult.NOT_FINISHED;
    }

	private void checkTwoTailed() {
		int n = resultingTraces.size();
		double p = probability;
    	double mu = Math.round(n * p);
    	double sigma = Math.sqrt(n * p * (1 - p));
		DistributionFunction dsf = new DistributionFunction(mu, sigma);
		double coverage = 0.0;

		int range = 0;

		for(int i = 0; i <= n; i++) {
			if(1.0 - coverage < faultTolerance) {
				range = i;
				break;
			}
			coverage = dsf.value(mu + i + 0.5) - dsf.value(mu - i - 0.5);
		}
		if(numberSuccess >= mu - range && numberSuccess <= mu + range) {
			this.result = HypothesisCheckResult.SUCCESS;
		} else {
			this.result = HypothesisCheckResult.FAIL;
		}
	}

	private void checkLeftTailed() {
		int n = resultingTraces.size();
		double p = probability;
		double mu = Math.round(n * p);
		double sigma = Math.sqrt(n * p * (1 - p));
		DistributionFunction dsf = new DistributionFunction(mu, sigma);
		double coverage = 0.0;

		int range = 0;

		for (int i = 0; i <= n; i++) {
			if (1.0 - coverage < faultTolerance) {
				range = i;
				break;
			}
			coverage = 1.0 - dsf.value(mu - i - 0.5);
		}

		if(numberSuccess >= mu - range && numberSuccess <= n) {
			this.result = HypothesisCheckResult.SUCCESS;
		} else {
			this.result = HypothesisCheckResult.FAIL;
		}
	}

	private void checkRightTailed() {
		int n = resultingTraces.size();
		double p = probability;
		int mu = (int) Math.round(n * p);
		double sigma = Math.sqrt(n * p * (1 - p));
		DistributionFunction dsf = new DistributionFunction(mu, sigma);
		double coverage = 0.0;

		int range = 0;

		for (int i = 0; i <= n; i++) {
			if (1.0 - coverage < faultTolerance) {
				range = i;
				break;
			}
			coverage = dsf.value(mu + i + 0.5);
		}

		if(numberSuccess >= 0 && numberSuccess <= mu + range) {
			this.result = HypothesisCheckResult.SUCCESS;
		} else {
			this.result = HypothesisCheckResult.FAIL;
		}
	}

    public void check() {
    	switch (hypothesisCheckingType) {
			case LEFT_TAILED:
				checkLeftTailed();
				break;
			case RIGHT_TAILED:
				checkRightTailed();
				break;
			case TWO_TAILED:
				checkTwoTailed();
				break;
			default:
				break;
		}
    }

	@Override
	protected void calculateStatistics() {
		int n = resultingTraces.size();
		double ratio = (double) numberSuccess / n;
		this.stats = new SimulationStats(n, numberSuccess, ratio, calculateExtendedStats());
	}

	public HypothesisCheckResult getResult() {
		return result;
	}

	public SimulationStats getStats() {
		return stats;
	}
}
