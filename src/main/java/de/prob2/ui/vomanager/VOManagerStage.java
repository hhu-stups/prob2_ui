package de.prob2.ui.vomanager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.StateSpace;
import de.prob.voparser.VOParseException;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.TreeCheckedCell;
import de.prob2.ui.vomanager.feedback.VOFeedbackManager;
import de.prob2.ui.vomanager.feedback.VOValidationFeedback;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
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

	public enum EditType {
		NONE, MODIFY;
	}

	public enum Mode {
		NONE, REQUIREMENT, VO
	}

	@FXML
	private TreeTableView<INameable> tvRequirements;

	@FXML
	private TreeTableColumn<INameable, Checked> requirementStatusColumn;

	@FXML
	private TreeTableColumn<INameable, String> requirementNameColumn;

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

	@FXML
	private TextArea taFeedback;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final VOChecker voChecker;

	private final VOErrorHandler voErrorHandler;

	private final RequirementHandler requirementHandler;

	private final VOFeedbackManager feedbackManager;

	private final I18n i18n;

	private final ObjectProperty<EditType> editTypeProperty;

	private final ObjectProperty<Mode> modeProperty;

	private final Map<String, List<String>> refinementChain;

	private Map<String, VOValidationFeedback> currentFeedback;

	@Inject
	public VOManagerStage(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace,
						  final VOChecker voChecker, final VOErrorHandler voErrorHandler, final RequirementHandler requirementHandler,
						  final VOFeedbackManager feedbackManager, final I18n i18n) {
		super();
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.voChecker = voChecker;
		this.voErrorHandler = voErrorHandler;
		this.requirementHandler = requirementHandler;
		this.feedbackManager = feedbackManager;
		this.i18n = i18n;
		this.editTypeProperty = new SimpleObjectProperty<>(EditType.NONE);
		this.modeProperty = new SimpleObjectProperty<>(Mode.NONE);
		this.refinementChain = new HashMap<>();
		stageManager.loadFXML(this, "vo_manager_view.fxml", this.getClass().getName());
	}

	@FXML
	public void initialize() {
		initializeTables();
		initializeEditingBoxes();
		initializeChoiceBoxes();
		initializeListenerOnProjectChange();
	}

	private void initializeTables() {
		requirementStatusColumn.setCellFactory(col -> new TreeCheckedCell<>());
		requirementStatusColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("checked"));
		requirementNameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));

		tvRequirements.setRowFactory(table -> {
			final TreeTableRow<INameable> row = new TreeTableRow<>();
			if(row.getItem() instanceof Machine) {
				return row;
			}

			MenuItem checkItem = new MenuItem(i18n.translate("common.buttons.check"));
			checkItem.setOnAction(e -> checkItem(row.getTreeItem()));

			MenuItem removeItem = new MenuItem(i18n.translate("common.buttons.remove"));
			removeItem.setOnAction(e -> removeItem(row.getTreeItem()));

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(checkItem, removeItem)));
			return row;
		});

		tvRequirements.setOnMouseClicked(e-> {
			TreeItem<INameable> treeItem = tvRequirements.getSelectionModel().getSelectedItem();
			if (treeItem != null && e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && treeItem.getChildren().isEmpty() && currentTrace.get() != null) {
				checkItem(treeItem);
			}
		});

		tvRequirements.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to != null && to.getValue() != null) {
				INameable item = to.getValue();
				if(item instanceof Requirement || item instanceof ValidationObligation) {
					showRequirement((IAbstractRequirement) item);
				} else {
					switchMode(EditType.NONE, Mode.NONE);
				}
			} else {
				switchMode(EditType.NONE, Mode.NONE);
			}
		});

		currentProject.addListener((observable, from, to) -> voChecker.deregisterAllTasks());
		ChangeListener<Machine> listener = (observable, from, to) -> updateOnMachine(to);
		currentProject.currentMachineProperty().addListener(listener);
		updateOnMachine(currentProject.getCurrentMachine());
		currentTrace.stateSpaceProperty().addListener((o, from, to) -> initializeRefinementHierarchy(to));
		initializeRefinementHierarchy(currentTrace.getStateSpace());
	}

	private void checkItem(TreeItem<INameable> treeItem) {
		INameable item = treeItem == null ? null : treeItem.getValue();
		if(item == null) {
			return;
		}
		try {
			if (item instanceof Requirement) {
				VOManagerSetting setting = cbViewSetting.getSelectionModel().getSelectedItem();
				Machine selectedMachine = setting == VOManagerSetting.MACHINE ? (Machine) treeItem.getParent().getValue() : null;
				voChecker.checkRequirement((Requirement) item, selectedMachine, setting);
			} else if (item instanceof ValidationObligation) {
				voChecker.checkVO(getMachineForItem(treeItem), (ValidationObligation) item);
			}
		} catch (VOParseException exc) {
			voErrorHandler.handleError(this.getScene().getWindow(), exc);
		}
	}

	private void updateOnMachine(Machine machine) {
		voChecker.deregisterAllTasks();
		if(machine != null) {
			updateVTsFromMachine(machine);
			updateVOsFromMachine(machine);
		}
	}

	private void updateVTsFromMachine(Machine machine) {
		for(String key : machine.getValidationTasks().keySet()) {
			IValidationTask validationTask = machine.getValidationTasks().get(key);
			validationTask.checkedProperty().addListener((observable, from, to) -> showFeedback());
			voChecker.registerTask(key, null); // TODO
		}
		machine.getValidationTasks().addListener((MapChangeListener<? super String, ? super IValidationTask>) o -> {
			if(o.wasRemoved()) {
				voChecker.registerTask(o.getKey(), null); // TODO
			}
			if(o.wasAdded()) {
				voChecker.deregisterTask(o.getKey());
			}
		});
	}

	private void updateVOsFromMachine(Machine machine) {
		for(ValidationObligation vo : machine.getValidationObligations()) {
			try {
				voChecker.parseVO(machine, vo);
			} catch (VOParseException e) {
				voErrorHandler.handleError(this, e);
			}
		}
	}

	private void initializeRefinementHierarchy(StateSpace stateSpace) {
		if(stateSpace == null) {
			return;
		}
		this.resolveRefinementHierarchy(stateSpace.getModel());
		updateRequirementsTable();
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
		final InvalidationListener updateListener = o -> this.updateRequirementsTable();
		final ChangeListener<Project> projectChangeListener = (observable, from, to) -> {
			btAddVO.disableProperty().unbind();
			final List<Machine> machines = to == null ? Collections.emptyList() : to.getMachines();
			requirementEditingBox.updateLinkedMachines(machines);
			voEditingBox.updateLinkedMachines(machines);

			if(from != null) {
				for (Requirement requirement : from.getRequirements()) {
					requirementHandler.resetListeners(from, requirement);
				}
				from.requirementsProperty().removeListener(updateListener);
				for (final Machine machine : from.getMachines()) {
					machine.validationObligationsProperty().removeListener(updateListener);
				}
			}

			if (to != null) {
				btAddVO.disableProperty().bind(to.requirementsProperty().emptyProperty());

				for(Requirement requirement : to.getRequirements()) {
					// TODO: Distinguish between two views for tvRequirements
					requirementHandler.initListeners(to, null, requirement, VOManagerSetting.REQUIREMENT);
				}

				to.requirementsProperty().addListener(updateListener);
				for (final Machine machine : to.getMachines()) {
					machine.validationObligationsProperty().addListener(updateListener);
				}
				updateRequirementsTable();
			} else {
				tvRequirements.setRoot(null);
			}

			switchMode(EditType.NONE, Mode.NONE);
			btAddRequirementVO.setDisable(to == null);
		};
		currentProject.addListener(projectChangeListener);
		projectChangeListener.changed(null, null, currentProject.get());
	}

	public void switchMode(EditType editType, Mode mode) {
		editTypeProperty.set(editType);
		modeProperty.set(mode);
	}

	public void closeEditingBox() {
		this.switchMode(EditType.NONE, Mode.NONE);
	}

	public void updateRequirementsTable() {
		VOManagerSetting setting = cbViewSetting.getSelectionModel().getSelectedItem();
		TreeItem<INameable> root = new TreeItem<>();
		if(setting == VOManagerSetting.MACHINE) {
			updateMachineRequirementsTable(root);
		} else if(setting == VOManagerSetting.REQUIREMENT) {
			updateRequirementsMachineTable(root);
		}
		for (final TreeItem<INameable> item : root.getChildren()) {
			item.setExpanded(true);
		}
		tvRequirements.setRoot(root);
		if(currentProject.getCurrentMachine() == null) {
			return;
		}
		showFeedback();
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

	public void showFeedback() {
		currentFeedback = feedbackManager.computeValidationFeedback(currentProject.getCurrentMachine().getValidationObligations());
		taFeedback.clear();
		if(currentFeedback == null) {
			return;
		}
		if(currentFeedback.isEmpty()) {
			for(ValidationObligation validationObligation : currentProject.getCurrentMachine().getValidationObligations()) {
				if(validationObligation.getChecked() == Checked.NOT_CHECKED) {
					taFeedback.appendText(i18n.translate("vomanager.feedback.notChecked"));
					return;
				}
			}
			taFeedback.appendText(i18n.translate("vomanager.feedback.successful"));
		} else {
			for (String vo : currentFeedback.keySet()) {
				VOValidationFeedback validationFeedback = currentFeedback.get(vo);
				taFeedback.appendText(i18n.translate("vomanager.feedback.failingVO", validationFeedback.getRequirement(), vo));
				taFeedback.appendText("\n");
				taFeedback.appendText(i18n.translate("vomanager.feedback.dependentVOs", validationFeedback.getDependentVOs().toString()));
				taFeedback.appendText("\n");
				taFeedback.appendText(i18n.translate("vomanager.feedback.dependentVTs", validationFeedback.getDependentVTs().toString()));
				taFeedback.appendText("\n");
				taFeedback.appendText(i18n.translate("vomanager.feedback.dependentRequirements", validationFeedback.getDependentRequirements().toString()));
				taFeedback.appendText("\n");
				taFeedback.appendText("\n");
			}
		}
	}

	@FXML
	public void addRequirement() {
		requirementEditingBox.resetRequirementEditing();
		switchMode(EditType.MODIFY, Mode.REQUIREMENT);
	}

	@FXML
	public void addVO() {
		voEditingBox.resetVOEditing();
		switchMode(EditType.MODIFY, Mode.VO);
	}

	private static Machine getMachineForItem(final TreeItem<INameable> treeItem) {
		final TreeItem<INameable> parentItem = treeItem.getParent();
		if (parentItem.getValue() instanceof Machine) {
			return (Machine) parentItem.getValue();
		} else {
			return (Machine) parentItem.getParent().getValue();
		}
	}

	private void removeItem(TreeItem<INameable> item) {
		if (item.getValue() instanceof Requirement) {
			currentProject.removeRequirement((Requirement)item.getValue());
		} else if (item.getValue() instanceof ValidationObligation) {
			getMachineForItem(item).getValidationObligations().remove((ValidationObligation)item.getValue());
		}
		// Machine items cannot be manually removed (they disappear when all their children are removed)
	}

	private void showRequirement(IAbstractRequirement requirement) {
		if(requirement instanceof Requirement) {
			requirementEditingBox.showRequirement((Requirement) requirement, true);
		} else if(requirement instanceof ValidationObligation) {
			voEditingBox.showValidationObligation((ValidationObligation) requirement, true);
		}
	}

	public EditType getEditType() {
		return editTypeProperty.get();
	}

	public void clearRequirementsSelection() {
		tvRequirements.getSelectionModel().clearSelection();
	}

	public INameable getSelectedRequirement() {
		return tvRequirements.getSelectionModel().getSelectedItem() == null ? null : tvRequirements.getSelectionModel().getSelectedItem().getValue();
	}

	public void replaceCurrentValidationObligation(final ValidationObligation newVo) {
		final TreeItem<INameable> treeItem = tvRequirements.getSelectionModel().getSelectedItem();
		final ValidationObligation oldVo = (ValidationObligation)treeItem.getValue();
		final Machine machine = getMachineForItem(treeItem);
		machine.getValidationObligations().set(machine.getValidationObligations().indexOf(oldVo), newVo);
	}

	public ValidationObligation getCurrentValidationObligation() {
		final TreeItem<INameable> treeItem = tvRequirements.getSelectionModel().getSelectedItem();
		return (ValidationObligation) treeItem.getValue();
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
