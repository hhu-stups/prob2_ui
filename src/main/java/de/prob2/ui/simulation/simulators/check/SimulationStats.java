package de.prob2.ui.simulation.simulators.check;

public class SimulationStats {

    private final int numberSimulations;

    private final int numberSuccess;

    private final double percentage;

    public SimulationStats(int numberSimulations, int numberSuccess, double percentage) {
        this.numberSimulations = numberSimulations;
        this.numberSuccess = numberSuccess;
        this.percentage = percentage;
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
}
