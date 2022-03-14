package de.prob2.ui.vomanager;

import com.google.inject.Injector;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.TreeCheckedCell;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.controlsfx.glyphfont.FontAwesome;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class VOManagerStage extends Stage {

	private enum EditType {
		NONE, ADD, EDIT;
	}

	private enum Mode {
		NONE, REQUIREMENT, VO, VT
	}

	@FXML
	private TreeTableView<IAbstractRequirement> tvRequirements;

	@FXML
	private TreeTableColumn<IAbstractRequirement, Checked> requirementStatusColumn;

	@FXML
	private TreeTableColumn<IAbstractRequirement, String> requirementNameColumn;

	@FXML
	private TreeTableColumn<IAbstractRequirement, String> typeColumn;

	@FXML
	private TreeTableColumn<IAbstractRequirement, String> specificationColumn;

	@FXML
	private TableView<ValidationTask> tvValidationTasks;

	@FXML
	private TableColumn<ValidationTask, Checked> vtStatusColumn;

	@FXML
	private TableColumn<ValidationTask, String> vtNameColumn;

	@FXML
	private TableColumn<ValidationTask, String> vtParametersColumn;

	@FXML
	private MenuButton btAddRequirementVO;

	@FXML
	private MenuItem btAddVO;

	@FXML
	private Button btAddVT;

	@FXML
	private TextField tfName;

	@FXML
	private TextArea taRequirement;

	@FXML
	private ChoiceBox<RequirementType> cbRequirementChoice;

	@FXML
	private ChoiceBox<ValidationTechnique> cbValidationTechniqueChoice;

	@FXML
	private ChoiceBox<Requirement> cbLinkRequirementChoice;

	@FXML
	private ChoiceBox<ValidationTask> cbTaskChoice;

	@FXML
	private TextField tfVOName;

	@FXML
	private TextArea taVOExpression;

	@FXML
	private TextField tfVTName;

	@FXML
	private VBox requirementEditingBox;

	@FXML
	private VBox voEditingBox;

	@FXML
	private VBox vtEditingBox;

	@FXML
	private VBox validationTaskBox;

	@FXML
	private Button applyButton;

	@FXML
	private Button applyVTButton;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final VOManager voManager;

	private final VOChecker voChecker;

	private final ResourceBundle bundle;

	private final ObjectProperty<EditType> editTypeProperty;

	private final ObjectProperty<Mode> modeProperty;

	@Inject
	public VOManagerStage(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace, final Injector injector,
						  final VOManager voManager, final VOChecker voChecker, final ResourceBundle bundle) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.voManager = voManager;
		this.voChecker = voChecker;
		this.bundle = bundle;
		this.editTypeProperty = new SimpleObjectProperty<>(EditType.NONE);
		this.modeProperty = new SimpleObjectProperty<>(Mode.NONE);
		stageManager.loadFXML(this, "vo_manager_view.fxml");
	}

	@FXML
	public void initialize() {
		requirementStatusColumn.setCellFactory(col -> new TreeCheckedCell<>());
		requirementStatusColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("checked"));

		requirementNameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		typeColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("shortTypeName"));
		specificationColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("configuration"));

		vtStatusColumn.setCellFactory(col -> new CheckedCell<>());
		vtStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));

		vtNameColumn.setCellValueFactory(new PropertyValueFactory<>("prefix"));
		vtParametersColumn.setCellValueFactory(new PropertyValueFactory<>("parameters"));

		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			btAddVO.disableProperty().unbind();

			tvValidationTasks.itemsProperty().unbind();
			if(to != null) {
				voManager.synchronizeMachine(to);
				tvValidationTasks.itemsProperty().bind(to.validationTasksProperty());
				btAddVO.disableProperty().bind(to.requirementsProperty().emptyProperty());
			}
			updateRoot();
			switchMode(EditType.NONE, Mode.NONE);
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());

		requirementEditingBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> editTypeProperty.get() != EditType.NONE && modeProperty.get() == Mode.REQUIREMENT, editTypeProperty, modeProperty));
		voEditingBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> editTypeProperty.get() != EditType.NONE && modeProperty.get() == Mode.VO, editTypeProperty, modeProperty));
		vtEditingBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> editTypeProperty.get() != EditType.NONE && modeProperty.get() == Mode.VT, editTypeProperty, modeProperty));

		validationTaskBox.visibleProperty().bind(cbValidationTechniqueChoice.getSelectionModel().selectedItemProperty().isNotNull());
		applyButton.visibleProperty().bind(cbRequirementChoice.getSelectionModel().selectedItemProperty().isNotNull());
		applyVTButton.visibleProperty().bind(cbValidationTechniqueChoice.getSelectionModel().selectedItemProperty().isNotNull().and(cbTaskChoice.getSelectionModel().selectedItemProperty().isNotNull()));



		cbTaskChoice.setConverter(new StringConverter<ValidationTask>() {
			@Override
			public String toString(ValidationTask object) {
				if(object == null) {
					return "";
				}
				return object.getParameters();
			}

			@Override
			public ValidationTask fromString(String string) {
				return null;
			}
		});

		cbValidationTechniqueChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to == null) {
				return;
			}
			cbTaskChoice.getItems().clear();
			List<ValidationTask> tasks = voManager.allTasks(to);
			if(tasks == null) {
				return;
			}
			cbTaskChoice.getItems().addAll(tasks);
		});

		cbLinkRequirementChoice.setConverter(new StringConverter<Requirement>() {
			@Override
			public String toString(Requirement object) {
				if(object == null) {
					return "";
				}
				return object.getName();
			}

			@Override
			public Requirement fromString(String string) {
				return null;
			}
		});

		modeProperty.addListener((observable, from, to) -> {
			if(to == Mode.VT) {
				btAddVT.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.TIMES_CIRCLE));
				btAddVT.setTooltip(new Tooltip());
			} else {
				btAddVT.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.PLUS_CIRCLE));
				btAddVT.setTooltip(new Tooltip());
			}
		});

		tvRequirements.setRowFactory(table -> {
			final TreeTableRow<IAbstractRequirement> row = new TreeTableRow<>();

			MenuItem checkItem = new MenuItem(bundle.getString("common.buttons.check"));
			checkItem.setOnAction(e -> voChecker.check(row.getItem()));

			MenuItem removeItem = new MenuItem(bundle.getString("common.buttons.remove"));
			removeItem.setOnAction(e -> removeRequirement(row.getItem()));

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(checkItem, removeItem)));
			return row;
		});

		tvRequirements.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to != null && to.getValue() != null) {
				IAbstractRequirement item = to.getValue();
				showRequirement(item, true);
			} else {
				switchMode(EditType.NONE, Mode.NONE);
			}
		});

		tvRequirements.setOnMouseClicked(e-> {
			TreeItem<IAbstractRequirement> treeItem = tvRequirements.getSelectionModel().getSelectedItem();
			IAbstractRequirement abstractRequirement = treeItem == null ? null : treeItem.getValue();

			if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && abstractRequirement != null && currentTrace.get() != null) {
				voChecker.check(abstractRequirement);
			}
		});

		tvValidationTasks.setRowFactory(table -> {
			final TableRow<ValidationTask> row = new TableRow<>();

			MenuItem checkItem = new MenuItem(bundle.getString("vomanager.menu.items.checkVT"));
			checkItem.setOnAction(e -> voChecker.check(row.getItem()));

			MenuItem removeItem = new MenuItem(bundle.getString("vomanager.menu.items.removeVT"));
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

		tvValidationTasks.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to != null) {
				switchMode(EditType.EDIT, Mode.VT);
				showValidationTask(to);
			} else {
				switchMode(EditType.NONE, Mode.NONE);
			}
		});

		tvValidationTasks.setOnMouseClicked(e-> {
			ValidationTask item = tvValidationTasks.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item != null && currentTrace.get() != null) {
				voChecker.check(item);
			}
		});
	}

	private void switchMode(EditType editType, Mode mode) {
		editTypeProperty.set(editType);
		modeProperty.set(mode);
	}

	private void updateRoot() {
		Machine currentMachine = currentProject.getCurrentMachine();
		if(currentMachine == null) {
			return;
		}
		List<Requirement> requirements = currentProject.getCurrentMachine().getRequirements();
		TreeItem<IAbstractRequirement> root = new TreeItem<>();
		for(Requirement requirement : requirements) {
			TreeItem<IAbstractRequirement> treeItem = new TreeItem<>(requirement);
			root.getChildren().add(treeItem);
			for(ValidationObligation validationObligation : requirement.getValidationObligations()) {
				treeItem.getChildren().add(new TreeItem<>(validationObligation));
			}
		}
		tvRequirements.setRoot(root);
	}

	private void resetRequirementEditing() {
		cbRequirementChoice.getSelectionModel().clearSelection();
		taRequirement.clear();
		tfName.clear();
		tvRequirements.getSelectionModel().clearSelection();
	}

	@FXML
	public void addRequirement() {
		resetRequirementEditing();
		switchMode(EditType.ADD, Mode.REQUIREMENT);
	}

	private void resetVOEditing() {
		Machine machine = currentProject.getCurrentMachine();
		tfVOName.clear();
		taVOExpression.clear();
		cbLinkRequirementChoice.getItems().clear();
		cbLinkRequirementChoice.getItems().addAll(machine.getRequirements());
		tvRequirements.getSelectionModel().clearSelection();
	}

	@FXML
	public void addVO() {
		resetVOEditing();
		switchMode(EditType.ADD, Mode.VO);
	}

	@FXML
	public void applyRequirement() {
		if(requirementIsValid(tfName.getText(), taRequirement.getText())) {
			Machine machine = currentProject.getCurrentMachine();
			boolean nameExists = machine.getRequirements().stream()
					.map(Requirement::getName)
					.collect(Collectors.toList())
					.contains(tfName.getText());
			EditType editType = editTypeProperty.get();
			if(editType == EditType.ADD) {
				if(nameExists) {
					warnAlreadyExists(Mode.REQUIREMENT);
					return;
				}
				machine.getRequirements().add(new Requirement(tfName.getText(), cbRequirementChoice.getValue(), taRequirement.getText(), Collections.emptyList()));
			} else if(editType == EditType.EDIT) {
				Requirement requirement = (Requirement) tvRequirements.getSelectionModel().getSelectedItem().getValue();
				if(nameExists && !requirement.getName().equals(tfName.getText())) {
					warnAlreadyExists(Mode.REQUIREMENT);
					return;
				}
				requirement.setData(tfName.getText(), cbRequirementChoice.getValue(), taRequirement.getText());
				requirement.updateValidationObligations();
			}

			// TODO: Replace refresh?
			switchMode(EditType.NONE, Mode.NONE);
			tvRequirements.getSelectionModel().clearSelection();
			updateRoot();
			tvRequirements.refresh();
		} else {
			warnNotValid(Mode.REQUIREMENT);
		}
	}

	private boolean requirementIsValid(String name, String text) {
		//isBlank() requires Java version >= 11
		String nameWithoutWhiteSpaces = name.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		String textWithoutWhiteSpaces = text.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		return nameWithoutWhiteSpaces.length() > 0 && textWithoutWhiteSpaces.length() > 0;
	}

	private boolean taskIsValid(String name) {
		//isBlank() requires Java version >= 11
		String nameWithoutWhiteSpaces = name.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		return nameWithoutWhiteSpaces.length() > 0;
	}

	private boolean voIsValid(String name, Requirement requirement) {
		//isBlank() requires Java version >= 11
		if(requirement == null) {
			return false;
		}
		String nameWithoutWhiteSpaces = name.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		return nameWithoutWhiteSpaces.length() > 0;
	}

	private void removeRequirement(IAbstractRequirement requirement) {
		if(requirement instanceof Requirement) {
			removeRequirement((Requirement) requirement);
		} else if(requirement instanceof ValidationObligation) {
			removeValidationObligation((ValidationObligation) requirement);
		}
	}

	private void removeRequirement(Requirement requirement) {
		currentProject.getCurrentMachine().getRequirements().remove(requirement);
		updateRoot();
		tvRequirements.refresh();
	}

	private void removeVOFromRequirement(ValidationObligation validationObligation) {
		Machine machine = currentProject.getCurrentMachine();
		String requirementID = validationObligation.getRequirement();

		for(Requirement requirement : machine.getRequirements()) {
			if(requirement.getName().equals(requirementID)) {
				requirement.removeValidationObligation(validationObligation);
				break;
			}
		}
	}

	private void removeVOFromView(ValidationObligation validationObligation) {
		for(TreeItem<IAbstractRequirement> treeItem : tvRequirements.getRoot().getChildren()) {
			Requirement requirement = (Requirement) treeItem.getValue();
			if(requirement.equals(cbLinkRequirementChoice.getValue())) {
				for(TreeItem<IAbstractRequirement> children : treeItem.getChildren()) {
					ValidationObligation treeItemVO = (ValidationObligation) children.getValue();
					if(treeItemVO.equals(validationObligation)) {
						treeItem.getChildren().remove(children);
						break;
					}
				}
			}
		}
	}

	private void removeValidationObligation(ValidationObligation validationObligation) {
		removeVOFromRequirement(validationObligation);
		removeVOFromView(validationObligation);
		tvRequirements.refresh();
	}

	private void showRequirement(IAbstractRequirement requirement, boolean edit) {
		if(requirement instanceof Requirement) {
			showRequirement((Requirement) requirement, edit);
		} else if(requirement instanceof ValidationObligation) {
			showValidationObligation((ValidationObligation) requirement, edit);
		}
	}

	private void showRequirement(Requirement requirement, boolean edit) {
		if(edit) {
			switchMode(EditType.EDIT, Mode.REQUIREMENT);
		}
		if(requirement == null) {
			return;
		}
		tfName.setText(requirement.getName());
		cbRequirementChoice.getSelectionModel().select(requirement.getType());
		taRequirement.setText(requirement.getText());
	}

	private void showValidationObligation(ValidationObligation validationObligation, boolean edit) {
		if(edit) {
			switchMode(EditType.EDIT, Mode.VO);
		}
		if(validationObligation == null) {
			return;
		}
		tfVOName.setText(validationObligation.getId());
		taVOExpression.setText(validationObligation.getExpression());
		Machine machine = currentProject.getCurrentMachine();
		Requirement requirement = machine.getRequirements().stream()
				.filter(req -> req.getName().equals(validationObligation.getRequirement()))
				.collect(Collectors.toList()).get(0);
		cbLinkRequirementChoice.getItems().clear();
		cbLinkRequirementChoice.getItems().addAll(machine.getRequirements());
		cbLinkRequirementChoice.getSelectionModel().select(requirement);
	}

	private void showValidationTask(ValidationTask validationTask) {
		if(validationTask == null) {
			return;
		}
		tfVTName.setText(validationTask.getId());
		cbValidationTechniqueChoice.getSelectionModel().select(validationTask.getValidationTechnique());
		cbTaskChoice.getSelectionModel().select(validationTask);
	}

	private void addVOInView(ValidationObligation validationObligation) {
		for(TreeItem<IAbstractRequirement> treeItem : tvRequirements.getRoot().getChildren()) {
			Requirement requirement = (Requirement) treeItem.getValue();
			if(requirement.equals(cbLinkRequirementChoice.getValue())) {
				requirement.addValidationObligation(validationObligation);
				break;
			}
		}
	}

	private void editVOInView(Machine machine, ValidationObligation validationObligation) {
		for(Requirement req : machine.getRequirements()) {
			if(req.getName().equals(validationObligation.getRequirement())) {
				req.getValidationObligations().remove(validationObligation);
			}
			if(req.getName().equals(cbLinkRequirementChoice.getValue().getName())) {
				req.getValidationObligations().add(validationObligation);
			}
		}
		validationObligation.setData(tfVOName.getText(), taVOExpression.getText(), cbLinkRequirementChoice.getValue().getName());
	}

	@FXML
	private void applyVO() {
		boolean voIsValid = voIsValid(tfVOName.getText(), cbLinkRequirementChoice.getValue());
		EditType editType = editTypeProperty.get();
		if(voIsValid) {
			ValidationObligation validationObligation;
			Machine machine = currentProject.getCurrentMachine();
			boolean nameExists = machine.getRequirements().stream()
					.flatMap(requirement -> requirement.getValidationObligations().stream())
					.map(ValidationObligation::getId)
					.collect(Collectors.toList())
					.contains(tfVOName.getText());
			if(editType == EditType.ADD) {
				if(nameExists) {
					warnAlreadyExists(Mode.VO);
					return;
				}
				addVOInView(new ValidationObligation(tfVOName.getText(), taVOExpression.getText(), cbLinkRequirementChoice.getValue().getName()));
			} else if(editType == EditType.EDIT) {
				TreeItem<IAbstractRequirement> treeItem = tvRequirements.getSelectionModel().getSelectedItem();
				validationObligation = (ValidationObligation) treeItem.getValue();
				if(nameExists && !validationObligation.getName().equals(tfVOName.getText())) {
					warnAlreadyExists(Mode.VO);
					return;
				}
				editVOInView(machine, validationObligation);
			}
			switchMode(EditType.NONE, Mode.NONE);
			tvRequirements.getSelectionModel().clearSelection();
			updateRoot();
			tvRequirements.refresh();
		} else {
			warnNotValid(Mode.VO);
		}
	}

	private void resetVTEditing() {
		tfVTName.clear();
		cbValidationTechniqueChoice.getSelectionModel().clearSelection();
		cbTaskChoice.getItems().clear();
	}

	@FXML
	private void addVT() {
		if(editTypeProperty.get() == EditType.NONE || modeProperty.get() != Mode.VT) {
			resetVTEditing();
			switchMode(EditType.ADD, Mode.VT);
		} else {
			switchMode(EditType.NONE, Mode.NONE);
		}
		tvValidationTasks.getSelectionModel().clearSelection();
	}

	@FXML
	private void applyVT() {
		boolean taskIsValid = taskIsValid(tfVTName.getText());
		EditType editType = editTypeProperty.get();
		if(taskIsValid) {
			ValidationTask task = cbTaskChoice.getSelectionModel().getSelectedItem();
			if(task == null) {
				return;
			}
			Machine machine = currentProject.getCurrentMachine();
			boolean nameExists = machine.getValidationTasks().stream()
					.map(ValidationTask::getId)
					.collect(Collectors.toList())
					.contains(tfVTName.getText());
			if(editType == EditType.ADD) {
				if(nameExists) {
					warnAlreadyExists(Mode.VT);
					return;
				}
				task.setId(tfVTName.getText());
				machine.getValidationTasks().add(task);
			} else if(editType == EditType.EDIT) {
				ValidationTask currentTask = tvValidationTasks.getSelectionModel().getSelectedItem();
				if(nameExists && !currentTask.getId().equals(tfVTName.getText())) {
					warnAlreadyExists(Mode.VT);
					return;
				}
				currentTask.setData(tfVTName.getText(), task.getExecutable(), machine.getName(), task.getExecutable(), voManager.extractParameters(task.getExecutable()));
			}
			switchMode(EditType.NONE, Mode.NONE);
			tvValidationTasks.getSelectionModel().clearSelection();
			tvValidationTasks.refresh();
		} else {
			warnNotValid(Mode.VT);
		}

	}

	public void warnNotValid(Mode mode) {
		switch(mode) {
			case REQUIREMENT:
				stageManager.makeAlert(Alert.AlertType.INFORMATION, bundle.getString("vomanager.warnings.requirement.notValid.header"), bundle.getString("vomanager.warnings.requirement.notValid.content")).show();
				break;
			case VO:
				stageManager.makeAlert(Alert.AlertType.INFORMATION, bundle.getString("vomanager.warnings.vo.notValid.header"), bundle.getString("vomanager.warnings.vo.notValid.content")).show();
				break;
			case VT:
				stageManager.makeAlert(Alert.AlertType.INFORMATION, bundle.getString("vomanager.warnings.vt.notValid.header"), bundle.getString("vomanager.warnings.vt.notValid.content")).show();
				break;
			default:
				throw new RuntimeException("Mode is not valid");
		}
	}

	public void warnAlreadyExists(Mode mode) {
		switch(mode) {
			case REQUIREMENT:
				stageManager.makeAlert(Alert.AlertType.INFORMATION, bundle.getString("vomanager.warnings.requirement.alreadyExists.header"), bundle.getString("vomanager.warnings.requirement.alreadyExists.content")).show();
				break;
			case VO:
				stageManager.makeAlert(Alert.AlertType.INFORMATION, bundle.getString("vomanager.warnings.vo.alreadyExists.header"), bundle.getString("vomanager.warnings.vo.alreadyExists.content")).show();
				break;
			case VT:
				stageManager.makeAlert(Alert.AlertType.INFORMATION, bundle.getString("vomanager.warnings.vt.alreadyExists.header"), bundle.getString("vomanager.warnings.vt.alreadyExists.content")).show();
				break;
			default:
				throw new RuntimeException("Mode is not valid");
		}
	}

}
