package de.prob2.ui.simulation;

import com.google.inject.Singleton;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.table.SimulationItem;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@Singleton
public class SimulationItemHandler {

    private final CurrentTrace currentTrace;

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

    public void handleTiming(SimulationItem item, boolean checkAll) {
        //item.getSimulationConfiguration().

        // TODO: check Timing simulation
    }

    public void handleItem(SimulationItem item, boolean checkAll) {
        /*if(!item.selected()) {
            return;
        }*/
        // TODO
        SimulationType type = item.getType();
        switch(type) {
            case TIMING:
                break;
            case MODEL_CHECKING:
                break;
            case PROBABILISTIC_MODEL_CHECKING:
                break;
            case HYPOTHESIS_TEST:
                break;
            case TRACE_REPLAY:
                break;
            default:
                break;
        }
    }

    public void handleMachine(Machine machine) {
        //machine.getSymbolicAnimationFormulas().forEach(item -> handleItem(item, true));
        // TODO
    }

}
