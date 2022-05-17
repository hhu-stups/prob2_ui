package de.prob2.ui.vomanager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.TreeCheckedCell;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

@FXMLInjected
@Singleton
public class VOManagerStage extends Stage {

	public static enum EditType {
		NONE, ADD, EDIT;
	}

	public static enum Mode {
		NONE, REQUIREMENT, VO
	}

	@FXML
	private TreeTableView<INameable> tvRequirements;

	@FXML
	private TreeTableColumn<INameable, Checked> requirementStatusColumn;

	@FXML
	private TreeTableColumn<INameable, String> requirementNameColumn;

	@FXML
	private TreeTableView<Object> tvValidationTasks; // contains Machine and ValidationTask objects

	@FXML
	private TreeTableColumn<Object, Checked> vtStatusColumn;

	@FXML
	private TreeTableColumn<Object, String> vtNameColumn;

	@FXML
	private MenuButton btAddRequirementVO;

	@FXML
	private MenuItem btAddVO;

	@FXML
	private RequirementsEditingBox requirementEditingBox;

	@FXML
	private VOEditingBox voEditingBox;

	@FXML
	private ChoiceBox<VOManagerSetting> cbViewSetting;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final VOChecker voChecker;

	private final RequirementHandler requirementHandler;

	private final ResourceBundle bundle;

	private final ObjectProperty<EditType> editTypeProperty;

	private final ObjectProperty<Mode> modeProperty;

	private final Map<String, List<String>> refinementChain;

	@Inject
	public VOManagerStage(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace,
			final VOChecker voChecker, final RequirementHandler requirementHandler, final ResourceBundle bundle) {
		super();
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.voChecker = voChecker;
		this.requirementHandler = requirementHandler;
		this.bundle = bundle;
		this.editTypeProperty = new SimpleObjectProperty<>(EditType.NONE);
		this.modeProperty = new SimpleObjectProperty<>(Mode.NONE);
		this.refinementChain = new HashMap<>();
		stageManager.loadFXML(this, "vo_manager_view.fxml", this.getClass().getName());
	}

