package de.prob2.ui.project;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.SimulatorStage;
import de.prob2.ui.visb.VisBStage;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Singleton
public class DefaultPathHandler {

    public enum DefaultKind {
        VISB, SIMB
    }

    private final StageManager stageManager;

    private final CurrentProject currentProject;

    private final ResourceBundle bundle;

    private SimulatorStage simulatorStage;

    private VisBStage visBStage;

    @Inject
    public DefaultPathHandler(final StageManager stageManager, final CurrentProject currentProject, final ResourceBundle bundle) {
        this.stageManager = stageManager;
        this.currentProject = currentProject;
        this.bundle = bundle;
    }

    public void manageDefault(DefaultKind kind) {
        Machine machine = currentProject.getCurrentMachine();
        List<ButtonType> buttons = new ArrayList<>();

        ButtonType loadButton = null;
        ButtonType setButton = null;
        ButtonType resetButton = null;
        ButtonType buttonTypeCancel = new ButtonType(bundle.getString("common.buttons.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ObjectProperty<Path> pathFromMachine = null;
        ObjectProperty<Path> path = null;
        String headerKey = null;
        String contentKey = null;
        Alert alert;
        Stage stage = null;


        switch (kind) {
            case VISB:
                loadButton = new ButtonType(bundle.getString("visb.defaultVisualisation.load"));
                setButton = new ButtonType(bundle.getString("visb.defaultVisualisation.set"));
                resetButton = new ButtonType(bundle.getString("visb.defaultVisualisation.reset"));
                pathFromMachine = machine.visBVisualizationProperty();
                path = visBStage.getVisBPath();
                headerKey = "visb.defaultVisualisation.header";
                if(pathFromMachine.isNotNull().get()) {
                    contentKey = "visb.defaultVisualisation.text";
                } else {
                    contentKey = "visb.defaultVisualisation.header";
                }
                stage = visBStage;
                break;
            case SIMB:
                loadButton = new ButtonType(bundle.getString("simulation.defaultSimulation.load"));
                setButton = new ButtonType(bundle.getString("simulation.defaultSimulation.set"));
                resetButton = new ButtonType(bundle.getString("simulation.defaultSimulation.reset"));
                pathFromMachine = machine.simulationProperty();
                path = simulatorStage.getConfigurationPath();
                headerKey = "simulation.defaultSimulation.header";
                if(pathFromMachine.isNotNull().get()) {
                    contentKey = "simulation.defaultSimulation.text";
                } else {
                    contentKey = "simulation.noDefaultSimulation.text";
                }
                stage = simulatorStage;
                break;
            default:
                break;
        }


        if(pathFromMachine.isNotNull().get()) {
            boolean notEqualsMachinePath = !currentProject.getLocation().relativize(path.get()).equals(pathFromMachine.get());
            if(path.get() != null && notEqualsMachinePath) {
                buttons.add(loadButton);
                buttons.add(setButton);
            }
            buttons.add(resetButton);
            buttons.add(buttonTypeCancel);
            alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION, buttons, headerKey, contentKey, pathFromMachine.get());
        } else {
            if(path.get() != null) {
                buttons.add(setButton);
            }
            buttons.add(buttonTypeCancel);
            alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION, buttons, headerKey, contentKey);
        }

        alert.initOwner(stage);

        Optional<ButtonType> result = alert.showAndWait();
        if(result.isPresent()) {
            if (result.get() == loadButton) {
                loadDefault(kind);
            } else if (result.get() == setButton) {
                setDefault(kind);
            } else if (result.get() == resetButton) {
                resetDefault(kind);
            } else {
                alert.close();
            }
        }
    }

    private void loadDefault(DefaultKind kind) {
        Machine currentMachine = currentProject.getCurrentMachine();
        switch (kind) {
            case VISB:
                visBStage.loadVisBFileFromMachine(currentMachine);
                break;
            case SIMB:
                simulatorStage.loadSimulationFromMachine(currentMachine);
                break;
            default:
                break;
        }
    }

    private void setDefault(DefaultKind kind) {
        Machine currentMachine = currentProject.getCurrentMachine();
        switch (kind) {
            case VISB:
                currentMachine.setVisBVisualisation(currentProject.getLocation().relativize(visBStage.getVisBPath().get()));
                break;
            case SIMB:
                currentMachine.setSimulation(currentProject.getLocation().relativize(simulatorStage.getConfigurationPath().get()));
                break;
            default:
                break;
        }
    }

    private void resetDefault(DefaultKind kind) {
        Machine currentMachine = currentProject.getCurrentMachine();
        switch (kind) {
            case VISB:
                currentMachine.setVisBVisualisation(null);
                break;
            case SIMB:
                currentMachine.setSimulation(null);
                break;
            default:
                break;
        }
    }

    public void setSimulatorStage(SimulatorStage simulatorStage) {
        this.simulatorStage = simulatorStage;
    }

    public void setVisBStage(VisBStage visBStage) {
        this.visBStage = visBStage;
    }
}
