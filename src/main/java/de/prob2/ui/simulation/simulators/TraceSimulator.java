package de.prob2.ui.simulation.simulators;

import com.google.inject.Singleton;
import de.prob.animator.command.ExecuteOperationException;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;

@Singleton
public class TraceSimulator extends AbstractTraceSimulator implements IRealTimeSimulator {

    private final Scheduler scheduler;

    private final CurrentTrace currentTrace;

    public TraceSimulator(Trace trace, ReplayTrace replayTrace, final Scheduler scheduler, final CurrentTrace currentTrace) {
        super(trace, replayTrace);
        this.scheduler = scheduler;
        this.currentTrace = currentTrace;
    }

    public TraceSimulator(Trace trace, PersistentTrace persistentTrace, final Scheduler scheduler, final CurrentTrace currentTrace) {
        super(trace, persistentTrace);
        this.scheduler = scheduler;
        this.currentTrace = currentTrace;
    }

    @Override
    public void run() {
        currentTrace.set(new Trace(currentTrace.getStateSpace()));
        scheduler.run();
    }

    @Override
    public void simulate() {
        scheduler.startSimulationStep();
        try {
            if(!finished) {
                // Read trace and pass it through chooseOperation to avoid race condition
                Trace trace = currentTrace.get();
                Trace newTrace = simulationStep(trace);
                currentTrace.set(newTrace);
                if(counter >= persistentTrace.getTransitionList().size()) {
                    finishSimulation();
                }
            }
        } catch (ExecuteOperationException e) {
            System.out.println("TRACE REPLAY IN SIMULATION ERROR");
        }
        scheduler.endSimulationStep();
    }

    @Override
    public BooleanProperty runningProperty() {
        return scheduler.runningPropertyProperty();
    }

    @Override
    public boolean isRunning() {
        return scheduler.isRunning();
    }

    public boolean isFinished() {
        return finished;
    }

    @FXML
    public void stop() {
        scheduler.stop();
        reset();
    }

    private void reset() {
        this.time.set(0);
        this.counter = 0;
    }

    @Override
    protected void finishSimulation() {
        super.finishSimulation();
        scheduler.stop();
        reset();
    }

}
