package de.prob2.ui.vomanager;


import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;

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
    private Button btAddOrCancelRequirement;

    @FXML
    private TextArea taRequirement;

    @FXML
    private ChoiceBox<Requirement.RequirementType> cbChoice;

    @FXML
    private VBox requirementEditingBox;

    private final CurrentProject currentProject;

    private final BooleanProperty editModeProperty;

    @Inject
    public VOManagerStage(final StageManager stageManager, final CurrentProject currentProject) {
        super();
        this.currentProject = currentProject;
        this.editModeProperty = new SimpleBooleanProperty(false);
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
            editModeProperty.set(false);
        };


        currentProject.currentMachineProperty().addListener(machineChangeListener);
        machineChangeListener.changed(null, null, currentProject.getCurrentMachine());

        requirementEditingBox.visibleProperty().bind(editModeProperty);

        editModeProperty.addListener((observable, from, to) -> {
            if(to) {
                btAddOrCancelRequirement.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.TIMES_CIRCLE));
                btAddOrCancelRequirement.setTooltip(new Tooltip());
            } else {
                btAddOrCancelRequirement.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.PLUS_CIRCLE));
                btAddOrCancelRequirement.setTooltip(new Tooltip());
            }
        });

        tvRequirements.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
            editModeProperty.set(to != null);
            showRequirement(to);
        });
    }

    @FXML
    public void addOrCancelRequirement() {
        if(!editModeProperty.get()) {
            cbChoice.getSelectionModel().clearSelection();
            taRequirement.clear();
        }
        editModeProperty.set(!editModeProperty.get());
    }

    @FXML
    public void applyRequirement() {
        currentProject.getCurrentMachine().getRequirements().add(new Requirement(cbChoice.getValue(), taRequirement.getText()));
    }

    private void showRequirement(Requirement requirement) {
        if(requirement == null) {
            return;
        }
        cbChoice.getSelectionModel().select(requirement.getType());
        taRequirement.setText(requirement.getText());
    }

}
