package de.prob2.ui.simulation;

public enum SimulationType {

    // Timing: needs simulation with ending condition, user can check timing value (playable)
    // Monte Carlo Simulation: Number of Simulations, Store traces with result, Apply different kind of hypothesis test, and estimation on results (playable)
    // Trace Replay: requires a trace (playable)

    TIMING("Timing"), MONTE_CARLO_SIMULATION("Monte Carlo Simulation"), TRACE_REPLAY("Trace Replay");

    private String name;

    SimulationType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
