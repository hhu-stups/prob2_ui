package de.prob2.ui.vomanager;



import com.google.inject.Injector;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.TraceViewHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    private final CurrentTrace currentTrace;

    private final Injector injector;

    private final VOTaskCreator taskCreator;

    private final VOChecker voChecker;

    private final ObjectProperty<EditType> editModeProperty;

    @Inject
    public VOManagerStage(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace, final Injector injector,
                          final VOTaskCreator taskCreator, final VOChecker voChecker) {
        super();
        this.currentProject = currentProject;
        this.currentTrace = currentTrace;
        this.injector = injector;
        this.taskCreator = taskCreator;
        this.voChecker = voChecker;
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
            tvRequirements.itemsProperty().unbind();
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

        cbRequirementChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
            cbTaskChoice.getItems().clear();
            if(to != null) {
                List<ValidationTask> tasks = VOTemplateGenerator.generate(to);
                cbTaskChoice.getItems().addAll(tasks);
            }
        });

        tvRequirements.setRowFactory(table -> {
            final TableRow<Requirement> row = new TableRow<>();

            Menu linkItem = new Menu("Link to Validation Obligation");

            row.itemProperty().addListener((observable, from, to) -> {
                final InvalidationListener linkingListener = o -> showPossibleLinkings(linkItem, to);
                updateLinkingListener(linkItem, from, to, linkingListener);
            });

            MenuItem checkItem = new MenuItem("Check Requirement");
            checkItem.setOnAction(e -> {
                Requirement requirement = row.getItem();
                requirement.getValidationObligations().forEach(voChecker::check);
            });

            MenuItem removeItem = new MenuItem("Remove Requirement");
            removeItem.setOnAction(e -> removeRequirement());

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(new ContextMenu(linkItem, checkItem, removeItem)));
            return row;
        });

        tvRequirements.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
            tvValidationObligations.itemsProperty().unbind();
            if(to != null) {
                editModeProperty.set(EditType.EDIT);
                showRequirement(to);
                tvValidationObligations.itemsProperty().bind(to.validationObligationsProperty());
            } else {
                editModeProperty.set(EditType.NONE);
            }
        });

        tvRequirements.setOnMouseClicked(e-> {
            Requirement requirement = tvRequirements.getSelectionModel().getSelectedItem();
            if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && requirement != null && currentTrace.get() != null) {
                requirement.getValidationObligations().forEach(voChecker::check);
            }
        });


        tvValidationObligations.setRowFactory(table -> {
            final TableRow<ValidationObligation> row = new TableRow<>();

            MenuItem checkItem = new MenuItem("Check VO");
            checkItem.setOnAction(e -> voChecker.check(row.getItem()));

            MenuItem removeItem = new MenuItem("Remove VO");
            removeItem.setOnAction(e -> {
                Requirement requirement = tvRequirements.getSelectionModel().getSelectedItem();
                requirement.removeValidationObligation(row.getItem());
            });

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(new ContextMenu(checkItem, removeItem)));
            return row;
        });

        tvValidationObligations.setOnMouseClicked(e-> {
            ValidationObligation item = tvValidationObligations.getSelectionModel().getSelectedItem();
            if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item != null && currentTrace.get() != null) {
                voChecker.check(item);
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
                validationObligation.setExecutable(executable);
                validationObligation.checkedProperty().addListener((observable, from, to) -> requirement.updateChecked());
            }
            requirement.updateChecked();
        }
    }

    private IExecutableItem lookupExecutable(Machine machine, ValidationTask task, Object executableItem) {
        switch (task) {
            case MODEL_CHECKING:
                return machine.getModelcheckingItems().stream()
                        .filter(item -> item.getOptions().equals(((ModelCheckingItem) executableItem).getOptions()))
                        .findAny()
                        .orElse(null);
            case LTL_MODEL_CHECKING:
                return machine.getLTLFormulas().stream()
                        .filter(item -> item.settingsEqual((LTLFormulaItem) executableItem))
                        .findAny()
                        .orElse(null);
            case SYMBOLIC_MODEL_CHECKING:
                // TODO: Implement
                break;
            case TRACE_REPLAY:
                return injector.getInstance(TraceViewHandler.class).getTraces().stream()
                        .filter(item -> item.getLocation().toString().equals(executableItem))
                        .findAny()
                        .orElse(null);
            default:
                throw new RuntimeException("Validation task is not valid: " + task);
        }
        return null;
    }

    private ValidationObligation linkValidationObligation(Object item) {
        ValidationObligation validationObligation;
        if(item instanceof ModelCheckingItem) {
            validationObligation = new ValidationObligation(ValidationTask.MODEL_CHECKING, ((ModelCheckingItem) item).getOptions().toString(), item);
        } else if(item instanceof LTLFormulaItem) {
            validationObligation = new ValidationObligation(ValidationTask.LTL_MODEL_CHECKING, ((LTLFormulaItem) item).getCode(), item);
        } else if(item instanceof SymbolicCheckingFormulaItem) {
            // TODO: Implement
            return null;
        } else if(item instanceof ReplayTrace) {
            validationObligation = new ValidationObligation(ValidationTask.TRACE_REPLAY, ((ReplayTrace) item).getName(), ((ReplayTrace) item).getLocation().toString());
        } else {
            throw new RuntimeException("Validation item is not valid. Class is: " + item.getClass());
        }
        updateExecutableInVO(validationObligation);
        return validationObligation;
    }

    private void updateExecutableInVO(ValidationObligation validationObligation) {
        switch (validationObligation.getTask()) {
            case MODEL_CHECKING:
            case LTL_MODEL_CHECKING:
                validationObligation.setExecutable((IExecutableItem) validationObligation.getItem());
                break;
            case SYMBOLIC_MODEL_CHECKING:
                // TODO: Implement
                break;
            case TRACE_REPLAY:
                validationObligation.setExecutable(injector.getInstance(TraceViewHandler.class).getTraces().stream()
                        .filter(item -> item.getLocation().toString().equals(validationObligation.getItem()))
                        .findAny()
                        .orElse(null));
                break;
            default:
                throw new RuntimeException("Validation task is invalid: " + validationObligation.getTask());
        }
    }

    private void showPossibleLinkings(Menu linkItem, Requirement requirement) {
        linkItem.getItems().clear();
        List<Observable> dependentProperties = dependentPropertiesFromRequirement(requirement);
        for(Observable observable : dependentProperties) {
            if(observable instanceof SetProperty) {
                ((SetProperty<?>) observable).forEach(obj -> createLinkingItem(linkItem, requirement, obj));
            } else if(observable instanceof ListProperty) {
                ((ListProperty<?>) observable).forEach(obj -> createLinkingItem(linkItem, requirement, obj));
            }
        }
    }

    private void createLinkingItem(Menu linkItem, Requirement requirement, Object voExecutable) {
        MenuItem voItem = new MenuItem(generateVOName(voExecutable));
        voItem.setOnAction(e -> {
            ValidationObligation validationObligation = linkValidationObligation(voExecutable);
            requirement.addValidationObligation(validationObligation);
            validationObligation.checkedProperty().addListener((o, from, to) -> requirement.updateChecked());
            voChecker.check(validationObligation);
        });
        linkItem.getItems().add(voItem);
    }

    private String generateVOName(Object item) {
        if(item instanceof ModelCheckingItem) {
            return String.format("MC(%s)", ((ModelCheckingItem) item).getOptions().getPrologOptions().stream().map(Enum::toString).collect(Collectors.joining(", ")));
        } else if(item instanceof LTLFormulaItem) {
            return String.format("LTL(%s)", ((LTLFormulaItem) item).getCode());
        } else if(item instanceof SymbolicCheckingFormulaItem) {
            // TODO: Implement
            return null;
        } else if(item instanceof ReplayTrace) {
            return String.format("TR(%s)", ((ReplayTrace) item).getName());
        } else {
            throw new RuntimeException("Validation item is not valid. Class is: " + item.getClass());
        }
    }

    private void updateLinkingListener(Menu linkItem, Requirement from, Requirement to, InvalidationListener linkingListener) {
        if (from != null) {
            List<Observable> dependentProperties = dependentPropertiesFromRequirement(from);
            for (Observable observable : dependentProperties) {
                observable.removeListener(linkingListener);
            }
        }

        if(to != null) {
            List<Observable> dependentProperties = dependentPropertiesFromRequirement(to);
            Observable[] dependentPropertiesAsArray = dependentProperties.toArray(new Observable[0]);
            BooleanBinding emptyProperty = Bindings.createBooleanBinding(() -> {
                boolean result = true;
                for(Observable observable : dependentPropertiesAsArray) {
                    if(observable instanceof SetProperty) {
                        result = result && ((SetProperty<?>) observable).emptyProperty().get();
                    } else if(observable instanceof ListProperty) {
                        result = result && ((ListProperty<?>) observable).emptyProperty().get();
                    }
                }
                return result;
            }, dependentPropertiesAsArray);

            for (Observable observable : dependentProperties) {
                linkItem.disableProperty().bind(emptyProperty);
                observable.addListener(linkingListener);
            }

            linkingListener.invalidated(null);
        }
    }

    private List<Observable> dependentPropertiesFromRequirement(Requirement requirement) {
        RequirementType requirementType = requirement.getType();
        List<Observable> lists = new ArrayList<>();
        Machine machine = currentProject.getCurrentMachine();
        switch (requirementType) {
            case INVARIANT:
                lists.add(machine.modelcheckingItemsProperty());
                lists.add(machine.ltlFormulasProperty());
                lists.add(machine.symbolicCheckingFormulasProperty());
                break;
            case SAFETY:
                lists.add(machine.ltlFormulasProperty());
                break;
            case LIVENESS:
                lists.add(machine.ltlFormulasProperty());
                break;
            case USE_CASE:
                lists.add(injector.getInstance(TraceViewHandler.class).getTraces());
                break;
            default:
                throw new RuntimeException("Requirement type is invalid: " + requirementType);
        }
        return lists;
    }

}
