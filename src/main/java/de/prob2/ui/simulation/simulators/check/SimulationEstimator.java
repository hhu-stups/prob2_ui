package de.prob2.ui.simulation.simulators.check;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.inject.Injector;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.simulators.Simulator;
import de.prob2.ui.verifications.CheckingStatus;

public class SimulationEstimator implements ISimulationPropertyChecker {

	public enum EstimationType {
		MINIMUM("simulation.type.estimation.minimum"),
		MAXIMUM("simulation.type.estimation.maximum"),
		MEAN("simulation.type.estimation.mean");

		private final String key;

		EstimationType(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}

		public String getName(I18n i18n) {
			return i18n.translate(key);
		}

		public String getShortKey() {
			switch (this) {
				case MINIMUM:
					return "simulation.type.estimation.minimum.short";
				case MAXIMUM:
					return "simulation.type.estimation.maximum.short";
				case MEAN:
					return "simulation.type.estimation.mean.short";
				default:
					break;
			}
			return "";
		}
	}

	private SimulationPropertyChecker simulationPropertyChecker;

	private final Injector injector;

	private final I18n i18n;

	private final EstimationType estimationType;

	private final SimulationCheckingType checkingType;

	private final double desiredValue;

	private final double epsilon;

	public SimulationEstimator(final Injector injector, final I18n i18n, final EstimationType estimationType,
							   final SimulationCheckingType checkingType, final double desiredValue, final double epsilon) {
		this.injector = injector;
		this.i18n = i18n;
		this.estimationType = estimationType;
		this.checkingType = checkingType;
		this.desiredValue = desiredValue;
		this.epsilon = epsilon;
	}

	public void initialize(final CurrentTrace currentTrace, final int numberExecutions, final int maxStepsBeforeProperty, final SimulationCheckingType type, final Map<String, Object> additionalInformation) {
		this.simulationPropertyChecker = new SimulationPropertyChecker(this, injector, currentTrace, numberExecutions, maxStepsBeforeProperty, type, additionalInformation);
	}

	private void checkMinimum() {
		List<Trace> resultingTraces = simulationPropertyChecker.getResultingTraces();
		double sum;
		if(checkingType == SimulationCheckingType.AVERAGE || checkingType == SimulationCheckingType.AVERAGE_MEAN_BETWEEN_STEPS ||
				checkingType == SimulationCheckingType.SUM || checkingType == SimulationCheckingType.SUM_MEAN_BETWEEN_STEPS ||
				checkingType == SimulationCheckingType.MINIMUM || checkingType == SimulationCheckingType.MINIMUM_MEAN_BETWEEN_STEPS ||
				checkingType == SimulationCheckingType.MAXIMUM || checkingType == SimulationCheckingType.MAXIMUM_MEAN_BETWEEN_STEPS) {
			sum = simulationPropertyChecker.getEstimatedValues().stream()
					.reduce(0.0, Double::sum);
		} else {
			sum = simulationPropertyChecker.getNumberSuccess();
		}
		int n = resultingTraces.size();
		double estimatedValue = sum / n;
		if(estimatedValue >= desiredValue - epsilon) {
			this.setResult(SimulationCheckingSimulator.MonteCarloCheckResult.SUCCESS);
		} else {
			this.setResult(SimulationCheckingSimulator.MonteCarloCheckResult.FAIL);
		}
	}

	private boolean checkMinimum(double estimatedValue, double desiredValue, double epsilon) {
		return estimatedValue >= desiredValue - epsilon;
	}

