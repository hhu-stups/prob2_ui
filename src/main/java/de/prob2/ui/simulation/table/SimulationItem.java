package de.prob2.ui.simulation.table;

import de.prob2.ui.simulation.SimulationCheckingConfiguration;
import de.prob2.ui.simulation.SimulationType;
import de.prob2.ui.verifications.Checked;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Objects;

public class SimulationItem {

    private SimulationType type;

    private String configuration;

    private SimulationCheckingConfiguration simulationCheckingConfiguration;

    private final transient ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

    public SimulationItem(SimulationCheckingConfiguration simulationCheckingConfiguration, String description) {
        this.simulationCheckingConfiguration = simulationCheckingConfiguration;
        updateItem();
    }

    private void updateItem() {
        this.type = simulationCheckingConfiguration.getType();
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

    public String getTypeAsName() {
        return type.getName();
    }

    public SimulationType getType() {
        return type;
    }

    public SimulationCheckingConfiguration getSimulationConfiguration() {
        return simulationCheckingConfiguration;
    }

    public String getConfiguration() {
        return configuration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, configuration);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof SimulationItem)) {
            return false;
        }
        SimulationItem otherItem = (SimulationItem) obj;
        return this.configuration.equals(otherItem.getConfiguration()) && this.type.equals(otherItem.getType());
    }

    public void reset() {
        // TODO
    }
}
