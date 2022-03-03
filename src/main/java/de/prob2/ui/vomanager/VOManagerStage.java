package de.prob2.ui.vomanager;

import com.google.inject.Injector;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.IExecutableItem;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VOManagerStage extends Stage {

	private static final List<ValidationTaskType> tasks = Arrays.asList(ValidationTaskType.MODEL_CHECKING, ValidationTaskType.LTL_MODEL_CHECKING, ValidationTaskType.SYMBOLIC_MODEL_CHECKING,
			ValidationTaskType.TRACE_REPLAY, ValidationTaskType.SIMULATION);

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
	private ChoiceBox<ValidationTaskType> cbTaskChoice;

	@FXML
	private CheckBox cbLinkTask;

	@FXML
	private VBox requirementEditingBox;

	@FXML
	private VBox taskBox;

	@FXML
	private Button applyButton;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final VOManager voManager;

	private final VOTaskCreator taskCreator;

	private final VOChecker voChecker;

	private final ObjectProperty<EditType> editModeProperty;

	@Inject
	public VOManagerStage(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace, final Injector injector,
			final VOManager voManager, final VOTaskCreator taskCreator, final VOChecker voChecker) {
		super();
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.voManager = voManager;
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
				voManager.synchronizeMachine(to);
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
		taskBox.visibleProperty().bind(cbRequirementChoice.getSelectionModel().selectedItemProperty().isNotNull().and(cbLinkTask.selectedProperty()));
		applyButton.visibleProperty().bind(cbRequirementChoice.getSelectionModel().selectedItemProperty().isNotNull());

		cbRequirementChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			cbTaskChoice.getItems().clear();
			if(to != null) {
				cbTaskChoice.getItems().addAll(tasks);
			}
		});

		tvRequirements.setRowFactory(table -> {
			final TableRow<Requirement> row = new TableRow<>();

			Menu linkItem = new Menu("Link to Validation Obligation");

			row.itemProperty().addListener((observable, from, to) -> {
				final InvalidationListener linkingListener = o -> voManager.showPossibleLinkings(linkItem, to);
				voManager.updateLinkingListener(linkItem, from, to, linkingListener);
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
				tvValidationObligations.itemsProperty().set(FXCollections.observableArrayList());
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
		boolean requirementIsValid = requirementIsValid(tfName.getText(), taRequirement.getText());
		boolean linkTaskValid = !cbLinkTask.isSelected() || cbTaskChoice.getValue() != null; // If the user selects to link a task, then the selected value for the task must be valid
		if(requirementIsValid && linkTaskValid) {
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
			if (cbLinkTask.isSelected()) {
				ValidationTaskType taskType = cbTaskChoice.getSelectionModel().getSelectedItem();
				ValidationTask validationTask = taskCreator.openTaskWindow(this, requirement, taskType);
				if (validationTask != null) {
					requirement.addValidationObligation(new ValidationObligation(validationTask, VOManager.extractConfiguration((IExecutableItem) validationTask.getItem())));
				}
			}
			// TODO: Replace refresh?
			editModeProperty.set(EditType.NONE);
			tvRequirements.getSelectionModel().clearSelection();
			tvRequirements.refresh();
		} else {
			// TODO: Show error
		}
	}

	private boolean requirementIsValid(String name, String text) {
		//isBlank() requires Java version >= 11
		String nameWithoutWhiteSpaces = name.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		String textWithoutWhiteSpaces = text.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		return nameWithoutWhiteSpaces.length() > 0 && textWithoutWhiteSpaces.length() > 0;
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


}
