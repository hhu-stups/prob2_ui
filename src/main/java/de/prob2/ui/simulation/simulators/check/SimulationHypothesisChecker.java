package de.prob2.ui.simulation.simulators.check;

import com.google.inject.Injector;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.simulators.Simulator;
import de.prob2.ui.verifications.Checked;
import org.apache.commons.math3.special.Erf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public class SimulationHypothesisChecker implements ISimulationPropertyChecker {

	public static class DistributionFunction {

		private final double mu;

		private final double sigma;

		public DistributionFunction(double mu, double sigma) {
			this.mu = mu;
			this.sigma = sigma;
		}

		public double value(double x) {
			return 0.5 * (1 + Erf.erf((x - mu)/(Math.sqrt(2*sigma*sigma))));
		}
	}

	public static class Distribution {

		private final int n;
		private final double p;
		private final double mu;
		private final double sigma;
		private final DistributionFunction dsf;

		public Distribution(int n, double p) {
			this.n = n;
			this.p = p;
			this.mu = n * p;
			this.sigma = Math.sqrt(n * p * (1 - p));
			this.dsf = new DistributionFunction(mu, sigma);
		}

		public int getN() {
			return n;
		}

		public double getP() {
			return p;
		}

		public double getMu() {
			return mu;
		}

		public double getSigma() {
			return sigma;
		}

		public DistributionFunction getDistributionFunction() {
			return dsf;
		}

		public double calculateCoverage(HypothesisCheckingType checkingType, int epsilon) {
			switch (checkingType) {
				case TWO_TAILED:
					return dsf.value(mu + epsilon + 0.5) - dsf.value(mu - epsilon - 0.5);
				case LEFT_TAILED:
					return 1.0 - dsf.value(mu - epsilon - 0.5);
				case RIGHT_TAILED:
					return dsf.value(mu + epsilon + 0.5);
				default:
					break;
			}
			return 0.0;
		}

		public boolean isSuccess(HypothesisCheckingType checkingType, int numberSuccess, int range) {
			switch (checkingType) {
				case TWO_TAILED:
					return numberSuccess >= mu - range && numberSuccess <= mu + range;
				case LEFT_TAILED:
					return numberSuccess >= mu - range && numberSuccess <= n;
				case RIGHT_TAILED:
					return numberSuccess >= 0 && numberSuccess <= mu + range;
				default:
					break;
			}
			return false;
		}
	}

	public enum HypothesisCheckingType {
		LEFT_TAILED("Left-tailed hypothesis test"),
		RIGHT_TAILED("Right-tailed hypothesis test"),
		TWO_TAILED("Two-tailed hypothesis test");

		private final String name;

		HypothesisCheckingType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	private SimulationPropertyChecker simulationPropertyChecker;

	private final Injector injector;

	private final HypothesisCheckingType hypothesisCheckingType;

	private final double probability;

	private final double significance;

	public SimulationHypothesisChecker(final Injector injector, final HypothesisCheckingType hypothesisCheckingType, final double probability, final double significance) {
		this.injector = injector;
		this.hypothesisCheckingType = hypothesisCheckingType;
		this.probability = probability;
		this.significance = significance;
	}

	public void initialize(final CurrentTrace currentTrace, final int numberExecutions, final int maxStepsBeforeProperty, final SimulationCheckingType type, final Map<String, Object> additionalInformation) {
		this.simulationPropertyChecker = new SimulationPropertyChecker(injector, currentTrace, numberExecutions, maxStepsBeforeProperty, type, additionalInformation);
	}

	@Override
	public void check() {
		List<Trace> resultingTraces = simulationPropertyChecker.getResultingTraces();
		Distribution distribution = new Distribution(resultingTraces.size(), probability);
		double coverage;
		int range = 0;

		for (int i = 0; i <= distribution.getN(); i++) {
			coverage = distribution.calculateCoverage(hypothesisCheckingType, i);
			if (1.0 - coverage < significance) {
				range = i;
				break;
			}
		}

		if(distribution.isSuccess(hypothesisCheckingType, simulationPropertyChecker.getNumberSuccess(), range)) {
			this.setResult(SimulationCheckingSimulator.MonteCarloCheckResult.SUCCESS);
		} else {
			this.setResult(SimulationCheckingSimulator.MonteCarloCheckResult.FAIL);
		}
	}

	@Override
	public Checked checkTrace(Trace trace, int time) {
		return simulationPropertyChecker.checkTrace(trace, time);
	}

	@Override
	public List<Trace> getResultingTraces() {
		return simulationPropertyChecker.getResultingTraces();
	}

	@Override
	public List<List<Integer>> getResultingTimestamps() {
		return simulationPropertyChecker.getResultingTimestamps();
	}

	@Override
	public List<Checked> getResultingStatus() {
		return simulationPropertyChecker.getResultingStatus();
	}

	@Override
	public SimulationStats getStats() {
		return simulationPropertyChecker.getStats();
	}

	@Override
	public SimulationCheckingSimulator.MonteCarloCheckResult getResult() {
		return simulationPropertyChecker.getResult();
	}

	@Override
	public void setResult(SimulationCheckingSimulator.MonteCarloCheckResult result) {
		simulationPropertyChecker.setResult(result);
	}

	@Override
	public int getNumberSuccess() {
		return simulationPropertyChecker.getNumberSuccess();
	}

	@Override
	public SimulationExtendedStats calculateExtendedStats() {
		return simulationPropertyChecker.calculateExtendedStats();
	}

	@Override
	public void calculateStatistics(long time) {
		double wallTime = new BigDecimal(time / 1000.0f).setScale(3, RoundingMode.HALF_UP).doubleValue();
		List<Trace> resultingTraces = simulationPropertyChecker.getResultingTraces();
		int numberSuccess = simulationPropertyChecker.getNumberSuccess();
		int n = resultingTraces.size();
		double ratio = (double) numberSuccess / n;
		this.setStats(new SimulationStats(n, numberSuccess, ratio, wallTime, this.calculateExtendedStats()));
	}

	@Override
	public void setStats(SimulationStats stats) {
		simulationPropertyChecker.setStats(stats);
	}

	@Override
	public void run() {
		simulationPropertyChecker.run(this);
	}

	public Simulator getSimulator() {
		return simulationPropertyChecker.getSimulator();
	}
}
