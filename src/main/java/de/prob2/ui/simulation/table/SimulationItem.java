package de.prob2.ui.simulation.table;

import de.prob2.ui.simulation.SimulationCheckingConfiguration;
import de.prob2.ui.simulation.SimulationType;
import de.prob2.ui.verifications.Checked;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Objects;

public class SimulationItem {

    private String configuration;

    private String description;

    private SimulationCheckingConfiguration simulationCheckingConfiguration;

    private final transient ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

    public SimulationItem(SimulationCheckingConfiguration simulationCheckingConfiguration, String description) {
        this.simulationCheckingConfiguration = simulationCheckingConfiguration;
        this.description = description;
        updateItem();
    }

    private void updateItem() {
        this.configuration = simulationCheckingConfiguration.getConfiguration();
    }

    public void setChecked(Checked checked) {
        this.checked.set(checked);
    }

    public ObjectProperty<Checked> checkedProperty() {
        return checked;
    }

    public Checked getChecked() {
        return checked.get();
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
    public int hashCode() {
        return Objects.hash(configuration, description);
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
