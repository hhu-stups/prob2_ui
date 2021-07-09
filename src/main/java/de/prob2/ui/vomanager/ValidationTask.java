package de.prob2.ui.vomanager;

public enum ValidationTask {
    MODEL_CHECKING("Model Checking"),
    LTL_MODEL_CHECKING("LTL Model Checking"),
    SYMBOLIC_MODEL_CHECKING("Symbolic Model Checking"),
    TRACE_REPLAY("Trace Replay"),
    SIMULATION("Simulation");

    private final String name;

    ValidationTask(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}
