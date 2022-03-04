package de.prob2.ui.vomanager;

import com.google.inject.Injector;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.TreeCheckedCell;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
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
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
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

	private enum EditMode {
		NONE, REQUIREMENT, VO, VT
	}

	@FXML
	private TreeTableView<Requirement> tvRequirements;

	@FXML
	private TreeTableColumn<Requirement, Checked> requirementStatusColumn;

	@FXML
	private TreeTableColumn<Requirement, String> requirementNameColumn;

	@FXML
	private TreeTableColumn<Requirement, String> typeColumn;

	@FXML
	private TreeTableColumn<Requirement, String> specificationColumn;

	@FXML
	private TableView<ValidationTask> tvValidationTasks;

	@FXML
	private TableColumn<ValidationTask, Checked> vtStatusColumn;

	@FXML
	private TableColumn<ValidationTask, String> vtNameColumn;

	@FXML
	private TableColumn<ValidationTask, String> vtConfigurationColumn;

	@FXML
	private Button btAddOrCancelRequirement;

	@FXML
	private Button btAddVT;

	@FXML
	private TextField tfName;

	@FXML
	private TextArea taRequirement;

	@FXML
	private ChoiceBox<RequirementType> cbRequirementChoice;

	@FXML
	private TextField tfVOName;

	@FXML
	private TextArea taVOPredicate;

	@FXML
	private VBox requirementEditingBox;

	@FXML
	private VBox voEditingBox;

	@FXML
	private Button applyButton;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final VOManager voManager;

	private final VOChecker voChecker;

	private final ObjectProperty<EditType> editTypeProperty;

	private final ObjectProperty<EditMode> editModeProperty;

	@Inject
	public VOManagerStage(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace, final Injector injector,
			final VOManager voManager, final VOChecker voChecker) {
		super();
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.voManager = voManager;
		this.voChecker = voChecker;
		this.editTypeProperty = new SimpleObjectProperty<>(EditType.NONE);
		this.editModeProperty = new SimpleObjectProperty<>(EditMode.NONE);
		stageManager.loadFXML(this, "vo_manager_view.fxml");
	}

	@FXML
	public void initialize() {
		requirementStatusColumn.setCellFactory(col -> new TreeCheckedCell<>());
		requirementStatusColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("checked"));

		requirementNameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		typeColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("shortTypeName"));
		specificationColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("text"));
		tvRequirements.setShowRoot(false);
		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			tvValidationTasks.itemsProperty().unbind();
			if(to != null) {
				voManager.synchronizeMachine(to);
				tvValidationTasks.itemsProperty().bind(to.validationTasksProperty());
			}
			updateRoot();
			editTypeProperty.set(EditType.NONE);
			editModeProperty.set(EditMode.NONE);
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());

		requirementEditingBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> editTypeProperty.get() != EditType.NONE && editModeProperty.get() == EditMode.REQUIREMENT, editTypeProperty, editModeProperty));
		voEditingBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> editTypeProperty.get() != EditType.NONE && editModeProperty.get() == EditMode.VO, editTypeProperty, editModeProperty));

		editTypeProperty.addListener((observable, from, to) -> {
			if(to != EditType.NONE) {
				btAddOrCancelRequirement.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.TIMES_CIRCLE));
				btAddOrCancelRequirement.setTooltip(new Tooltip());
			} else {
				if(editModeProperty.get() == EditMode.REQUIREMENT) {
					btAddOrCancelRequirement.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.PLUS_CIRCLE));
					btAddOrCancelRequirement.setTooltip(new Tooltip());
				}
			}
		});
		applyButton.visibleProperty().bind(cbRequirementChoice.getSelectionModel().selectedItemProperty().isNotNull());

		tvRequirements.setRowFactory(table -> {
			final TreeTableRow<Requirement> row = new TreeTableRow<>();

			//Menu linkItem = new Menu("Link to Validation Obligation");

			//row.itemProperty().addListener((observable, from, to) -> {
			//	final InvalidationListener linkingListener = o -> voManager.showPossibleLinkings(linkItem, to);
			//	voManager.updateLinkingListener(linkItem, from, to, linkingListener);
			//});

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
							.otherwise(new ContextMenu(checkItem, removeItem)));
			return row;
		});

		tvRequirements.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to != null && to.getValue() != null) {
				Requirement requirement = to.getValue();
				editTypeProperty.set(EditType.EDIT);
				editModeProperty.set(EditMode.REQUIREMENT);
				showRequirement(requirement);
			} else {
				editTypeProperty.set(EditType.NONE);
				editModeProperty.set(EditMode.NONE);
			}
		});

		tvRequirements.setOnMouseClicked(e-> {
			TreeItem<Requirement> treeItem = tvRequirements.getSelectionModel().getSelectedItem();
			Requirement requirement = treeItem == null ? null : treeItem.getValue();
			if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && requirement != null && currentTrace.get() != null) {
				requirement.getValidationObligations().forEach(voChecker::check);
			}
		});

		tvValidationTasks.setRowFactory(table -> {
			final TableRow<ValidationTask> row = new TableRow<>();

			MenuItem checkItem = new MenuItem("Check VT");
			checkItem.setOnAction(e -> voChecker.check(row.getItem()));

			MenuItem removeItem = new MenuItem("Remove VT");
			removeItem.setOnAction(e -> {
				ValidationTask validationTask = row.getItem();
				Machine currentMachine = currentProject.getCurrentMachine();
				currentMachine.getValidationTasks().remove(validationTask);
				// TODO: Implement dependency between VO and VT
			});

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(checkItem, removeItem)));
			return row;
		});

		tvValidationTasks.setOnMouseClicked(e-> {
			ValidationTask item = tvValidationTasks.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item != null && currentTrace.get() != null) {
				voChecker.check(item);
			}
		});
	}

	private void updateRoot() {
		Machine currentMachine = currentProject.getCurrentMachine();
		if(currentMachine == null) {
			return;
		}
		List<Requirement> requirements = currentProject.getCurrentMachine().getRequirements();
		TreeItem<Requirement> root = new TreeItem<>();
		for(Requirement requirement : requirements) {
			root.getChildren().add(new TreeItem<>(requirement));
		}
		tvRequirements.setRoot(root);
	}

	@FXML
	public void addOrCancelRequirement() {
		if(editTypeProperty.get() == EditType.NONE) {
			cbRequirementChoice.getSelectionModel().clearSelection();
			taRequirement.clear();
			tfName.clear();
			editTypeProperty.set(EditType.ADD);
			editModeProperty.set(EditMode.REQUIREMENT);
		} else {
			editTypeProperty.set(EditType.NONE);
			editModeProperty.set(EditMode.NONE);
		}
		tvRequirements.getSelectionModel().clearSelection();
	}

	@FXML
	public void applyRequirement() {
		EditType editType = editTypeProperty.get();
		boolean requirementIsValid = requirementIsValid(tfName.getText(), taRequirement.getText());

		if(requirementIsValid) {
			Requirement requirement = null;
			if(editType == EditType.ADD) {
				requirement = new Requirement(tfName.getText(), cbRequirementChoice.getValue(), taRequirement.getText(), Collections.emptyList());
				currentProject.getCurrentMachine().getRequirements().add(requirement);
			} else if(editType == EditType.EDIT) {
				requirement = tvRequirements.getSelectionModel().getSelectedItem().getValue();
				requirement.setName(tfName.getText());
				requirement.setType(cbRequirementChoice.getValue());
				requirement.setText(taRequirement.getText());
			}
			assert requirement != null;

			// TODO: Replace refresh?
			editTypeProperty.set(EditType.NONE);
			editModeProperty.set(EditMode.NONE);
			tvRequirements.getSelectionModel().clearSelection();
			updateRoot();
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
		Requirement requirement = tvRequirements.getSelectionModel().getSelectedItem().getValue();
		currentProject.getCurrentMachine().getRequirements().remove(requirement);
		updateRoot();
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

	@FXML
	private void applyVO() {

	}

	@FXML
	private void addVT() {

	}

}
