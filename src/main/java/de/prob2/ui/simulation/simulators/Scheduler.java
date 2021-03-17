package de.prob2.ui.simulation.simulators;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Singleton
public class Scheduler {

    private final ChangeListener<Trace> listener;

    private final Timer timer;

    private RealTimeSimulator realTimeSimulator;

    private final CurrentTrace currentTrace;

    private final BooleanProperty runningProperty;

    private final BooleanProperty executingOperationProperty;

    @Inject
    public Scheduler(final CurrentTrace currentTrace, final DisablePropertyController disablePropertyController) {
        super();
        this.timer = new Timer(true);
        this.currentTrace = currentTrace;
        this.runningProperty = new SimpleBooleanProperty(false);
        this.executingOperationProperty = new SimpleBooleanProperty(false);
        this.listener = (observable, from, to) -> {
            if(to != null) {
                if (!to.getCurrentState().isInitialised()) {
                	realTimeSimulator.setupBeforeSimulation(to);
                }
            }
        };
        disablePropertyController.addDisableExpression(this.executingOperationProperty);
    }

    public void run() {
        runningProperty.set(true);
        Trace trace;
        if(realTimeSimulator.getTime() > 0) {
            trace = currentTrace.get();
        } else {
            trace = new Trace(currentTrace.getStateSpace());
            currentTrace.set(trace);
        }
		realTimeSimulator.setupBeforeSimulation(trace);
        currentTrace.addListener(listener);
        startSimulationLoop();
    }

    private void startSimulationLoop() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(realTimeSimulator.isRunning()) {
                    realTimeSimulator.simulate();
                }
                if(runningProperty.get()) {
                    startSimulationLoop();
                }
            }
        }, realTimeSimulator.getDelay());
    }

    public void setSimulator(RealTimeSimulator realTimeSimulator) {
        this.realTimeSimulator = realTimeSimulator;
    }

    public void stop() {
        currentTrace.removeListener(listener);
        runningProperty.set(false);
    }

    public BooleanProperty runningPropertyProperty() {
        return runningProperty;
    }

    public boolean isRunning() {
        return runningProperty.get();
    }

    public void startSimulationStep() {
        executingOperationProperty.set(true);
    }

    public void endSimulationStep() {
        executingOperationProperty.set(false);
    }

    public void finish() {
        currentTrace.removeListener(listener);
        executingOperationProperty.set(false);
    }

    public void stopTimer() {
        timer.purge();
    }

}
