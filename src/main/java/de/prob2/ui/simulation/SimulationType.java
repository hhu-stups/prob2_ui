package de.prob2.ui.simulation;

public enum SimulationType {

    // Timing: needs simulation with ending condition, user can check timing value (playable)
    // Hypothesis Test: Number of Simulations, Store traces with result, Apply different kind of hypothesis test
    // Estimation: Number of Simulations, Store traces with result, Apply different kind of estimations
    // Trace Replay: requires a trace (playable)

    TIMING("Timing"),
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
