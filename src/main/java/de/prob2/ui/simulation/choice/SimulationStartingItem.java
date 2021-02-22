package de.prob2.ui.simulation.choice;

import de.prob2.ui.simulation.simulators.check.SimulationMonteCarlo;
import javafx.beans.NamedArg;

public class SimulationStartingItem {

    private SimulationMonteCarlo.StartingType startingType;

    public SimulationStartingItem(@NamedArg("startingType") SimulationMonteCarlo.StartingType startingType) {
        this.startingType = startingType;
    }

    @Override
    public String toString() {
        return startingType.getName();
    }

    public SimulationMonteCarlo.StartingType getStartingType() {
        return startingType;
    }

}
