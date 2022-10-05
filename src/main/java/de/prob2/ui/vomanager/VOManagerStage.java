package de.prob2.ui.vomanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

@FXMLInjected
@Singleton
public class VOManagerStage extends Stage {
	public enum Mode {
		NONE, REQUIREMENT, VO
	}

	@FXML
	private TreeTableView<INameable> tvRequirements;

	@FXML
	private TreeTableColumn<INameable, String> requirementNameColumn;

	@FXML
	private TreeTableColumn<INameable, Checked> requirementStatusColumn;

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

	private final VOFeedbackManager feedbackManager;

	private final I18n i18n;

	private final ObjectProperty<Mode> modeProperty;

	private final Map<String, List<String>> refinementChain;

	private Map<String, VOValidationFeedback> currentFeedback;

	@Inject
	public VOManagerStage(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace,
						  final VOChecker voChecker, final VOErrorHandler voErrorHandler,
						  final VOFeedbackManager feedbackManager, final I18n i18n) {
		super();
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.voChecker = voChecker;
		this.voErrorHandler = voErrorHandler;
		this.feedbackManager = feedbackManager;
		this.i18n = i18n;
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

	private static Optional<ValidationObligation> voForMachineAndRequirement(final INameable value, final INameable parentValue) {
		final Machine machine;
		final Requirement requirement;
		if (value instanceof Machine) {
			machine = (Machine)value;
			requirement = (Requirement)parentValue;
		} else if (value instanceof Requirement) {
			machine = (Machine)parentValue;
			requirement = (Requirement)value;
		} else {
			throw new AssertionError("Unhandled type in VO tree: " + value.getClass());
		}

		return requirement.getValidationObligation(machine);
	}

	private static ObservableValue<Checked> checkedPropertyConjunction(final Collection<? extends ObjectExpression<Checked>> checkeds) {
		return Bindings.createObjectBinding(() ->
			checkeds.stream()
				.map(ObservableObjectValue::get)
				.reduce(Checked::and)
				.orElse(null),
			checkeds.toArray(new Observable[0]));
	}

	private void initializeTables() {
		requirementNameColumn.setCellValueFactory(features -> {
			final TreeItem<INameable> treeItem = features.getValue();
			final INameable value = treeItem.getValue();
			if (value == null) {
				return null;
			}
			
			final StringProperty name;
			if (value instanceof Machine) {
				name = ((Machine)value).nameProperty();
			} else {
				name = new SimpleStringProperty(value.getName());
			}
			
			final INameable parentValue = treeItem.getParent().getValue();
			
			if (parentValue == null || value instanceof ValidationObligation) {
				// Top-level item - show just the name.
				return name;
			} else {
				// Second-level item - also show validation expression (if any) for this machine/requirement combination.
				return voForMachineAndRequirement(value, parentValue)
					.map(vo -> Bindings.format("%s: %s", name, vo.getExpression()))
					.orElse(name);
			}
		});
		requirementStatusColumn.setCellFactory(col -> new TreeCheckedCell<>());
		requirementStatusColumn.setCellValueFactory(features -> {
			final TreeItem<INameable> treeItem = features.getValue();
			final INameable value = treeItem.getValue();
			if (value == null) {
				return null;
			}
			
			final INameable parentValue = treeItem.getParent().getValue();
			
			if (value instanceof ValidationObligation) {
				return ((ValidationObligation)value).checkedProperty();
			} else if (parentValue != null) {
				// Second-level item - show checked status of the VO (if any) for this machine/requirement combination.
				return voForMachineAndRequirement(value, parentValue)
					.map(ValidationObligation::checkedProperty)
					.orElse(null);
			} else if (value instanceof Requirement) {
				// Top-level requirement item - show combined checked status of all VOs for this requirement.
				final Requirement requirement = (Requirement)value;
				return checkedPropertyConjunction(requirement.getValidationObligations().stream()
					.map(ValidationObligation::checkedProperty)
					.collect(Collectors.toList()));
			} else if (value instanceof Machine) {
				// Top-level machine item - show combined checked status of all VOs for this machine.
				final Machine machine = (Machine)value;
				return checkedPropertyConjunction(currentProject.requirementsProperty().stream()
					.map(req -> req.getValidationObligation(machine))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.map(ValidationObligation::checkedProperty)
					.collect(Collectors.toList()));
			} else {
				throw new AssertionError("Unhandled type in VO tree: " + value.getClass());
			}
		});

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
				if(item instanceof Requirement) {
					requirementEditingBox.showRequirement((Requirement)item);
					switchMode(Mode.REQUIREMENT);
				} else if(item instanceof ValidationObligation) {
					voEditingBox.showValidationObligation((ValidationObligation)item, getRequirementForItem(to));
					switchMode(Mode.VO);
				} else {
					switchMode(Mode.NONE);
				}
			} else {
				switchMode(Mode.NONE);
			}
		});

		currentProject.currentMachineProperty().addListener((o, from, to) -> {
			if (to != null) {
				for (final IValidationTask vt : to.getValidationTasks().values()) {
					vt.checkedProperty().addListener(o1 -> showFeedback());
				}
			}
		});
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

		requirementEditingBox.visibleProperty().bind(modeProperty.isEqualTo(Mode.REQUIREMENT));
		voEditingBox.visibleProperty().bind(modeProperty.isEqualTo(Mode.VO));
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

			if (to != null) {
				btAddVO.setDisable(to.getRequirements().isEmpty());
				updateRequirementsTable();
			} else {
				tvRequirements.setRoot(null);
			}

			switchMode(Mode.NONE);
			btAddRequirementVO.setDisable(to == null);
		};
		currentProject.addListener(projectChangeListener);
		projectChangeListener.changed(null, null, currentProject.get());
	}

	private void switchMode(Mode mode) {
		modeProperty.set(mode);
	}

	public void closeEditingBox() {
		this.switchMode(Mode.NONE);
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
					requirement.getValidationObligation(machine).ifPresent(vo ->
						requirementItem.getChildren().add(new TreeItem<>(vo))
					);
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
			for (final ValidationObligation vo : requirement.getValidationObligations()) {
				if (currentTrace.getModel() == null || (refinementChain.containsKey(vo.getMachine()) && refinementChain.get(vo.getMachine()).contains(requirement.getIntroducedAt()))) {
					TreeItem<INameable> machineItem = new TreeItem<>(currentProject.get().getMachine(vo.getMachine()));
					machineItem.getChildren().add(new TreeItem<>(vo));
					requirementItem.getChildren().add(machineItem);
				}
			}
			root.getChildren().add(requirementItem);
		}
	}

	public void showFeedback() {
		currentFeedback = feedbackManager.computeValidationFeedback(currentProject.getRequirements(), currentProject.getCurrentMachine());
		taFeedback.clear();
		if(currentFeedback == null) {
			return;
		}
		if(currentFeedback.isEmpty()) {
			for (Requirement requirement : currentProject.getRequirements()) {
				final Optional<ValidationObligation> vo = requirement.getValidationObligation(currentProject.getCurrentMachine());
				if (vo.isPresent() && vo.get().getChecked() == Checked.NOT_CHECKED) {
					taFeedback.appendText(i18n.translate("vomanager.feedback.notChecked"));
					return;
				}
			}
			taFeedback.appendText(i18n.translate("vomanager.feedback.successful"));
		} else {
			for (VOValidationFeedback validationFeedback : currentFeedback.values()) {
				taFeedback.appendText(i18n.translate("vomanager.feedback.failingVO", validationFeedback.getRequirement()));
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
		switchMode(Mode.REQUIREMENT);
	}

	@FXML
	public void addVO() {
		voEditingBox.resetVOEditing();
		switchMode(Mode.VO);
	}

	private static Machine getMachineForItem(final TreeItem<INameable> treeItem) {
		final TreeItem<INameable> parentItem = treeItem.getParent();
		if (parentItem.getValue() instanceof Machine) {
			return (Machine) parentItem.getValue();
		} else {
			return (Machine) parentItem.getParent().getValue();
		}
	}

	private static Requirement getRequirementForItem(final TreeItem<INameable> treeItem) {
		final TreeItem<INameable> parentItem = treeItem.getParent();
		if (parentItem.getValue() instanceof Requirement) {
			return (Requirement) parentItem.getValue();
		} else {
			return (Requirement) parentItem.getParent().getValue();
		}
	}

	private void removeItem(TreeItem<INameable> item) {
		if (item.getValue() instanceof Requirement) {
			currentProject.removeRequirement((Requirement)item.getValue());
		} else if (item.getValue() instanceof ValidationObligation) {
			final Requirement oldRequirement = getRequirementForItem(item);
			final Set<ValidationObligation> updatedVos = new HashSet<>(oldRequirement.getValidationObligations());
			updatedVos.remove((ValidationObligation)item.getValue());
			final List<Requirement> predecessors = new ArrayList<>(oldRequirement.getPreviousVersions());
			predecessors.add(oldRequirement);
			final Requirement updatedRequirement = new Requirement(oldRequirement.getName(), oldRequirement.getIntroducedAt(), oldRequirement.getType(), oldRequirement.getText(), updatedVos, predecessors, oldRequirement.getParent());
			currentProject.replaceRequirement(oldRequirement, updatedRequirement);
		}
		// Machine items cannot be manually removed (they disappear when all their children are removed)
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
		final Requirement oldRequirement = getRequirementForItem(treeItem);
		final Set<ValidationObligation> updatedVos = new HashSet<>(oldRequirement.getValidationObligations());
		updatedVos.remove(oldVo);
		updatedVos.add(newVo);

		final List<Requirement> predecessors = new ArrayList<>(oldRequirement.getPreviousVersions());
		predecessors.add(oldRequirement);
		final Requirement updatedRequirement = new Requirement(oldRequirement.getName(), oldRequirement.getIntroducedAt(), oldRequirement.getType(), oldRequirement.getText(), updatedVos, predecessors, oldRequirement.getParent());
		currentProject.replaceRequirement(oldRequirement, updatedRequirement);
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
