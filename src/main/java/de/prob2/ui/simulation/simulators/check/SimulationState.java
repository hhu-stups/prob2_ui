package de.prob2.ui.simulation.simulators.check;

import de.prob.statespace.State;

import java.util.Map;
import java.util.Objects;

public class SimulationState {

    private final State bState;

    private final Map<String, Integer> operationToRemainingTime;

    public SimulationState(State bState, Map<String, Integer> operationToRemainingTime) {
        this.bState = bState;
        this.operationToRemainingTime = operationToRemainingTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bState, operationToRemainingTime);
    }

    @Override
    public String toString() {
        return "{State ID: " + bState.getId()  + ", " + operationToRemainingTime + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof SimulationState)) {
            return false;
        }
        if(this == obj) {
            return true;
        }
        SimulationState otherState = (SimulationState) obj;
        return bState.equals(otherState.getBState()) && operationToRemainingTime.equals(otherState.getOperationToRemainingTime());
    }

    public State getBState() {
        return bState;
    }

    public Map<String, Integer> getOperationToRemainingTime() {
        return operationToRemainingTime;
    }

}
