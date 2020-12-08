package de.prob2.ui.simulation.table;

public class SimulationItem {

    // Standard: ending time, ending condition
    // Timing: starting condition, ending time, ending condition
    // Model Checking: ending time, ending condition
    // Probabilistic Model Checking: ending time, ending condition
    // Hypothesis test/Monte Carlo simulation: ending time, ending condition
    // Trace replay: ending time, ending condition

    /*
    add ending time and ending condition to configuration file
    starting condition to start timer for timing - also part of configuration file to search for starting state of simulation
    TIMING option has time value to be checked
    */

    public enum SimulationType {
        STANDARD, TIMING, MODEL_CHECKING, PROBABILISTIC_MODEL_CHECKING, HYPOTHESIS_TEST, TRACE_REPLAY
    }

    private SimulationType type;



}
