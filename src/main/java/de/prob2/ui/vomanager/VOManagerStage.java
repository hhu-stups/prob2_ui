package de.prob2.ui.vomanager;


import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import javax.inject.Inject;

public class VOManagerStage extends Stage {

    @FXML
    private TableView<Requirement> tvRequirements;

    @FXML
    private TableColumn<SimulationItem, Checked> requirementStatusColumn;

    @FXML
    private TableColumn<SimulationItem, String> typeColumn;

    @FXML
    private TableColumn<SimulationItem, String> specificationColumn;

    @FXML
    private TextArea taRequirement;

    @FXML
    private ChoiceBox<Requirement.RequirementType> cbChoice;

    private final CurrentProject currentProject;

    @Inject
    public VOManagerStage(final StageManager stageManager, final CurrentProject currentProject) {
        super();
        this.currentProject = currentProject;
        stageManager.loadFXML(this, "vo_manager_view.fxml");
    }

    @FXML
    public void initialize() {
        requirementStatusColumn.setCellFactory(col -> new CheckedCell<>());
        requirementStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        specificationColumn.setCellValueFactory(new PropertyValueFactory<>("text"));

        final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
            if(to != null) {
                tvRequirements.itemsProperty().bind(to.requirementsProperty());
            } else {
                tvRequirements.setItems(FXCollections.observableArrayList());
            }
        };


        currentProject.currentMachineProperty().addListener(machineChangeListener);
        machineChangeListener.changed(null, null, currentProject.getCurrentMachine());
    }

    @FXML
    public void addRequirement() {

    }

    @FXML
    public void applyRequirement() {
        currentProject.getCurrentMachine().getRequirements().add(new Requirement(cbChoice.getValue(), taRequirement.getText()));
    }

}
