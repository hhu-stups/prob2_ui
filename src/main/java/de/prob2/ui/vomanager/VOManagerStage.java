package de.prob2.ui.vomanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.model.representation.AbstractModel;
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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
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

import se.sawano.java.text.AlphanumericComparator;

@FXMLInjected
@Singleton
public class VOManagerStage extends Stage {
	public enum Mode {
		NONE, REQUIREMENT, VO
	}

	@FXML
	private TreeTableView<VOManagerItem> tvRequirements;

	@FXML
	private TreeTableColumn<VOManagerItem, String> requirementNameColumn;

	@FXML
	private TreeTableColumn<VOManagerItem, Checked> requirementStatusColumn;

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

	private final ObservableSet<String> relatedMachineNames;

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
		this.relatedMachineNames = FXCollections.observableSet();
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
		requirementNameColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue().getValue().getDisplayText()));
		requirementNameColumn.setComparator(new AlphanumericComparator(Locale.ROOT)::compare);
		requirementStatusColumn.setCellFactory(col -> new TreeCheckedCell<>());
		requirementStatusColumn.setCellValueFactory(features -> features.getValue().getValue().checkedProperty());

		tvRequirements.setRowFactory(table -> new TreeTableRow<VOManagerItem>() {
			@Override
			protected void updateItem(final VOManagerItem item, final boolean empty) {
				super.updateItem(item, empty);
				
				this.getStyleClass().remove("unrelated");
				if (empty) {
					this.setContextMenu(null);
				} else {
					if (item.getVo() != null) {
						final MenuItem checkItem = new MenuItem(i18n.translate("vomanager.table.requirements.contextMenu.vo.check"));
						checkItem.setOnAction(e -> checkItem(item));
						
						final MenuItem removeItem = new MenuItem(i18n.translate("vomanager.table.requirements.contextMenu.vo.remove"));
						removeItem.setOnAction(e -> removeItem(item));
						
						this.setContextMenu(new ContextMenu(checkItem, removeItem));
					} else if (item.getRequirement() != null) {
						final MenuItem checkItem = new MenuItem(i18n.translate("vomanager.table.requirements.contextMenu.requirement.check", item.getRequirement().getValidationObligations().size()));
						checkItem.setOnAction(e -> checkItem(item));
						checkItem.setDisable(item.getRequirement().getValidationObligations().isEmpty());
						
						final MenuItem removeItem = new MenuItem(i18n.translate("vomanager.table.requirements.contextMenu.requirement.remove"));
						removeItem.setOnAction(e -> removeItem(item));
						
						this.setContextMenu(new ContextMenu(checkItem, removeItem));
					} else {
						// TODO Allow checking (but not removing) machines from VO manager tree
						this.setContextMenu(null);
					}
					
					// Gray out items belonging to machines that are not in the current machine's refinement chain.
					if (item.getMachineName() != null && !relatedMachineNames.isEmpty() && !relatedMachineNames.contains(item.getMachineName())) {
						this.getStyleClass().add("unrelated");
					}
				}
			}
		});

		tvRequirements.setOnMouseClicked(e-> {
			TreeItem<VOManagerItem> treeItem = tvRequirements.getSelectionModel().getSelectedItem();
			if (treeItem != null && e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && treeItem.getChildren().isEmpty() && currentTrace.get() != null) {
				checkItem(treeItem.getValue());
			}
		});

		tvRequirements.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to != null && to.getValue() != null) {
				VOManagerItem item = to.getValue();
				if (item.getRequirement() != null) {
					requirementEditingBox.showRequirement(item.getRequirement());
					switchMode(Mode.REQUIREMENT);
				} else if (item.getVo() != null) {
					voEditingBox.showValidationObligation(item.getVo(), item.getRequirement());
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
		relatedMachineNames.addListener((InvalidationListener)o -> updateRequirementsTable());
		currentTrace.modelProperty().addListener((o, from, to) -> this.updateRelatedMachines(to));
		this.updateRelatedMachines(currentTrace.getModel());
	}

	private void checkItem(final VOManagerItem item) {
		if(item == null) {
			return;
		}
		try {
			if (item.getVo() != null) {
				voChecker.checkVO(item.getVo());
			} else if (item.getRequirement() != null) {
				voChecker.checkRequirement(item.getRequirement());
			}
		} catch (VOParseException exc) {
			voErrorHandler.handleError(this.getScene().getWindow(), exc);
		}
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

	private static TreeItem<VOManagerItem> findItemForLastSelection(final TreeItem<VOManagerItem> item, final VOManagerItem lastSelected) {
		for (final TreeItem<VOManagerItem> subItem : item.getChildren()) {
			final VOManagerItem value = subItem.getValue();
			if (
				Objects.equals(value.getRequirementName(), lastSelected.getRequirementName())
				&& Objects.equals(value.getMachineName(), lastSelected.getMachineName())
			) {
				return subItem;
			} else {
				final TreeItem<VOManagerItem> found = findItemForLastSelection(subItem, lastSelected);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	public void updateRequirementsTable() {
		VOManagerSetting setting = cbViewSetting.getSelectionModel().getSelectedItem();
		TreeItem<VOManagerItem> root = new TreeItem<>();
		if(setting == VOManagerSetting.MACHINE) {
			updateMachineRequirementsTable(root);
		} else if(setting == VOManagerSetting.REQUIREMENT) {
			updateRequirementsMachineTable(root);
		}
		for (final TreeItem<VOManagerItem> item : root.getChildren()) {
			item.setExpanded(true);
		}
		// Try to find an item in the new tree
		// that matches the currently selected item in the old tree,
		// so that the selection can be restored after the tree is updated.
		final TreeItem<VOManagerItem> lastSelectedItem = tvRequirements.getSelectionModel().getSelectedItem();
		final TreeItem<VOManagerItem> newSelectedItem;
		if (lastSelectedItem == null) {
			newSelectedItem = null;
		} else {
			newSelectedItem = findItemForLastSelection(root, lastSelectedItem.getValue());
		}
		tvRequirements.setRoot(root);
		// Restore the previous selection if possible.
		if (newSelectedItem != null) {
			tvRequirements.getSelectionModel().select(newSelectedItem);
		}
		if(currentProject.getCurrentMachine() == null) {
			return;
		}
		showFeedback();
	}

	private void updateMachineRequirementsTable(TreeItem<VOManagerItem> root) {
		for (Machine machine : currentProject.getMachines()) {
			TreeItem<VOManagerItem> machineItem = new TreeItem<>(new VOManagerItem.TopLevelMachine(currentProject.getRequirements(), machine));
			for (Requirement requirement : currentProject.getRequirements()) {
				// Show the requirement under the machine where it was introduced
				// and under any other machines that have corresponding VOs.
				final Optional<ValidationObligation> vo = requirement.getValidationObligation(machine);
					if (requirement.getIntroducedAt().equals(machine.getName()) || vo.isPresent()) {
					machineItem.getChildren().add(new TreeItem<>(new VOManagerItem.RequirementUnderMachine(requirement, machine, vo.orElse(null))));
				}
			}
			if (!machineItem.getChildren().isEmpty()) {
				root.getChildren().add(machineItem);
			}
		}
	}

	private void updateRequirementsMachineTable(TreeItem<VOManagerItem> root) {
		for(Requirement requirement : currentProject.getRequirements()) {
			TreeItem<VOManagerItem> requirementItem = new TreeItem<>(new VOManagerItem.TopLevelRequirement(requirement));
			for (final ValidationObligation vo : requirement.getValidationObligations()) {
				requirementItem.getChildren().add(new TreeItem<>(new VOManagerItem.MachineUnderRequirement(requirement, currentProject.get().getMachine(vo.getMachine()), vo)));
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
			boolean checked = true;
			for (Requirement requirement : currentProject.getRequirements()) {
				final Optional<ValidationObligation> vo = requirement.getValidationObligation(currentProject.getCurrentMachine());
				if (vo.isPresent() && vo.get().getChecked() == Checked.NOT_CHECKED) {
					taFeedback.appendText(i18n.translate("vomanager.feedback.notChecked"));
					checked = false;
					break;
				}
			}
			if(checked) {
				taFeedback.appendText(i18n.translate("vomanager.feedback.successful"));
			}
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

	private void removeItem(final VOManagerItem item) {
		if (item.getVo() != null) {
			final Requirement oldRequirement = item.getRequirement();
			final Set<ValidationObligation> updatedVos = new HashSet<>(oldRequirement.getValidationObligations());
			updatedVos.remove(item.getVo());
			final List<Requirement> predecessors = new ArrayList<>(oldRequirement.getPreviousVersions());
			predecessors.add(oldRequirement);
			final Requirement updatedRequirement = new Requirement(oldRequirement.getName(), oldRequirement.getIntroducedAt(), oldRequirement.getType(), oldRequirement.getText(), updatedVos, predecessors, oldRequirement.getParent());
			currentProject.replaceRequirement(oldRequirement, updatedRequirement);
		} else if (item.getRequirement() != null) {
			currentProject.removeRequirement(item.getRequirement());
		}
		// Machine items cannot be manually removed (they disappear when all their children are removed)
	}

	public void clearRequirementsSelection() {
		tvRequirements.getSelectionModel().clearSelection();
	}

	VOManagerItem getSelectedItem() {
		return tvRequirements.getSelectionModel().getSelectedItem() == null ? null : tvRequirements.getSelectionModel().getSelectedItem().getValue();
	}

	private void updateRelatedMachines(AbstractModel model) {
		relatedMachineNames.clear();
		if(model == null) {
			return;
		}
		relatedMachineNames.addAll(model.getGraph().getVertices());
	}

}