	private void checkMaximum() {
		List<Trace> resultingTraces = simulationPropertyChecker.getResultingTraces();
		double sum;
		if(checkingType == SimulationCheckingType.AVERAGE || checkingType == SimulationCheckingType.AVERAGE_MEAN_BETWEEN_STEPS ||
				checkingType == SimulationCheckingType.SUM || checkingType == SimulationCheckingType.SUM_MEAN_BETWEEN_STEPS ||
				checkingType == SimulationCheckingType.MINIMUM || checkingType == SimulationCheckingType.MINIMUM_MEAN_BETWEEN_STEPS ||
				checkingType == SimulationCheckingType.MAXIMUM || checkingType == SimulationCheckingType.MAXIMUM_MEAN_BETWEEN_STEPS) {
			sum = simulationPropertyChecker.getEstimatedValues().stream()
					.reduce(0.0, Double::sum);
		} else {
			sum = simulationPropertyChecker.getNumberSuccess();
		}
		int n = resultingTraces.size();
		double estimatedValue = sum / n;
		if(checkMaximum(estimatedValue, desiredValue, epsilon)) {
			this.setResult(SimulationCheckingSimulator.MonteCarloCheckResult.SUCCESS);
		} else {
			this.setResult(SimulationCheckingSimulator.MonteCarloCheckResult.FAIL);
		}
	}

	private boolean checkMaximum(double estimatedValue, double desiredValue, double epsilon) {
		return estimatedValue <= desiredValue + epsilon;
	}

	private void checkMean() {
		List<Trace> resultingTraces = simulationPropertyChecker.getResultingTraces();
		double sum;
		if(checkingType == SimulationCheckingType.AVERAGE || checkingType == SimulationCheckingType.AVERAGE_MEAN_BETWEEN_STEPS ||
				checkingType == SimulationCheckingType.SUM || checkingType == SimulationCheckingType.SUM_MEAN_BETWEEN_STEPS ||
				checkingType == SimulationCheckingType.MINIMUM || checkingType == SimulationCheckingType.MINIMUM_MEAN_BETWEEN_STEPS ||
				checkingType == SimulationCheckingType.MAXIMUM || checkingType == SimulationCheckingType.MAXIMUM_MEAN_BETWEEN_STEPS) {
			sum = simulationPropertyChecker.getEstimatedValues().stream()
					.reduce(0.0, Double::sum);
		} else {
			sum = simulationPropertyChecker.getNumberSuccess();
		}
		int n = resultingTraces.size();
		double estimatedValue = sum / n;
		if(checkMean(estimatedValue, desiredValue, epsilon)) {
			this.setResult(SimulationCheckingSimulator.MonteCarloCheckResult.SUCCESS);
		} else {
			this.setResult(SimulationCheckingSimulator.MonteCarloCheckResult.FAIL);
		}
	}

	private boolean checkMean(double estimatedValue, double desiredValue, double epsilon) {
		return estimatedValue >= desiredValue - epsilon && estimatedValue <= desiredValue + epsilon;
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

	public boolean check(double estimatedValue, double desiredValue, double epsilon) {
		switch (estimationType) {
			case MINIMUM:
				return checkMinimum(estimatedValue, desiredValue, epsilon);
			case MAXIMUM:
				return checkMaximum(estimatedValue, desiredValue, epsilon);
			case MEAN:
				return checkMean(estimatedValue, desiredValue, epsilon);
			default:
				break;
		}
		return false;
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
	public List<CheckingStatus> getResultingStatus() {
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

	public void calculateStatistics(long time) {
		double wallTime = new BigDecimal(time / 1000.0f).setScale(3, RoundingMode.HALF_UP).doubleValue();
		List<Trace> resultingTraces = simulationPropertyChecker.getResultingTraces();
		int numberSuccess = simulationPropertyChecker.getNumberSuccess();

		int n = resultingTraces.size();
		double ratio = (double) numberSuccess / n;
		List<Integer> traceLengths = resultingTraces.stream().map(Trace::size).collect(Collectors.toList());
		this.setStats(new SimulationStats(n, numberSuccess, ratio, simulationPropertyChecker.getEstimatedValues(), wallTime, traceLengths, this.calculateExtendedStats()));
	}

	@Override
	public Map<String, List<Integer>> getOperationExecutions() {
		return simulationPropertyChecker.getOperationExecutions();
	}

	@Override
	public void setStats(SimulationStats stats) {
		simulationPropertyChecker.setStats(stats);
	}

	@Override
	public void run() {
		simulationPropertyChecker.run(this);
	}

	@Override
	public CheckingStatus checkTrace(Trace trace, int time) {
		return simulationPropertyChecker.checkTrace(trace, time);
	}

	public Simulator getSimulator() {
		return simulationPropertyChecker.getSimulator();
	}
}
