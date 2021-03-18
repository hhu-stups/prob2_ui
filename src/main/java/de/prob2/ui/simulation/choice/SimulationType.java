package de.prob2.ui.simulation.choice;

public enum SimulationType {

    MONTE_CARLO_SIMULATION("Monte Carlo Simulation"),
    HYPOTHESIS_TEST("Hypothesis Test"),
    ESTIMATION("Estimation");

    private final String name;

    SimulationType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
