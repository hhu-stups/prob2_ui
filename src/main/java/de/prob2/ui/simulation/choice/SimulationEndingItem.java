package de.prob2.ui.simulation.choice;

import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import de.prob2.ui.simulation.simulators.check.SimulationMonteCarlo;
import javafx.beans.NamedArg;

public class SimulationEndingItem {

    private SimulationMonteCarlo.EndingType endingType;

    public SimulationEndingItem(@NamedArg("endingType") SimulationMonteCarlo.EndingType endingType) {
        this.endingType = endingType;
    }

    @Override
    public String toString() {
        return endingType.getName();
    }

    public SimulationMonteCarlo.EndingType getEndingType() {
        return endingType;
    }

}
