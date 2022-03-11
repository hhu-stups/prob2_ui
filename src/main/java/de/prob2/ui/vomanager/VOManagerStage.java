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
import de.prob2.ui.verifications.TreeCheckedCell;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VOManagerStage extends Stage {

	private static final List<ValidationTechnique> tasks = Arrays.asList(ValidationTechnique.MODEL_CHECKING, ValidationTechnique.LTL_MODEL_CHECKING, ValidationTechnique.SYMBOLIC_MODEL_CHECKING,
			ValidationTechnique.TRACE_REPLAY, ValidationTechnique.SIMULATION);

	private enum EditType {
		NONE, ADD, EDIT;
	}

	private enum EditMode {
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
	private TextArea taVOPredicate;

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

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final VOManager voManager;

	private final VOChecker voChecker;

	private final VOTaskCreator voTaskCreator;

	private final ObjectProperty<EditType> editTypeProperty;

	private final ObjectProperty<EditMode> editModeProperty;

	@Inject
	public VOManagerStage(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace, final Injector injector,
			final VOManager voManager, final VOChecker voChecker, final VOTaskCreator voTaskCreator) {
		super();
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.voManager = voManager;
		this.voChecker = voChecker;
		this.voTaskCreator = voTaskCreator;
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
		specificationColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("configuration"));

		vtStatusColumn.setCellFactory(col -> new CheckedCell<>());
		vtStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));

		vtNameColumn.setCellValueFactory(new PropertyValueFactory<>("prefix"));
		vtParametersColumn.setCellValueFactory(new PropertyValueFactory<>("parameters"));

		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			btAddVO.disableProperty().unbind();

			tvValidationTasks.itemsProperty().unbind();
			if(to != null) {
				//voManager.synchronizeMachine(to);
				tvValidationTasks.itemsProperty().bind(to.validationTasksProperty());
				btAddVO.disableProperty().bind(to.requirementsProperty().emptyProperty());
			}
			updateRoot();
			editTypeProperty.set(EditType.NONE);
			editModeProperty.set(EditMode.NONE);
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());

		requirementEditingBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> editTypeProperty.get() != EditType.NONE && editModeProperty.get() == EditMode.REQUIREMENT, editTypeProperty, editModeProperty));
		voEditingBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> editTypeProperty.get() != EditType.NONE && editModeProperty.get() == EditMode.VO, editTypeProperty, editModeProperty));
		vtEditingBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> editTypeProperty.get() != EditType.NONE && editModeProperty.get() == EditMode.VT, editTypeProperty, editModeProperty));

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

		editModeProperty.addListener((observable, from, to) -> {
			if(to == EditMode.VT) {
				btAddVT.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.TIMES_CIRCLE));
				btAddVT.setTooltip(new Tooltip());
			} else {
				btAddVT.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.PLUS_CIRCLE));
				btAddVT.setTooltip(new Tooltip());
			}
		});



		tvRequirements.setRowFactory(table -> {
			final TreeTableRow<IAbstractRequirement> row = new TreeTableRow<>();

			//Menu linkItem = new Menu("Link to Validation Obligation");

			//row.itemProperty().addListener((observable, from, to) -> {
			//	final InvalidationListener linkingListener = o -> voManager.showPossibleLinkings(linkItem, to);
			//	voManager.updateLinkingListener(linkItem, from, to, linkingListener);
			//});

			MenuItem checkItem = new MenuItem("Check");
			checkItem.setOnAction(e -> {
				IAbstractRequirement item = row.getItem();
				if(item instanceof Requirement) {
					Requirement requirement = (Requirement) item;
					requirement.getValidationObligations().forEach(voChecker::check);
				} else if(item instanceof ValidationObligation) {
					ValidationObligation validationObligation = (ValidationObligation) item;
					voChecker.check(validationObligation);
				}
			});

			MenuItem removeItem = new MenuItem("Remove");
			removeItem.setOnAction(e -> {
				IAbstractRequirement item = row.getItem();
				if(item instanceof Requirement) {
					removeRequirement();
				} else if(item instanceof ValidationObligation) {
					removeValidationObligation();
				}
			});

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(checkItem, removeItem)));
			return row;
		});

		tvRequirements.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to != null && to.getValue() != null) {
				IAbstractRequirement item = to.getValue();
				if(item instanceof Requirement) {
					Requirement requirement = (Requirement) item;
					editTypeProperty.set(EditType.EDIT);
					editModeProperty.set(EditMode.REQUIREMENT);
					showRequirement(requirement);
				} else if(item instanceof ValidationObligation) {
					ValidationObligation validationObligation = (ValidationObligation) item;
					editTypeProperty.set(EditType.EDIT);
					editModeProperty.set(EditMode.VO);
					showValidationObligation(validationObligation);
				}
			} else {
				editTypeProperty.set(EditType.NONE);
				editModeProperty.set(EditMode.NONE);
			}
		});

		tvRequirements.setOnMouseClicked(e-> {
			TreeItem<IAbstractRequirement> treeItem = tvRequirements.getSelectionModel().getSelectedItem();
			IAbstractRequirement abstractRequirement = treeItem == null ? null : treeItem.getValue();

			if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && abstractRequirement != null && currentTrace.get() != null) {
				if(abstractRequirement instanceof Requirement) {
					Requirement requirement = (Requirement) abstractRequirement;
					requirement.getValidationObligations().forEach(voChecker::check);
				} else if(abstractRequirement instanceof ValidationObligation) {
					ValidationObligation validationObligation = (ValidationObligation) abstractRequirement;
					voChecker.check(validationObligation);
				}
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

		tvValidationTasks.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to != null) {
				editTypeProperty.set(EditType.EDIT);
				editModeProperty.set(EditMode.VT);
				showValidationTask(to);
			} else {
				editTypeProperty.set(EditType.NONE);
				editModeProperty.set(EditMode.NONE);
			}
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

	@FXML
	public void addRequirement() {
		cbRequirementChoice.getSelectionModel().clearSelection();
		taRequirement.clear();
		tfName.clear();
		editTypeProperty.set(EditType.ADD);
		editModeProperty.set(EditMode.REQUIREMENT);
		tvRequirements.getSelectionModel().clearSelection();
	}

	@FXML
	public void addVO() {
		Machine machine = currentProject.getCurrentMachine();
		tfVOName.clear();
		taVOPredicate.clear();
		cbLinkRequirementChoice.getItems().clear();
		cbLinkRequirementChoice.getItems().addAll(machine.getRequirements());
		editTypeProperty.set(EditType.ADD);
		editModeProperty.set(EditMode.VO);
		tvRequirements.getSelectionModel().clearSelection();
	}

	@FXML
	public void applyRequirement() {
		EditType editType = editTypeProperty.get();
		boolean requirementIsValid = requirementIsValid(tfName.getText(), taRequirement.getText());

		if(requirementIsValid) {
			Requirement requirement = null;
			Machine machine = currentProject.getCurrentMachine();
			boolean nameExists = machine.getRequirements().stream()
					.map(Requirement::getName)
					.collect(Collectors.toList())
					.contains(tfName.getText());
			if(editType == EditType.ADD) {
				if(nameExists) {
					return;
				}
				requirement = new Requirement(tfName.getText(), cbRequirementChoice.getValue(), taRequirement.getText(), Collections.emptyList());
				machine.getRequirements().add(requirement);
			} else if(editType == EditType.EDIT) {
				requirement = (Requirement) tvRequirements.getSelectionModel().getSelectedItem().getValue();
				if(nameExists && !requirement.getName().equals(tfName.getText())) {
					return;
				}
				requirement.setName(tfName.getText());
				requirement.setType(cbRequirementChoice.getValue());
				requirement.setText(taRequirement.getText());
				for(ValidationObligation validationObligation : requirement.getValidationObligations()) {
					validationObligation.setRequirement(requirement.getName());
				}
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

	private void removeRequirement() {
		Requirement requirement = (Requirement) tvRequirements.getSelectionModel().getSelectedItem().getValue();
		currentProject.getCurrentMachine().getRequirements().remove(requirement);
		updateRoot();
		tvRequirements.refresh();
	}

	private void removeValidationObligation() {
		ValidationObligation validationObligation = (ValidationObligation) tvRequirements.getSelectionModel().getSelectedItem().getValue();
		Machine machine = currentProject.getCurrentMachine();
		String requirementID = validationObligation.getRequirement();
		for(Requirement requirement : machine.getRequirements()) {
			if(requirement.getName().equals(requirementID)) {
				requirement.removeValidationObligation(validationObligation);
				break;
			}
		}
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

	private void showValidationObligation(ValidationObligation validationObligation) {
		if(validationObligation == null) {
			return;
		}
		tfVOName.setText(validationObligation.getId());
		taVOPredicate.setText(validationObligation.getPredicate());
		Machine machine = currentProject.getCurrentMachine();
		Requirement requirement = machine.getRequirements().stream()
				.filter(req -> req.getName().equals(validationObligation.getRequirement()))
				.collect(Collectors.toList()).get(0);
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

	@FXML
	private void applyVO() {
		boolean voIsValid = voIsValid(tfVOName.getText(), cbLinkRequirementChoice.getValue());
		EditType editType = editTypeProperty.get();
		if(voIsValid) {
			ValidationObligation validationObligation;
			Machine machine = currentProject.getCurrentMachine();
			boolean nameExists = machine.getValidationObligations().stream()
					.map(ValidationObligation::getId)
					.collect(Collectors.toList())
					.contains(tfVOName.getText());
			if(editType == EditType.ADD) {
				if(nameExists) {
					return;
				}
				validationObligation = new ValidationObligation(tfVOName.getText(), taVOPredicate.getText(), cbLinkRequirementChoice.getValue().getName());
				machine.getValidationObligations().add(validationObligation);
				for(TreeItem<IAbstractRequirement> treeItem : tvRequirements.getRoot().getChildren()) {
					Requirement requirement = (Requirement) treeItem.getValue();
					if(requirement.equals(cbLinkRequirementChoice.getValue())) {
						requirement.addValidationObligation(validationObligation);
						break;
					}
				}
			} else if(editType == EditType.EDIT) {
				validationObligation = (ValidationObligation) tvRequirements.getSelectionModel().getSelectedItem().getValue();
				if(nameExists && !validationObligation.getName().equals(tfVOName.getText())) {
					return;
				}
				validationObligation.setId(tfVOName.getText());
				validationObligation.setPredicate(taVOPredicate.getText());
				validationObligation.setRequirement(cbLinkRequirementChoice.getValue().getName());
			}

			editTypeProperty.set(EditType.NONE);
			editModeProperty.set(EditMode.NONE);
			tvRequirements.getSelectionModel().clearSelection();
			updateRoot();
			tvRequirements.refresh();
		} else {
			// TODO: Show message
		}
	}

	@FXML
	private void addVT() {
		if(editTypeProperty.get() == EditType.NONE || editModeProperty.get() != EditMode.VT) {
			tfVTName.clear();
			cbValidationTechniqueChoice.getSelectionModel().clearSelection();
			cbTaskChoice.getItems().clear();
			editTypeProperty.set(EditType.ADD);
			editModeProperty.set(EditMode.VT);
		} else {
			editTypeProperty.set(EditType.NONE);
			editModeProperty.set(EditMode.NONE);
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
					return;
				}
				task.setId(tfVTName.getText());
				machine.getValidationTasks().add(task);
			} else if(editType == EditType.EDIT) {
				ValidationTask currentTask = tvValidationTasks.getSelectionModel().getSelectedItem();
				if(nameExists && !currentTask.getId().equals(tfVTName.getText())) {
					return;
				}
				currentTask.setId(tfVTName.getText());
				currentTask.setExecutable(task.getExecutable());
				currentTask.setContext("machine"); //TODO:
				currentTask.setItem(task.getItem());
				currentTask.setParameters(voTaskCreator.extractParameters(task.getItem()));
			}

			editTypeProperty.set(EditType.NONE);
			editModeProperty.set(EditMode.NONE);
			tvValidationTasks.getSelectionModel().clearSelection();
			tvValidationTasks.refresh();
		} else {
			// TODO: Show message
		}

	}
}
