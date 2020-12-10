package de.prob2.ui.simulation.table;

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







}
