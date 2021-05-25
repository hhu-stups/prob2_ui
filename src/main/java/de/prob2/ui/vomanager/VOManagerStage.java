package de.prob2.ui.vomanager;


import de.prob.check.ModelCheckingOptions;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
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
import java.util.Collections;
import java.util.List;

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
    private TableView<ValidationObligation> tvValidationObligations;

    @FXML
    private TableColumn<ValidationObligation, Checked> voStatusColumn;

    @FXML
    private TableColumn<ValidationObligation, String> voNameColumn;

    @FXML
    private TableColumn<ValidationObligation, String> voConfigurationColumn;

    @FXML
    private Button btAddOrCancelRequirement;

    @FXML
    private TextField tfName;

    @FXML
    private TextArea taRequirement;

    @FXML
    private ChoiceBox<RequirementType> cbRequirementChoice;

    @FXML
    private ChoiceBox<ValidationTask> cbTaskChoice;

    @FXML
    private VBox requirementEditingBox;

    @FXML
    private VBox taskBox;

    @FXML
    private Button applyButton;

    private final CurrentProject currentProject;

    private final VOTaskCreator taskCreator;

    private final ObjectProperty<EditType> editModeProperty;

    @Inject
    public VOManagerStage(final StageManager stageManager, final CurrentProject currentProject, final VOTaskCreator taskCreator) {
        super();
        this.currentProject = currentProject;
        this.taskCreator = taskCreator;
        this.editModeProperty = new SimpleObjectProperty<>(EditType.NONE);
        stageManager.loadFXML(this, "vo_manager_view.fxml");
    }

    @FXML
    public void initialize() {
        requirementStatusColumn.setCellFactory(col -> new CheckedCell<>());
        requirementStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));

        requirementNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("shortTypeName"));
        specificationColumn.setCellValueFactory(new PropertyValueFactory<>("text"));

        voStatusColumn.setCellFactory(col -> new CheckedCell<>());
        voStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));

        voNameColumn.setCellValueFactory(new PropertyValueFactory<>("task"));
        voConfigurationColumn.setCellValueFactory(new PropertyValueFactory<>("configuration"));

        final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
            if(to != null) {
                synchronizeMachine(to);
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

        // TODO: Implement possibility, just to add requirement without choosing a validation task

        taskBox.visibleProperty().bind(cbRequirementChoice.getSelectionModel().selectedItemProperty().isNotNull());
        applyButton.visibleProperty().bind(cbTaskChoice.getSelectionModel().selectedItemProperty().isNotNull());

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
            tvValidationObligations.itemsProperty().unbind();
            if(to != null) {
                editModeProperty.set(EditType.EDIT);
                showRequirement(to);
                List<ValidationTask> tasks = VOTemplateGenerator.generate(to);
                cbTaskChoice.getItems().clear();
                cbTaskChoice.getItems().addAll(tasks);
                tvValidationObligations.itemsProperty().bind(to.validationObligationsProperty());
            } else {
                editModeProperty.set(EditType.NONE);
            }
        });
    }

    @FXML
    public void addOrCancelRequirement() {
        if(editModeProperty.get() == EditType.NONE) {
            cbRequirementChoice.getSelectionModel().clearSelection();
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
            requirement = new Requirement(tfName.getText(), cbRequirementChoice.getValue(), taRequirement.getText(), Collections.emptyList());
            currentProject.getCurrentMachine().getRequirements().add(requirement);
        } else if(editType == EditType.EDIT) {
            requirement = tvRequirements.getSelectionModel().getSelectedItem();
            requirement.setName(tfName.getText());
            requirement.setType(cbRequirementChoice.getValue());
            requirement.setText(taRequirement.getText());
        }
        assert requirement != null;

        ValidationTask task = cbTaskChoice.getSelectionModel().getSelectedItem();
        ValidationObligation validationObligation = taskCreator.openTaskWindow(requirement, task);
        if(validationObligation != null) {
            requirement.addValidationObligation(validationObligation);
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
        cbRequirementChoice.getSelectionModel().select(requirement.getType());
        taRequirement.setText(requirement.getText());
    }

    private void synchronizeMachine(Machine machine) {
        for(Requirement requirement : machine.getRequirements()) {
            for(ValidationObligation validationObligation : requirement.validationObligationsProperty()) {
                IExecutableItem executable = lookupExecutable(machine, validationObligation.getTask(), validationObligation.getItem());
                validationObligation.setItem(executable);
                validationObligation.checkedProperty().addListener((observable, from, to) -> requirement.updateChecked());
            }
            requirement.updateChecked();
        }
    }

    private IExecutableItem lookupExecutable(Machine machine, ValidationTask task, IExecutableItem executable) {
        switch (task) {
            case MODEL_CHECKING:
                return machine.getModelcheckingItems().stream()
                        .filter(item -> item.getOptions().equals(((ModelCheckingItem) executable).getOptions()))
                        .findAny()
                        .orElse(null);
            case LTL_MODEL_CHECKING:
                return machine.getLTLFormulas().stream()
                        .filter(item -> item.settingsEqual((LTLFormulaItem) executable))
                        .findAny()
                        .orElse(null);
            case SYMBOLIC_MODEL_CHECKING:
                // TODO: Implement
                break;
            case TRACE_REPLAY:
                // TODO: Implement
                break;
            default:
                throw new RuntimeException("Validation task is not valid: " + task);
        }
        return executable;
    }

}
