package de.prob2.ui.vomanager;

import com.google.inject.Injector;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
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
import javafx.util.StringConverter;
import org.controlsfx.glyphfont.FontAwesome;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class VOManagerStage extends Stage {

	private static enum EditType {
		NONE, ADD, EDIT;
	}

	private static enum Mode {
		NONE, REQUIREMENT, VO, VT
	}

	@FXML
	private TreeTableView<INameable> tvRequirements;

	@FXML
	private TreeTableColumn<INameable, Checked> requirementStatusColumn;

	@FXML
	private TreeTableColumn<INameable, String> requirementNameColumn;

	@FXML
	private TreeTableView<INameable> tvValidationTasks;

	@FXML
	private TreeTableColumn<INameable, Checked> vtStatusColumn;

	@FXML
	private TreeTableColumn<INameable, String> vtNameColumn;

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
	private ChoiceBox<Machine> cbVOLinkMachineChoice;

	@FXML
	private ChoiceBox<Machine> cbVTLinkMachineChoice;

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

	@FXML
	private ChoiceBox<VOManagerSetting> cbViewSetting;

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

		vtStatusColumn.setCellFactory(col -> new TreeCheckedCell<>());
		vtStatusColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("checked"));

		vtNameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));


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
			updateTaskChoice(to);
		});

		cbVTLinkMachineChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to == null) {
				return;
			}
			updateTaskChoice(cbValidationTechniqueChoice.getSelectionModel().getSelectedItem());
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

		final ChangeListener<Project> projectChangeListener = (observable, from, to) -> {
			btAddVO.disableProperty().unbind();

			cbVOLinkMachineChoice.getItems().clear();
			cbVTLinkMachineChoice.getItems().clear();
			cbVOLinkMachineChoice.getItems().addAll(to.getMachines());
			cbVTLinkMachineChoice.getItems().addAll(to.getMachines());

			voManager.synchronizeProject(to);
			btAddVO.disableProperty().bind(to.requirementsProperty().emptyProperty());
			updateRequirementsTable();
			updateValidationTasksTable();
			switchMode(EditType.NONE, Mode.NONE);
		};
		currentProject.addListener(projectChangeListener);
		projectChangeListener.changed(null, null, currentProject.get());


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
			final TreeTableRow<INameable> row = new TreeTableRow<>();

			if(row.getItem() instanceof Machine) {
				return row;
			}

			MenuItem checkItem = new MenuItem(bundle.getString("common.buttons.check"));
			checkItem.setOnAction(e -> {
				IAbstractRequirement item = (IAbstractRequirement) row.getItem();
				if(item instanceof Requirement) {
					VOManagerSetting setting = cbViewSetting.getSelectionModel().getSelectedItem();
					Machine machine = setting == VOManagerSetting.MACHINE ? (Machine) row.getTreeItem().getParent().getValue() : null;
					voChecker.checkRequirement((Requirement) item, machine, setting);
				} else if(item instanceof ValidationObligation) {
					voChecker.checkVO((ValidationObligation) item);
				}

			});

			MenuItem removeItem = new MenuItem(bundle.getString("common.buttons.remove"));
			removeItem.setOnAction(e -> removeRequirement((IAbstractRequirement) row.getItem()));

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(checkItem, removeItem)));
			return row;
		});

		tvRequirements.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to != null && to.getValue() != null) {
				INameable item = to.getValue();
				if(item instanceof Requirement || item instanceof ValidationObligation) {
					showRequirement((IAbstractRequirement) item, true);
				}
			} else {
				switchMode(EditType.NONE, Mode.NONE);
			}
		});

		tvRequirements.setOnMouseClicked(e-> {
			TreeItem<INameable> treeItem = tvRequirements.getSelectionModel().getSelectedItem();
			INameable nameable = treeItem == null ? null : treeItem.getValue();
			if(nameable == null) {
				return;
			}
			if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && currentTrace.get() != null) {
				if (nameable instanceof Requirement) {
					VOManagerSetting setting = cbViewSetting.getSelectionModel().getSelectedItem();
					Machine machine = setting == VOManagerSetting.MACHINE ? (Machine) treeItem.getParent().getValue() : null;
					voChecker.checkRequirement((Requirement) nameable, machine, setting);
				} else if (nameable instanceof ValidationObligation) {
					voChecker.checkVO((ValidationObligation) nameable);
				}
			}
		});

		tvValidationTasks.setRowFactory(table -> {
			final TreeTableRow<INameable> row = new TreeTableRow<>();

			MenuItem checkItem = new MenuItem(bundle.getString("vomanager.menu.items.checkVT"));
			checkItem.setOnAction(e -> {
				INameable item = row.getItem();
				if(item instanceof Machine) {
					return;
				}
				voChecker.checkVT((ValidationTask) item);
			});

			MenuItem removeItem = new MenuItem(bundle.getString("vomanager.menu.items.removeVT"));
			removeItem.setOnAction(e -> {
				INameable item = row.getItem();
				if(item instanceof Machine) {
					return;
				}
				ValidationTask validationTask = (ValidationTask) item;
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
			if(to.getValue() instanceof Machine) {
				return;
			}
			ValidationTask task = (ValidationTask) to.getValue();
			if(task != null) {
				switchMode(EditType.EDIT, Mode.VT);
				showValidationTask(task);
			} else {
				switchMode(EditType.NONE, Mode.NONE);
			}
		});

		tvValidationTasks.setOnMouseClicked(e-> {
			TreeItem<INameable> treeItem = tvValidationTasks.getSelectionModel().getSelectedItem();
			if(treeItem == null) {
				return;
			}
			if(treeItem.getValue() instanceof Machine) {
				return;
			}
			ValidationTask item = (ValidationTask) treeItem.getValue();
			if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item != null && currentTrace.get() != null) {
				voChecker.checkVT(item);
			}
		});

		cbViewSetting.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> updateRequirementsTable());
		cbViewSetting.getSelectionModel().select(VOManagerSetting.MACHINE);
	}

	private void switchMode(EditType editType, Mode mode) {
		editTypeProperty.set(editType);
		modeProperty.set(mode);
	}

	private void updateRequirementsTable() {
		VOManagerSetting setting = cbViewSetting.getSelectionModel().getSelectedItem();

		TreeItem<INameable> root = new TreeItem<>();
		if(setting == VOManagerSetting.MACHINE) {
			// Hierarchy for now: Machine, Requirements, VO as default
			List<Requirement> requirements = currentProject.getRequirements();
			for (Machine machine : currentProject.getMachines()) {
				TreeItem<INameable> machineItem = new TreeItem<>(machine);
				root.getChildren().add(machineItem);
				for (Requirement requirement : requirements) {
					TreeItem<INameable> requirementItem = new TreeItem<>(requirement);
					machineItem.getChildren().add(requirementItem);
					for (ValidationObligation validationObligation : machine.getValidationObligations()) {
						if (validationObligation.getRequirement().equals(requirement.getName())) {
							requirementItem.getChildren().add(new TreeItem<>(validationObligation));
						}
					}
				}
			}
		} else if(setting == VOManagerSetting.REQUIREMENT) {
			List<Requirement> requirements = currentProject.getRequirements();

			for(Requirement requirement : requirements) {
				TreeItem<INameable> requirementItem = new TreeItem<>(requirement);
				root.getChildren().add(requirementItem);
				for(Machine machine : currentProject.getMachines()) {
					TreeItem<INameable> machineItem = new TreeItem<>(machine);
					requirementItem.getChildren().add(machineItem);
					for (ValidationObligation validationObligation : machine.getValidationObligations()) {
						if (validationObligation.getRequirement().equals(requirement.getName())) {
							machineItem.getChildren().add(new TreeItem<>(validationObligation));
						}
					}
				}
			}
		}
		tvRequirements.setRoot(root);
	}

	private void updateValidationTasksTable() {
		TreeItem<INameable> root = new TreeItem<>();
		for(Machine machine : currentProject.getMachines()) {
			TreeItem<INameable> machineItem = new TreeItem<>(machine);
			root.getChildren().add(machineItem);
			for(ValidationTask validationTask : machine.getValidationTasks()) {
				TreeItem<INameable> validationTaskItem = new TreeItem<>(validationTask);
				machineItem.getChildren().add(validationTaskItem);
			}
		}
		tvValidationTasks.setRoot(root);
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
		tfVOName.clear();
		taVOExpression.clear();
		cbLinkRequirementChoice.getItems().clear();
		cbLinkRequirementChoice.getItems().addAll(currentProject.getRequirements());
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
			boolean nameExists = currentProject.getRequirements().stream()
					.map(Requirement::getName)
					.collect(Collectors.toList())
					.contains(tfName.getText());
			EditType editType = editTypeProperty.get();
			if(editType == EditType.ADD) {
				if(nameExists) {
					warnAlreadyExists(Mode.REQUIREMENT);
					return;
				}
				currentProject.getRequirements().add(new Requirement(tfName.getText(), cbRequirementChoice.getValue(), taRequirement.getText()));
			} else if(editType == EditType.EDIT) {
				Requirement requirement = (Requirement) tvRequirements.getSelectionModel().getSelectedItem().getValue();
				if(nameExists && !requirement.getName().equals(tfName.getText())) {
					warnAlreadyExists(Mode.REQUIREMENT);
					return;
				}
				String oldName = requirement.getName();
				requirement.setData(tfName.getText(), cbRequirementChoice.getValue(), taRequirement.getText());

				// Update validation obligations, this means update VO of ids that are affected
				for (Machine machine : currentProject.getMachines()) {
					for(ValidationObligation validationObligation : machine.getValidationObligations()) {
						if(validationObligation.getRequirement().equals(oldName)) {
							validationObligation.setRequirement(tfName.getText());
						}
					}
				}
			}

			// TODO: Replace refresh?
			switchMode(EditType.NONE, Mode.NONE);
			tvRequirements.getSelectionModel().clearSelection();
			updateRequirementsTable();
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
		currentProject.getRequirements().remove(requirement);
		updateRequirementsTable();
		tvRequirements.refresh();
	}

	private void removeVOFromMachine(ValidationObligation validationObligation) {
		Machine machine = currentProject.getCurrentMachine();
		machine.getValidationObligations().remove(validationObligation);
	}

	private void removeVOFromView(ValidationObligation validationObligation) {
		for(TreeItem<INameable> machineItem : tvRequirements.getRoot().getChildren()) {
			for (TreeItem<INameable> requirementItem : machineItem.getChildren()) {
				Requirement requirement = (Requirement) requirementItem.getValue();
				if (requirement.equals(cbLinkRequirementChoice.getValue())) {
					for (TreeItem<INameable> children : requirementItem.getChildren()) {
						ValidationObligation treeItemVO = (ValidationObligation) children.getValue();
						if (treeItemVO.equals(validationObligation)) {
							requirementItem.getChildren().remove(children);
							break;
						}
					}
				}
			}
		}
	}

	private void removeValidationObligation(ValidationObligation validationObligation) {
		removeVOFromMachine(validationObligation);
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
		Requirement requirement = currentProject.getRequirements().stream()
				.filter(req -> req.getName().equals(validationObligation.getRequirement()))
				.collect(Collectors.toList()).get(0);
		Machine linkedMachine = currentProject.getMachines().stream()
				.filter(machine -> machine.getValidationObligations().contains(validationObligation))
				.findAny()
				.orElse(null);
		cbVOLinkMachineChoice.getSelectionModel().select(linkedMachine);
		cbLinkRequirementChoice.getItems().clear();
		cbLinkRequirementChoice.getItems().addAll(currentProject.getRequirements());
		cbLinkRequirementChoice.getSelectionModel().select(requirement);
	}

	private void showValidationTask(ValidationTask validationTask) {
		if(validationTask == null) {
			return;
		}
		tfVTName.setText(validationTask.getId());
		Machine linkedMachine = currentProject.getMachines().stream()
				.filter(machine -> machine.getName().equals(validationTask.getContext()))
				.findAny()
				.orElse(null);
		cbVTLinkMachineChoice.getSelectionModel().select(linkedMachine);
		cbValidationTechniqueChoice.getSelectionModel().select(validationTask.getValidationTechnique());
		cbTaskChoice.getSelectionModel().select(validationTask);
	}

	private void addVOInView(ValidationObligation validationObligation) {
		for(TreeItem<INameable> machineItem : tvRequirements.getRoot().getChildren()) {
			Machine machine = (Machine) machineItem.getValue();
			for (TreeItem<INameable> requirementItem : machineItem.getChildren()) {
				Requirement requirement = (Requirement) requirementItem.getValue();
				if (requirement.equals(cbLinkRequirementChoice.getValue()) && machine.getName().equals(cbVOLinkMachineChoice.getValue().getName())) {
					requirementItem.getChildren().add(new TreeItem<>(validationObligation));
					break;
				}
			}
		}
	}

	private void editVOInView(ValidationObligation validationObligation) {
		validationObligation.setData(tfVOName.getText(), taVOExpression.getText(), cbLinkRequirementChoice.getValue().getName());
	}

	@FXML
	private void applyVO() {
		boolean voIsValid = voIsValid(tfVOName.getText(), cbLinkRequirementChoice.getValue());
		EditType editType = editTypeProperty.get();
		if(voIsValid) {
			ValidationObligation validationObligation;
			Machine machine = cbVOLinkMachineChoice.getSelectionModel().getSelectedItem();
			boolean nameExists = currentProject.getMachines().stream()
					.flatMap(m -> m.getValidationObligations().stream())
					.map(ValidationObligation::getId)
					.collect(Collectors.toList())
					.contains(tfVOName.getText());
			if(editType == EditType.ADD) {
				if(nameExists) {
					warnAlreadyExists(Mode.VO);
					return;
				}
				validationObligation = new ValidationObligation(tfVOName.getText(), taVOExpression.getText(), cbLinkRequirementChoice.getValue().getName());
				machine.getValidationObligations().add(validationObligation);
				addVOInView(validationObligation);
			} else if(editType == EditType.EDIT) {
				TreeItem<INameable> treeItem = tvRequirements.getSelectionModel().getSelectedItem();
				validationObligation = (ValidationObligation) treeItem.getValue();
				if(nameExists && !validationObligation.getName().equals(tfVOName.getText())) {
					warnAlreadyExists(Mode.VO);
					return;
				}
				editVOInView(validationObligation);
			}
			switchMode(EditType.NONE, Mode.NONE);
			tvRequirements.getSelectionModel().clearSelection();
			updateRequirementsTable();
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
			Machine machine = cbVTLinkMachineChoice.getSelectionModel().getSelectedItem();
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
				task.setContext(machine.getName());
				machine.getValidationTasks().add(task);
			} else if(editType == EditType.EDIT) {
				TreeItem<INameable> treeItem = tvValidationTasks.getSelectionModel().getSelectedItem();
				if(treeItem.getValue() instanceof Machine) {
					return;
				}
				ValidationTask currentTask = (ValidationTask) treeItem.getValue();
				if(nameExists && !currentTask.getId().equals(tfVTName.getText())) {
					warnAlreadyExists(Mode.VT);
					return;
				}
				currentTask.setData(tfVTName.getText(), task.getExecutable(), machine.getName(), task.getExecutable(), voManager.extractParameters(task.getExecutable()));
			}
			switchMode(EditType.NONE, Mode.NONE);
			tvValidationTasks.getSelectionModel().clearSelection();
			updateValidationTasksTable();
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

	private void updateTaskChoice(ValidationTechnique validationTechnique) {
		cbTaskChoice.getItems().clear();
		Machine machine = cbVTLinkMachineChoice.getSelectionModel().getSelectedItem();
		if(validationTechnique == null || machine == null) {
			return;
		}
		List<ValidationTask> tasks = voManager.allTasks(validationTechnique, machine);
		if(tasks == null) {
			return;
		}
		cbTaskChoice.getItems().addAll(tasks);
	}

}
