package de.prob2.ui.simulation.simulators.check;

import com.google.inject.Injector;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.simulators.Simulator;
import de.prob2.ui.verifications.Checked;

import java.util.List;
import java.util.Map;

public class SimulationEstimator implements ISimulationPropertyChecker {

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

	private ISimulationPropertyChecker simulationPropertyChecker;

	// TODO: BlackBoxChecker

	private final Injector injector;

	private final EstimationType estimationType;

	private final double desiredValue;

	private final double epsilon;

	private SimulationStats stats;

	private SimulationMonteCarlo.MonteCarloCheckResult result;

	public SimulationEstimator(final Injector injector, final EstimationType estimationType, final double desiredValue, final double epsilon) {
		this.injector = injector;
		this.estimationType = estimationType;
		this.desiredValue = desiredValue;
		this.epsilon = epsilon;
	}

	public void initializeMonteCarlo(final CurrentTrace currentTrace, final int numberExecutions, final int maxStepsBeforeProperty, final SimulationCheckingType type, final Map<String, Object> additionalInformation) {
		this.simulationPropertyChecker = new SimulationMonteCarloChecker(injector, currentTrace, numberExecutions, maxStepsBeforeProperty, type, additionalInformation);
	}

	private void checkMinimum() {
		List<Trace> resultingTraces = simulationPropertyChecker.getResultingTraces();
		int numberSuccess = simulationPropertyChecker.getNumberSuccess();
		int n = resultingTraces.size();
		double ratio = (double) numberSuccess / n;
		if(ratio >= desiredValue - epsilon) {
			this.result = SimulationMonteCarlo.MonteCarloCheckResult.SUCCESS;
		} else {
			this.result = SimulationMonteCarlo.MonteCarloCheckResult.FAIL;
		}
	}

	private void checkMaximum() {
		List<Trace> resultingTraces = simulationPropertyChecker.getResultingTraces();
		int numberSuccess = simulationPropertyChecker.getNumberSuccess();
		int n = resultingTraces.size();
		double ratio = (double) numberSuccess / n;
		if(ratio <= desiredValue + epsilon) {
			this.result = SimulationMonteCarlo.MonteCarloCheckResult.SUCCESS;
		} else {
			this.result = SimulationMonteCarlo.MonteCarloCheckResult.FAIL;
		}
	}

	private void checkMean() {
		List<Trace> resultingTraces = simulationPropertyChecker.getResultingTraces();
		int numberSuccess = simulationPropertyChecker.getNumberSuccess();
		int n = resultingTraces.size();
		double ratio = (double) numberSuccess / n;
		if(ratio >= desiredValue - epsilon && ratio <= desiredValue + epsilon) {
			this.result = SimulationMonteCarlo.MonteCarloCheckResult.SUCCESS;
		} else {
			this.result = SimulationMonteCarlo.MonteCarloCheckResult.FAIL;
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
	public SimulationMonteCarlo.MonteCarloCheckResult getResult() {
		return simulationPropertyChecker.getResult();
	}

	public void setResult(SimulationMonteCarlo.MonteCarloCheckResult result) {
		this.result = result;
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
	public void run() {
		if(simulationPropertyChecker instanceof Simulator) {
			((Simulator) simulationPropertyChecker).run(this);
			return;
		}
		simulationPropertyChecker.run();
		setResult(simulationPropertyChecker.getResult());
	}

	@Override
	public Checked checkTrace(Trace trace, int time) {
		return simulationPropertyChecker.checkTrace(trace, time);
	}

	public Simulator getSimulator() {
		if(simulationPropertyChecker instanceof SimulationMonteCarloChecker) {
			return ((SimulationMonteCarloChecker) simulationPropertyChecker).getSimulator();
		}
		return null;
	}
}
