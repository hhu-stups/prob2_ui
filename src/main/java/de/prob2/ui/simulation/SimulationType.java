package de.prob2.ui.simulation;

public enum SimulationType {

    // Timing: needs simulation with ending condition, user can check timing value (playable)
    // Model Checking: uses B machine + simulation configuration to apply model checking on a reduced state space (counter-examples playable)
    // Probabilistic Model Checking: Same as Model Checking, carries probability (counter-examples playable)
    // Hypothesis Test: Number of Simulations, Store traces with result, Apply different kind of hypothesis test on results (playable)
    // Trace Replay: requires a trace (playable)

    TIMING("Timing"), MODEL_CHECKING("Model Checking"), PROBABILISTIC_MODEL_CHECKING("Probabilistic Model Checking"),
    MONTE_CARLO_SIMULATION("Monte Carlo Simulation"), TRACE_REPLAY("Trace Replay");

    private String name;

    SimulationType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
