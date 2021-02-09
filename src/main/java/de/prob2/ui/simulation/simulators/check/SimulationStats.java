package de.prob2.ui.simulation.simulators.check;

public class SimulationStats {

    private final int numberSimulations;

    private final int numberSuccess;

    private final double percentage;

    private final SimulationExtendedStats extendedStats;


    public SimulationStats(int numberSimulations, int numberSuccess, double percentage,
                           SimulationExtendedStats extendedStats) {
        this.numberSimulations = numberSimulations;
        this.numberSuccess = numberSuccess;
        this.percentage = percentage;
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

    public SimulationExtendedStats getExtendedStats() {
        return extendedStats;
    }
}
