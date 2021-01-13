package de.prob2.ui.simulation.choice;

public enum SimulationType {

    // Hypothesis Test: Number of Simulations, Store traces with result, Apply different kind of hypothesis test
    // Estimation: Number of Simulations, Store traces with result, Apply different kind of estimations
    // Trace Replay: requires a trace (playable)

    HYPOTHESIS_TEST("Hypothesis Test"),
    ESTIMATION("Estimation"),
    TRACE_REPLAY("Trace Replay");

    private String name;

    SimulationType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
