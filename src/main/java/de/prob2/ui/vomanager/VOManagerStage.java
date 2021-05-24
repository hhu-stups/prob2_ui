package de.prob2.ui.vomanager;


import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;

import javax.inject.Inject;

public class VOManagerStage extends Stage {

    private enum EditType {
        NONE, ADD, EDIT;
    }

    @FXML
    private TableView<Requirement> tvRequirements;

    @FXML
    private TableColumn<Requirement, Checked> requirementStatusColumn;

    @FXML
    private TableColumn<Requirement, String> requirementNameColumn;

    @FXML
    private TableColumn<Requirement, String> typeColumn;

    @FXML
    private TableColumn<Requirement, String> specificationColumn;

    @FXML
    private Button btAddOrCancelRequirement;

    @FXML
    private TextField tfName;

    @FXML
    private TextArea taRequirement;

    @FXML
    private ChoiceBox<Requirement.RequirementType> cbChoice;

    @FXML
    private VBox requirementEditingBox;

    private final CurrentProject currentProject;

    private final ObjectProperty<EditType> editModeProperty;

    @Inject
    public VOManagerStage(final StageManager stageManager, final CurrentProject currentProject) {
        super();
        this.currentProject = currentProject;
        this.editModeProperty = new SimpleObjectProperty<>(EditType.NONE);
        stageManager.loadFXML(this, "vo_manager_view.fxml");
    }

    @FXML
    public void initialize() {
        requirementStatusColumn.setCellFactory(col -> new CheckedCell<>());
        requirementStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));

        requirementNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        specificationColumn.setCellValueFactory(new PropertyValueFactory<>("text"));

        final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
            if(to != null) {
                tvRequirements.itemsProperty().bind(to.requirementsProperty());
            } else {
                tvRequirements.setItems(FXCollections.observableArrayList());
            }
            editModeProperty.set(EditType.NONE);
        };


        currentProject.currentMachineProperty().addListener(machineChangeListener);
        machineChangeListener.changed(null, null, currentProject.getCurrentMachine());

        requirementEditingBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> editModeProperty.get() != EditType.NONE, editModeProperty));

        editModeProperty.addListener((observable, from, to) -> {
            if(to != EditType.NONE) {
                btAddOrCancelRequirement.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.TIMES_CIRCLE));
                btAddOrCancelRequirement.setTooltip(new Tooltip());
            } else {
                btAddOrCancelRequirement.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.PLUS_CIRCLE));
                btAddOrCancelRequirement.setTooltip(new Tooltip());
            }
        });

        tvRequirements.setRowFactory(table -> {
            final TableRow<Requirement> row = new TableRow<>();

            MenuItem checkItem = new MenuItem("Check Requirement");
            checkItem.setOnAction(e -> {
                // TODO: Implement
            });

            MenuItem removeItem = new MenuItem("Remove Requirement");
            removeItem.setOnAction(e -> removeRequirement());

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(new ContextMenu(checkItem, removeItem)));
            return row;
        });

        tvRequirements.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
            editModeProperty.set(to != null ? EditType.EDIT : EditType.NONE);
            showRequirement(to);
        });
    }

    @FXML
    public void addOrCancelRequirement() {
        if(editModeProperty.get() == EditType.NONE) {
            cbChoice.getSelectionModel().clearSelection();
            taRequirement.clear();
            tfName.clear();
            editModeProperty.set(EditType.ADD);
        } else {
            editModeProperty.set(EditType.NONE);
        }
        tvRequirements.getSelectionModel().clearSelection();
    }

    @FXML
    public void applyRequirement() {
        EditType editType = editModeProperty.get();
        Requirement requirement = null;
        if(editType == EditType.ADD) {
            requirement = new Requirement(tfName.getText(), cbChoice.getValue(), taRequirement.getText());
            currentProject.getCurrentMachine().getRequirements().add(requirement);
        } else if(editType == EditType.EDIT) {
            requirement = tvRequirements.getSelectionModel().getSelectedItem();
            requirement.setName(tfName.getText());
            requirement.setType(cbChoice.getValue());
            requirement.setText(taRequirement.getText());
        }
        // TODO: Replace refresh?
        editModeProperty.set(EditType.NONE);
        tvRequirements.getSelectionModel().clearSelection();
        tvRequirements.refresh();
    }

    private void removeRequirement() {
        Requirement requirement = tvRequirements.getSelectionModel().getSelectedItem();
        currentProject.getCurrentMachine().getRequirements().remove(requirement);
        tvRequirements.refresh();
    }

    private void showRequirement(Requirement requirement) {
        if(requirement == null) {
            return;
        }
        tfName.setText(requirement.getName());
        cbChoice.getSelectionModel().select(requirement.getType());
        taRequirement.setText(requirement.getText());
    }

}
