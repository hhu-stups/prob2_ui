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

    private RealTimeSimulator realTimeSimulator;

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
                	realTimeSimulator.setupBeforeSimulation(to);
                }
            }
        };
        disablePropertyController.addDisableExpression(this.executingOperationProperty);
    }

    public void run() {
        runningProperty.set(true);
        Trace trace = currentTrace.get();
		realTimeSimulator.setupBeforeSimulation(trace);
        currentTrace.addListener(listener);
        thread = new Thread(() -> {
            while(realTimeSimulator.isRunning()) {
                try {
                    Thread.sleep(realTimeSimulator.getDelay());
                    realTimeSimulator.simulate();
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

    public void setSimulator(RealTimeSimulator realTimeSimulator) {
        this.realTimeSimulator = realTimeSimulator;
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
