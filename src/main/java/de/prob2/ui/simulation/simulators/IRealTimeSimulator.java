package de.prob2.ui.simulation.simulators;

import de.prob.statespace.Trace;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;

import java.io.File;

public interface IRealTimeSimulator {

    void initSimulator(File file);

    void simulate();

    Trace setupBeforeSimulation(Trace trace);

    void run();

    BooleanProperty runningPropertyProperty();

    boolean isRunning();

    int getDelay();

    void updateRemainingTime(int delay);

    void updateDelay();

    void stop();

    IntegerProperty timeProperty();

    boolean isFinished();

}
