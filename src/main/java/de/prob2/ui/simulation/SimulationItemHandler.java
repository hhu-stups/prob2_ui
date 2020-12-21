package de.prob2.ui.simulation;

import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import de.prob2.ui.simulation.simulators.check.SimulationModelChecker;
import de.prob2.ui.simulation.simulators.check.SimulationTimeChecker;
import de.prob2.ui.simulation.simulators.check.SimulationTraceChecker;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.Checked;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Singleton
public class SimulationItemHandler {

    private final CurrentTrace currentTrace;

    private Path path;

    @Inject
    private SimulationItemHandler(final CurrentTrace currentTrace) {
        this.currentTrace = currentTrace;
    }

    public List<SimulationItem> getItems(final Machine machine) {
        return machine.getSimulations();
    }

    public Optional<SimulationItem> addItem(final Machine machine, final SimulationItem item) {
        final List<SimulationItem> items = this.getItems(machine);
        final Optional<SimulationItem> existingItem = items.stream().filter(item::equals).findAny();
        if(!existingItem.isPresent()) {
            items.add(item);
        }
        return existingItem;
    }

    private void handleTiming(SimulationItem item, boolean checkAll) {
        Trace trace = currentTrace.get();
        SimulationTimeChecker timeChecker = new SimulationTimeChecker(trace, (int) item.getSimulationConfiguration().getField("TIME"));
        timeChecker.initSimulator(path.toFile());
        timeChecker.run();
        SimulationTimeChecker.TimeCheckResult result = timeChecker.check();
        switch (result) {
            case SUCCESS:
                item.setChecked(Checked.SUCCESS);
                break;
            case FAIL:
                item.setChecked(Checked.FAIL);
                break;
            case NOT_FINISHED:
                item.setChecked(Checked.NOT_CHECKED);
                break;
            default:
                break;
        }
    }

    private void handleModelChecking(SimulationItem item, boolean checkAll) {
        SimulationModelChecker modelChecker = new SimulationModelChecker(currentTrace.getStateSpace());
        modelChecker.initSimulator(path.toFile());
        modelChecker.check();
        SimulationModelChecker.ModelCheckResult result = modelChecker.getResult();
        switch (result) {
            case SUCCESS:
                item.setChecked(Checked.SUCCESS);
                break;
            case FAIL:
                item.setChecked(Checked.FAIL);
                break;
            case NOT_FINISHED:
                item.setChecked(Checked.NOT_CHECKED);
                break;
            default:
                break;
        }
    }

    private void handleHypothesisTest(SimulationItem item, boolean checkAll) {
        Trace trace = currentTrace.get();
        SimulationHypothesisChecker hypothesisChecker = new SimulationHypothesisChecker(trace, (int) item.getSimulationConfiguration().getField("EXECUTIONS"), (int) item.getSimulationConfiguration().getField("STEPS_PER_EXECUTION"));
        hypothesisChecker.initSimulator(path.toFile());
        hypothesisChecker.run();
        SimulationHypothesisChecker.HypothesisCheckResult result = hypothesisChecker.check();
        switch (result) {
            case SUCCESS:
                item.setChecked(Checked.SUCCESS);
                break;
            case FAIL:
                item.setChecked(Checked.FAIL);
                break;
            case NOT_FINISHED:
                item.setChecked(Checked.NOT_CHECKED);
                break;
            default:
                break;
        }
    }

    private void handleTraceReplay(SimulationItem item, boolean checkAll) {
        Trace trace = currentTrace.get();
        ReplayTrace replayTrace = (ReplayTrace) item.getSimulationConfiguration().getField("TRACE");
        SimulationTraceChecker traceChecker = new SimulationTraceChecker(trace, replayTrace);
        traceChecker.initSimulator(path.toFile());
        traceChecker.run();
        SimulationTraceChecker.TraceCheckResult result = traceChecker.check();
        switch (result) {
            case SUCCESS:
                item.setChecked(Checked.SUCCESS);
                break;
            case FAIL:
                item.setChecked(Checked.FAIL);
                break;
            case NOT_FINISHED:
                item.setChecked(Checked.NOT_CHECKED);
                break;
            default:
                break;
        }
    }

    public void handleItem(SimulationItem item, boolean checkAll) {
        /*if(!item.selected()) {
            return;
        }*/
        // TODO
        SimulationType type = item.getType();
        switch(type) {
            case TIMING:
                handleTiming(item, checkAll);
                break;
            case MODEL_CHECKING:
                handleModelChecking(item, checkAll);
                break;
            case PROBABILISTIC_MODEL_CHECKING:
                break;
            case MONTE_CARLO_SIMULATION:
                handleHypothesisTest(item, checkAll);
                break;
            case TRACE_REPLAY:
                handleTraceReplay(item, checkAll);
                break;
            default:
                break;
        }
    }

    public void handleMachine(Machine machine) {
        //machine.getSymbolicAnimationFormulas().forEach(item -> handleItem(item, true));
        // TODO
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
