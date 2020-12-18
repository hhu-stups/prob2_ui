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

@Singleton
public class Scheduler {

    private ChangeListener<Trace> listener;

    private TimerTask task;

    private Timer timer;

    private IRealTimeSimulator simulator;

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

    public void run(int interval) {
        this.timer = new Timer();
        runningProperty.set(true);
        Trace trace = currentTrace.get();
        currentTrace.set(simulator.setupBeforeSimulation(trace));
        currentTrace.addListener(listener);

        this.task = new TimerTask() {
            @Override
            public void run() {
                simulator.simulate();
            }
        };
        timer.scheduleAtFixedRate(task, interval, interval);
    }

    public void setSimulator(IRealTimeSimulator simulator) {
        this.simulator = simulator;
    }

    public void stop() {
        if(timer != null) {
            timer.cancel();
            timer = null;
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
        task.cancel();
        currentTrace.removeListener(listener);
        executingOperationProperty.set(false);
    }

}