	private void initializeTables() {
		requirementStatusColumn.setCellFactory(col -> new TreeCheckedCell<>());
		requirementStatusColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("checked"));
		requirementNameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		vtStatusColumn.setCellFactory(col -> new TreeCheckedCell<>());
		vtStatusColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("checked"));
		vtNameColumn.setCellValueFactory(features -> {
			final Object obj = features.getValue().getValue();
			final StringProperty value = new SimpleStringProperty();
			if (obj instanceof INameable) {
				value.set(((INameable)obj).getName());
			} else if (obj instanceof IValidationTask) {
				value.set(((IValidationTask)obj).getId());
			} else {
				throw new IllegalArgumentException("Unsupported class in VT table: " + value.getClass());
			}
			return value;
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
			final TreeTableRow<Object> row = new TreeTableRow<>();

			MenuItem checkItem = new MenuItem(bundle.getString("vomanager.menu.items.checkVT"));
			checkItem.setOnAction(e -> {
				Object item = row.getItem();
				if(item instanceof IValidationTask) {
					voChecker.checkVT((IValidationTask) item);
				}
			});

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(checkItem)));
			return row;
		});

		tvValidationTasks.setOnMouseClicked(e-> {
			TreeItem<Object> treeItem = tvValidationTasks.getSelectionModel().getSelectedItem();
			if(treeItem == null) {
				return;
			}
			if(treeItem.getValue() instanceof IValidationTask) {
				IValidationTask item = (IValidationTask) treeItem.getValue();
				if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item != null && currentTrace.get() != null) {
					voChecker.checkVT(item);
				}
			}
		});

		currentTrace.stateSpaceProperty().addListener((o, from, to) -> initializeRefinementHierarchy(to));
		initializeRefinementHierarchy(currentTrace.getStateSpace());
	}

	private void initializeRefinementHierarchy(StateSpace stateSpace) {
		if(stateSpace == null) {
			return;
		}
		this.resolveRefinementHierarchy(stateSpace.getModel());
		updateRequirementsTable();
		tvRequirements.refresh();
	}

	private void initializeEditingBoxes() {
		requirementEditingBox.setVoManagerStage(this);
		voEditingBox.setVoManagerStage(this);

		requirementEditingBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> editTypeProperty.get() != EditType.NONE && modeProperty.get() == Mode.REQUIREMENT, editTypeProperty, modeProperty));
		voEditingBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> editTypeProperty.get() != EditType.NONE && modeProperty.get() == Mode.VO, editTypeProperty, modeProperty));
	}

	private void initializeChoiceBoxes() {
		cbViewSetting.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> updateRequirementsTable());
		cbViewSetting.getSelectionModel().select(VOManagerSetting.MACHINE);
	}

	private void initializeListenerOnProjectChange() {
		final ChangeListener<Project> projectChangeListener = (observable, from, to) -> {
			btAddVO.disableProperty().unbind();
			final List<Machine> machines = to == null ? Collections.emptyList() : to.getMachines();
			requirementEditingBox.updateLinkedMachines(machines);
			voEditingBox.updateLinkedMachines(machines);

			if(from != null) {
				for (Requirement requirement : from.getRequirements()) {
					requirementHandler.resetListeners(from, requirement);
				}
			}

			if (to != null) {
				btAddVO.disableProperty().bind(to.requirementsProperty().emptyProperty());

				for(Requirement requirement : to.getRequirements()) {
					// TODO: Distinguish between two views for tvRequirements
					requirementHandler.initListeners(to, null, requirement, VOManagerSetting.REQUIREMENT);
				}

				updateRequirementsTable();
				updateValidationTasksTable();
			} else {
				tvRequirements.setRoot(null);
				tvValidationTasks.setRoot(null);
			}

			switchMode(EditType.NONE, Mode.NONE);
			btAddRequirementVO.setDisable(to == null);
		};
		currentProject.addListener(projectChangeListener);
		projectChangeListener.changed(null, null, currentProject.get());
	}

	@FXML
	public void initialize() {
		initializeTables();
		initializeEditingBoxes();
		initializeChoiceBoxes();
		initializeListenerOnProjectChange();
	}

	public void switchMode(EditType editType, Mode mode) {
		editTypeProperty.set(editType);
		modeProperty.set(mode);
	}

	public void updateRequirementsTable() {
		VOManagerSetting setting = cbViewSetting.getSelectionModel().getSelectedItem();
		TreeItem<INameable> root = new TreeItem<>();
		if(setting == VOManagerSetting.MACHINE) {
			updateMachineRequirementsTable(root);
		} else if(setting == VOManagerSetting.REQUIREMENT) {
			updateRequirementsMachineTable(root);
		}
		tvRequirements.setRoot(root);
	}

	private void updateMachineRequirementsTable(TreeItem<INameable> root) {
		for (Machine machine : currentProject.getMachines()) {
			TreeItem<INameable> machineItem = new TreeItem<>(machine);
			for (Requirement requirement : currentProject.getRequirements()) {
				if (currentTrace.getModel() == null || (refinementChain.containsKey(machine.getName()) && refinementChain.get(machine.getName()).contains(requirement.getIntroducedAt()))) {
					TreeItem<INameable> requirementItem = new TreeItem<>(requirement);
					for (ValidationObligation validationObligation : machine.getValidationObligations()) {
						if (validationObligation.getRequirement().equals(requirement.getName())) {
							requirementItem.getChildren().add(new TreeItem<>(validationObligation));
						}
					}
					// Show the requirement under the machine where it was introduced
					// and under any other machines that have corresponding VOs.
					if (requirement.getIntroducedAt().equals(machine.getName()) || !requirementItem.getChildren().isEmpty()) {
						machineItem.getChildren().add(requirementItem);
					}
				}
			}
			if (!machineItem.getChildren().isEmpty()) {
				root.getChildren().add(machineItem);
			}
		}
	}

	private void updateRequirementsMachineTable(TreeItem<INameable> root) {
		for(Requirement requirement : currentProject.getRequirements()) {
			TreeItem<INameable> requirementItem = new TreeItem<>(requirement);
			for(Machine machine : currentProject.getMachines()) {
				if (currentTrace.getModel() == null || (refinementChain.containsKey(machine.getName()) && refinementChain.get(machine.getName()).contains(requirement.getIntroducedAt()))) {
					TreeItem<INameable> machineItem = new TreeItem<>(machine);
					for (ValidationObligation validationObligation : machine.getValidationObligations()) {
						if (validationObligation.getRequirement().equals(requirement.getName())) {
							machineItem.getChildren().add(new TreeItem<>(validationObligation));
						}
					}
					if (!machineItem.getChildren().isEmpty()) {
						requirementItem.getChildren().add(machineItem);
					}
				}
			}
			root.getChildren().add(requirementItem);
		}
	}

	public void updateValidationTasksTable() {
		TreeItem<Object> root = new TreeItem<>();
		for(Machine machine : currentProject.getMachines()) {
			if (machine.getValidationTasks().isEmpty()) {
				continue;
			}
			TreeItem<Object> machineItem = new TreeItem<>(machine);
			root.getChildren().add(machineItem);
			for(IValidationTask validationTask : machine.getValidationTasks()) {
				TreeItem<Object> validationTaskItem = new TreeItem<>(validationTask);
				machineItem.getChildren().add(validationTaskItem);
			}
		}
		tvValidationTasks.setRoot(root);
	}

	@FXML
	public void addRequirement() {
		requirementEditingBox.resetRequirementEditing();
		switchMode(EditType.ADD, Mode.REQUIREMENT);
	}

	@FXML
	public void addVO() {
		voEditingBox.resetVOEditing();
		switchMode(EditType.ADD, Mode.VO);
	}

	private void removeRequirement(IAbstractRequirement requirement) {
		if(requirement instanceof Requirement) {
			removeRequirement((Requirement) requirement);
		} else if(requirement instanceof ValidationObligation) {
			removeValidationObligation((ValidationObligation) requirement);
		}
	}

	private void removeRequirement(Requirement requirement) {
		currentProject.removeRequirement(requirement);
		updateRequirementsTable();
		tvRequirements.refresh();
	}

	private void removeVOFromMachine(ValidationObligation validationObligation) {
		Machine machine = currentProject.getCurrentMachine();
		machine.getValidationObligations().remove(validationObligation);
	}

	private void removeValidationObligation(ValidationObligation validationObligation) {
		removeVOFromMachine(validationObligation);
		tvRequirements.refresh();
	}

	private void showRequirement(IAbstractRequirement requirement, boolean edit) {
		if(requirement instanceof Requirement) {
			requirementEditingBox.showRequirement((Requirement) requirement, edit);
		} else if(requirement instanceof ValidationObligation) {
			voEditingBox.showValidationObligation((ValidationObligation) requirement, edit);
		}
	}

	public EditType getEditType() {
		return editTypeProperty.get();
	}

	public void clearRequirementsSelection() {
		tvRequirements.getSelectionModel().clearSelection();
	}

	public INameable getSelectedRequirement() {
		return tvRequirements.getSelectionModel().getSelectedItem().getValue();
	}

	public void refreshRequirementsTable() {
		// TODO: Replace refresh?
		this.switchMode(VOManagerStage.EditType.NONE, VOManagerStage.Mode.NONE);
		this.clearRequirementsSelection();
		this.updateRequirementsTable();
		tvRequirements.refresh();
	}

	private void resolveRefinementHierarchy(AbstractModel model) {
		if(model == null) {
			return;
		}
		if(model instanceof ClassicalBModel) {
			// TODO
		} else if(model instanceof EventBModel) {
			refinementChain.clear();
			// TODO: Implement parsing project without considering the loaded machine
			List<String> machines = ((EventBModel) model).getMachines().stream()
					.map(de.prob.model.representation.Machine::getName)
					.collect(Collectors.toList());
			for(int i = 0; i < machines.size(); i++) {
				String machine = machines.get(i);
				List<String> refinedMachines = machines.subList(0, i+1);
				refinementChain.put(machine, refinedMachines);
			}
		}
	}

}
