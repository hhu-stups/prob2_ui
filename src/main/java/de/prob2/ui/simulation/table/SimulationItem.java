package de.prob2.ui.simulation.table;

import de.prob2.ui.simulation.SimulationCheckingConfiguration;
import de.prob2.ui.simulation.SimulationType;
import de.prob2.ui.verifications.Checked;

public class SimulationItem {

    private Checked checked;

    private String configuration;

    private String description;

    private SimulationCheckingConfiguration simulationCheckingConfiguration;

    public SimulationItem(SimulationCheckingConfiguration simulationCheckingConfiguration, String description) {
        this.simulationCheckingConfiguration = simulationCheckingConfiguration;
        this.description = description;
        updateItem();
    }

    private void updateItem() {
        this.checked = Checked.NOT_CHECKED;
        this.configuration = simulationCheckingConfiguration.getConfiguration();
    }


    public SimulationType getType() {
        return this.simulationCheckingConfiguration.getType();
    }

    public SimulationCheckingConfiguration getSimulationConfiguration() {
        return simulationCheckingConfiguration;
    }

    public String getConfiguration() {
        return configuration;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof SimulationItem)) {
            return false;
        }
        SimulationItem otherItem = (SimulationItem) obj;
        return this.configuration.equals(otherItem.getConfiguration()) && this.description.equals(otherItem.getDescription());
    }

    public void reset() {
        // TODO
    }
}
