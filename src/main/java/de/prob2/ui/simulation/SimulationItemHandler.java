package de.prob2.ui.simulation;

import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import de.prob2.ui.simulation.simulators.check.SimulationTimeChecker;
import de.prob2.ui.simulation.simulators.check.SimulationTraceChecker;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.Checked;
import javafx.application.Platform;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public void removeItem(final Machine machine, SimulationItem item) {
        final List<SimulationItem> items = this.getItems(machine);
        items.remove(item);
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

    private void handleHypothesisTest(SimulationItem item, boolean checkAll) {
        Trace trace = currentTrace.get();

        Map<String, Object> additionalInformation = new HashMap<>();

        int executions = (int) item.getSimulationConfiguration().getField("EXECUTIONS");
        if(item.getSimulationConfiguration().containsField("STEPS_PER_EXECUTION")) {
            additionalInformation.put("STEPS_PER_EXECUTION", item.getSimulationConfiguration().getField("STEPS_PER_EXECUTION"));
        } else if(item.getSimulationConfiguration().containsField("ENDING_PREDICATE")) {
            additionalInformation.put("ENDING_PREDICATE", item.getSimulationConfiguration().getField("ENDING_PREDICATE"));
        } else if(item.getSimulationConfiguration().containsField("ENDING_TIME")) {
            additionalInformation.put("ENDING_TIME", item.getSimulationConfiguration().getField("ENDING_TIME"));
        }
		SimulationCheckingType checkingType = (SimulationCheckingType) item.getSimulationConfiguration().getField("CHECKING_TYPE");
		SimulationHypothesisChecker.HypothesisCheckingType hypothesisCheckingType = (SimulationHypothesisChecker.HypothesisCheckingType) item.getSimulationConfiguration().getField("HYPOTHESIS_CHECKING_TYPE");
		double probability = (double) item.getSimulationConfiguration().getField("PROBABILITY");


		if(item.getSimulationConfiguration().containsField("TIME")) {
		    additionalInformation.put("TIME", item.getSimulationConfiguration().getField("TIME"));
        }

        SimulationHypothesisChecker hypothesisChecker = new SimulationHypothesisChecker(trace, executions, checkingType, hypothesisCheckingType, probability, additionalInformation);
        hypothesisChecker.initSimulator(path.toFile());
		Thread thread = new Thread(() -> {
			hypothesisChecker.run();
			SimulationHypothesisChecker.HypothesisCheckResult result = hypothesisChecker.getResult();
			Platform.runLater(() -> {
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
			});
		});
		thread.start();
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

    public void checkItem(SimulationItem item, boolean checkAll) {
        /*if(!item.selected()) {
            return;
        }*/
        // TODO
        SimulationType type = item.getType();
        switch(type) {
            case TIMING:
                handleTiming(item, checkAll);
                break;
            case HYPOTHESIS_TEST:
                handleHypothesisTest(item, checkAll);
                break;
            case ESTIMATION:
                // TODO
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
