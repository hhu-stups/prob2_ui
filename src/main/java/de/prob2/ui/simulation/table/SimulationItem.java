package de.prob2.ui.simulation.table;

public class SimulationItem {

    // Timing: starting condition, ending time, ending condition

    /*
    starting condition to start timer for timing - also part of configuration file to search for starting state of simulation
    TIMING option has time value to be checked
    */

    public enum SimulationType {
        STANDARD, TIMING, MODEL_CHECKING, PROBABILISTIC_MODEL_CHECKING, HYPOTHESIS_TEST, TRACE_REPLAY
    }

    private SimulationType type;



}
