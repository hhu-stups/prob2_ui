package de.prob2.ui.simulation.simulators.check;

import java.util.List;

public class SimulationStats {

	private final int numberSimulations;

	private final int numberSuccess;

	private final double percentage;

	private final List<Double> estimatedValues;

	private final double wallTime;

	private final List<Integer> traceLengths;

	private final SimulationExtendedStats extendedStats;

	public SimulationStats(int numberSimulations, int numberSuccess, double percentage, List<Double> estimatedValues,
			double wallTime, List<Integer> traceLengths, SimulationExtendedStats extendedStats) {
		this.numberSimulations = numberSimulations;
		this.numberSuccess = numberSuccess;
		this.estimatedValues = estimatedValues;
		this.percentage = percentage;
		this.wallTime = wallTime;
		this.traceLengths = traceLengths;
		this.extendedStats = extendedStats;
	}

	public int getNumberSimulations() {
		return numberSimulations;
	}

	public int getNumberSuccess() {
		return numberSuccess;
	}

	public double getPercentage() {
		return percentage;
	}

	public List<Double> getEstimatedValues() {
		return estimatedValues;
	}

	public double getEstimatedValue() {
		return estimatedValues.isEmpty() ? 0 : estimatedValues.stream().reduce(0.0, Double::sum) / estimatedValues.size();
	}

	public double getWallTime() {
		return wallTime;
	}

	public List<Integer> getTraceLengths() {
		return traceLengths;
	}

	public double getAverageTraceLength() {
		return traceLengths.stream().reduce(0, Integer::sum) / (double) traceLengths.size();
	}

	public SimulationExtendedStats getExtendedStats() {
		return extendedStats;
	}
}
