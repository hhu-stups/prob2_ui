package de.prob2.ui.vomanager;

public enum RequirementType {
    INVARIANT("Invariant Requirement"),
    DEADLOCK_FREEDOM("Deadlock-freedom Requirement"),
    SAFETY("Safety Requirement"),
    LIVENESS("Liveness Requirement"),
    FAIRNESS("Fairness Requirement"),
    USE_CASE("Use Case Requirement");

    private final String name;

    RequirementType(String name) {
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