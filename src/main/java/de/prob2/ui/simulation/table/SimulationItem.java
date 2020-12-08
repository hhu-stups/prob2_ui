package de.prob2.ui.simulation.table;

public class SimulationItem {

    // Timing: needs simulation with ending condition, user can check timing value (playable)
	// Model Checking: uses B machine + simulation configuration to apply model checking on a reduced state space (counter-examples playable)
	// Probabilistic Model Checking: Same as Model Checking, carries probability (counter-examples playable)
	// Hypothesis Test: Number of Simulations, Store traces with result, Apply different kind of hypothesis test on results (playable)
	// Trace Replay: requires a trace (playable)

    /*
    starting condition to start timer for timing - also part of configuration file to search for starting state of simulation
    TIMING option has time value to be checked
    */

    public enum SimulationType {
        TIMING, MODEL_CHECKING, PROBABILISTIC_MODEL_CHECKING, HYPOTHESIS_TEST, TRACE_REPLAY
    }

    private SimulationType type;



}
