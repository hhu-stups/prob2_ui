package de.prob2.ui.simulation.simulators.check;

public class SimulationStats {

	private final int numberSimulations;

	private final int numberSuccess;

	private final double percentage;

	private final double estimatedValue;

	private final double wallTime;

	private final double averageTraceLength;

	private final SimulationExtendedStats extendedStats;

	public SimulationStats(int numberSimulations, int numberSuccess, double percentage, double estimatedValue,
			double wallTime, double averageTraceLength, SimulationExtendedStats extendedStats) {
		this.numberSimulations = numberSimulations;
		this.numberSuccess = numberSuccess;
		this.estimatedValue = estimatedValue;
		this.percentage = percentage;
		this.wallTime = wallTime;
		this.averageTraceLength = averageTraceLength;
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

	public double getEstimatedValue() {
		return estimatedValue;
	}

	public double getWallTime() {
		return wallTime;
	}

	public double getAverageTraceLength() {
		return averageTraceLength;
	}

	public SimulationExtendedStats getExtendedStats() {
		return extendedStats;
	}
}
