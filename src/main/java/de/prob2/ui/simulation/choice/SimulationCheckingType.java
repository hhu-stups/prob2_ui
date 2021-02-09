package de.prob2.ui.simulation.choice;

public enum SimulationCheckingType {
    ALL_INVARIANTS("All Invariants"),
    PREDICATE_INVARIANT("Predicate as Invariant"),
    PREDICATE_FINAL("Final Predicate"),
    PREDICATE_EVENTUALLY("Predicate Eventually"),
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
