package de.prob2.ui.vomanager;

import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.IExecutableItem;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

@FXMLInjected
@Singleton
public class VTEditingBox extends VBox {

	@FXML
	private ChoiceBox<ValidationTechnique> cbValidationTechniqueChoice;

	@FXML
	private ChoiceBox<Machine> cbVTLinkMachineChoice;

	@FXML
	private ChoiceBox<IExecutableItem> cbTaskChoice;

	@FXML
	private TextField tfVTName;

	@FXML
	private Button applyVTButton;

	@FXML
	private VBox validationTaskBox;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final VOManager voManager;

	private VOManagerStage voManagerStage;

	@Inject
	public VTEditingBox(final StageManager stageManager, final CurrentProject currentProject, final VOManager voManager) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.voManager = voManager;
		stageManager.loadFXML(this, "vt_editing_box.fxml");
	}

	@FXML
	private void initialize() {
		cbTaskChoice.setConverter(new StringConverter<IExecutableItem>() {
			@Override
			public String toString(IExecutableItem object) {
				if(object == null) {
					return "";
				}
				final String parameters = voManager.extractParameters(object);
				if(object instanceof SimulationItem) {
					SimulationItem simulationItem = (SimulationItem)object;
					return String.format("%s:\n%s", simulationItem.getSimulationModel().getPath(), parameters);
				}
				return parameters;
			}

			@Override
			public IExecutableItem fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String to IExecutableItem not supported");
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

		validationTaskBox.visibleProperty().bind(cbValidationTechniqueChoice.getSelectionModel().selectedItemProperty().isNotNull());
		applyVTButton.visibleProperty().bind(cbValidationTechniqueChoice.getSelectionModel().selectedItemProperty().isNotNull().and(cbTaskChoice.getSelectionModel().selectedItemProperty().isNotNull()));
	}

	public void showValidationTask(ValidationTask validationTask) {
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
		cbTaskChoice.getSelectionModel().select(validationTask.getExecutable());
	}

	public void resetVTEditing() {
		tfVTName.clear();
		cbValidationTechniqueChoice.getSelectionModel().clearSelection();
		cbTaskChoice.getItems().clear();
	}

	@FXML
	private void applyVT() {
		boolean taskIsValid = voManager.taskIsValid(tfVTName.getText());
		VOManagerStage.EditType editType = voManagerStage.getEditType();
		if(taskIsValid) {
			IExecutableItem executable = cbTaskChoice.getSelectionModel().getSelectedItem();
			if(executable == null) {
				return;
			}
			Machine machine = cbVTLinkMachineChoice.getSelectionModel().getSelectedItem();
			boolean nameExists = machine.getValidationTasks().stream()
					.map(ValidationTask::getId)
					.collect(Collectors.toList())
					.contains(tfVTName.getText());
			if(editType == VOManagerStage.EditType.ADD) {
				addVT(nameExists);
			} else if(editType == VOManagerStage.EditType.EDIT) {
				editVT(nameExists);
			}
			voManagerStage.refreshVTTable();
		} else {
			warnNotValid();
		}
	}

	private void addVT(boolean nameExists) {
		if(nameExists) {
			warnAlreadyExists();
			return;
		}
		IExecutableItem executable = cbTaskChoice.getSelectionModel().getSelectedItem();
		Machine machine = cbVTLinkMachineChoice.getSelectionModel().getSelectedItem();
		ValidationTask task = new ValidationTask(tfVTName.getText(), machine.getName(), cbValidationTechniqueChoice.getValue(), voManager.extractParameters(executable), executable);
		machine.getValidationTasks().add(task);
	}

	private void editVT(boolean nameExists) {
		INameable taskItem = voManagerStage.getSelectedVT();
		if(taskItem instanceof Machine) {
			return;
		}
		ValidationTask currentTask = (ValidationTask) taskItem;
		if(nameExists && !currentTask.getId().equals(tfVTName.getText())) {
			warnAlreadyExists();
			return;
		}
		IExecutableItem executable = cbTaskChoice.getSelectionModel().getSelectedItem();
		Machine machine = cbVTLinkMachineChoice.getSelectionModel().getSelectedItem();
		currentTask.setData(tfVTName.getText(), executable, machine.getName(), voManager.extractParameters(executable));
	}

	private void updateTaskChoice(ValidationTechnique validationTechnique) {
		cbTaskChoice.getItems().clear();
		Machine machine = cbVTLinkMachineChoice.getSelectionModel().getSelectedItem();
		if(validationTechnique == null || machine == null) {
			return;
		}
		cbTaskChoice.getItems().addAll(voManager.getExecutables(machine, validationTechnique));
	}

	private void warnNotValid() {
		stageManager.makeAlert(Alert.AlertType.INFORMATION, "vomanager.warnings.vt.notValid.header", "vomanager.warnings.vt.notValid.content").show();
	}

	private void warnAlreadyExists() {
		stageManager.makeAlert(Alert.AlertType.INFORMATION, "vomanager.warnings.vt.alreadyExists.header", "vomanager.warnings.vt.alreadyExists.content").show();
	}

	public void setVoManagerStage(VOManagerStage voManagerStage) {
		this.voManagerStage = voManagerStage;
	}

	public void updateLinkedMachines(List<Machine> machines) {
		cbVTLinkMachineChoice.getItems().clear();
		cbVTLinkMachineChoice.getItems().addAll(machines);
	}

}
