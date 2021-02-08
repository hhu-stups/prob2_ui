package de.prob2.ui.simulation.simulators.check;

import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import org.apache.commons.math3.analysis.function.Gaussian;

import java.util.List;
import java.util.Map;

public class SimulationHypothesisChecker extends AbstractSimulationMonteCarlo {

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

    private HypothesisCheckResult result;

    public SimulationHypothesisChecker(final CurrentTrace currentTrace, final Trace trace, final int numberExecutions, final SimulationCheckingType type,
									   final HypothesisCheckingType hypothesisCheckingType, final double probability, final Map<String, Object> additionalInformation) {
        super(currentTrace, trace, numberExecutions, type, additionalInformation);
		this.hypothesisCheckingType = hypothesisCheckingType;
		this.probability = probability;
		this.result = HypothesisCheckResult.NOT_FINISHED;
    }

	private void checkTwoTailed() {
		int n = resultingTraces.size();
		double p = probability;
    	double mu = Math.round(n * p);
    	double sigma = Math.sqrt(n * p * (1 - p));
		Gaussian gaussian = new Gaussian(mu, sigma);
		double coverage = 0.0;

		int range = 0;

		for(int i = 1; i <= Math.ceil(mu/2); i++) {
			if(100.0 - coverage < p) {
				range = i;
				break;
			}
			coverage = gaussian.value(mu + i + 0.5) - gaussian.value(mu - i - 0.5);
		}
		int numberFailed = n - numberSuccess;
		if(numberFailed >= mu - range && numberFailed <= mu + range) {
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
		Gaussian gaussian = new Gaussian(mu, sigma);
		double coverage = 0.0;

		int range = 0;

		if(p >= 0.5) {
			for (int i = 1; i <= mu; i++) {
				if (100.0 - coverage < p) {
					range = i;
					break;
				}
				coverage = 100.0 - gaussian.value(mu - i - 0.5);
			}
		} else {
			for (int i = 1; i <= mu; i++) {
				if (100.0 - coverage >= p) {
					range = i;
					break;
				}
				coverage = 100.0 - gaussian.value(mu + i - 0.5);
			}
		}
		int numberFailed = n - numberSuccess;
		if(numberFailed >= mu - range && numberFailed <= n) {
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
		Gaussian gaussian = new Gaussian(mu, sigma);
		double coverage = 0.0;

		int range = 0;

		if(p >= 0.5) {
			for (int i = 1; i <= mu; i++) {
				if (100.0 - coverage < p) {
					range = i;
					break;
				}
				coverage = gaussian.value(mu + i + 0.5);
			}
		} else {
			for (int i = 1; i <= mu; i++) {
				if (100.0 - coverage >= p) {
					range = i;
					break;
				}
				coverage = gaussian.value(mu - i + 0.5);
			}
		}
		int numberFailed = n - numberSuccess;
		if(numberFailed >= 0 && numberFailed <= mu + range) {
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

	public HypothesisCheckResult getResult() {
		return result;
	}
}
