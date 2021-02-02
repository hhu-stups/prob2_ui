package de.prob2.ui.simulation.simulators;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

@Singleton
public class Scheduler {

    private Thread thread;

    private ChangeListener<Trace> listener;

    private Simulator simulator;

    private final CurrentTrace currentTrace;

    private final BooleanProperty runningProperty;

    private final BooleanProperty executingOperationProperty;

    @Inject
    public Scheduler(final CurrentTrace currentTrace, final DisablePropertyController disablePropertyController) {
        super();
        this.currentTrace = currentTrace;
        this.runningProperty = new SimpleBooleanProperty(false);
        this.executingOperationProperty = new SimpleBooleanProperty(false);
        this.listener = (observable, from, to) -> {
            if(to != null) {
                if (!to.getCurrentState().isInitialised()) {
                    Trace newTrace = simulator.setupBeforeSimulation(to);
                    currentTrace.set(newTrace);
                }
            }
        };
        disablePropertyController.addDisableExpression(this.executingOperationProperty);
    }

    public void run() {
        runningProperty.set(true);
        Trace trace = currentTrace.get();
        currentTrace.set(simulator.setupBeforeSimulation(trace));
        currentTrace.addListener(listener);
        thread = new Thread(() -> {
            while(simulator.isRunning()) {
                try {
                    Thread.sleep(simulator.getDelay());
                    simulator.simulate();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(thread.isInterrupted()) {
                    return;
                }
            }
        });
        thread.start();
    }

    public void setSimulator(Simulator simulator) {
        this.simulator = simulator;
    }

    public void stop() {
        if(thread != null) {
            thread.interrupt();
        }
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

}
