package de.prob2.ui.simulation.choice;

public enum SimulationCheckingType {
    ALL_INVARIANTS("All Invariants"),
    INVARIANT("Invariant"),
    ALMOST_CERTAIN_PROPERTY("Almost-certain property"),
    TIMING("Timing");

    private String name;

    SimulationCheckingType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
