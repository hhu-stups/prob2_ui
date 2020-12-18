package de.prob2.ui.simulation.simulators;

import de.prob.statespace.Trace;
import javafx.beans.property.BooleanProperty;

public interface IRealTimeSimulator {

    void simulate();

    Trace setupBeforeSimulation(Trace trace);

    BooleanProperty runningPropertyProperty();

    boolean isRunning();

}
