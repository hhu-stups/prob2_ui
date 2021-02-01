package de.prob2.ui.simulation.simulators;

import de.prob.statespace.Trace;
import de.prob2.ui.simulation.configuration.ConfigurationCheckingError;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;

import java.io.File;
import java.io.IOException;

public interface IRealTimeSimulator {

    void initSimulator(File file) throws IOException, ConfigurationCheckingError;

    void simulate();

    Trace setupBeforeSimulation(Trace trace);

    void run();

    BooleanProperty runningProperty();

    boolean isRunning();

    int getDelay();

    void updateRemainingTime(int delay);

    void updateDelay();

    void stop();

    IntegerProperty timeProperty();

    int getTime();

    boolean endingConditionReached(Trace trace);

}
