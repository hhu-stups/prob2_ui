package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.stream.Collectors;

@FXMLInjected
@Singleton
public class VOEditingBox extends VBox {

	@FXML
	private TextField tfVOName;

	@FXML
	private ComboBox<String> cbVOExpression;

	@FXML
	private ChoiceBox<Requirement> cbLinkRequirementChoice;

	@FXML
	private ChoiceBox<Machine> cbVOLinkMachineChoice;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private VOManagerStage voManagerStage;

	@Inject
	public VOEditingBox(final StageManager stageManager, final CurrentProject currentProject) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "vo_editing_box.fxml");
	}

	@FXML
	private void initialize() {
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

		cbVOLinkMachineChoice.valueProperty().addListener((o, from, to) -> {
			if (to == null) {
				cbVOExpression.getItems().clear();
			} else {
				cbVOExpression.getItems().setAll(to.getValidationTasks().stream()
					.map(IValidationTask::getId)
					.collect(Collectors.toList()));
			}
		});
	}

	public void resetVOEditing() {
		tfVOName.clear();
		cbVOExpression.setValue("");
		cbVOExpression.getItems().clear();
		cbLinkRequirementChoice.getItems().clear();
		cbLinkRequirementChoice.getItems().addAll(currentProject.getRequirements());
		cbVOLinkMachineChoice.getSelectionModel().clearSelection();
		voManagerStage.clearRequirementsSelection();
	}

	public void showValidationObligation(ValidationObligation validationObligation, boolean edit) {
		if(edit) {
			voManagerStage.switchMode(VOManagerStage.EditType.EDIT, VOManagerStage.Mode.VO);
		}
		if(validationObligation == null) {
			return;
		}
		tfVOName.setText(validationObligation.getId());
		cbVOExpression.setValue(validationObligation.getExpression());
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

	@FXML
	private void applyVO() {
		boolean voIsValid = !tfVOName.getText().trim().isEmpty() && cbLinkRequirementChoice.getValue() != null;
		VOManagerStage.EditType editType = voManagerStage.getEditType();
		if(voIsValid) {
			boolean nameExists = currentProject.getMachines().stream()
					.flatMap(m -> m.getValidationObligations().stream())
					.map(ValidationObligation::getId)
					.collect(Collectors.toList())
					.contains(tfVOName.getText());
			if(editType == VOManagerStage.EditType.ADD) {
				addVO(nameExists);
			} else if(editType == VOManagerStage.EditType.EDIT) {
				editVO(nameExists);
			}
			voManagerStage.refreshRequirementsTable();
		} else {
			warnNotValid();
		}
	}

	private void addVO(boolean nameExists) {
		if(nameExists) {
			warnAlreadyExists();
			return;
		}
		Machine machine = cbVOLinkMachineChoice.getSelectionModel().getSelectedItem();
		ValidationObligation validationObligation = new ValidationObligation(tfVOName.getText(), cbVOExpression.getValue(), cbLinkRequirementChoice.getValue().getName());
		machine.getValidationObligations().add(validationObligation);
	}

	private void editVO(boolean nameExists) {
		ValidationObligation validationObligation = (ValidationObligation) voManagerStage.getSelectedRequirement();
		if(nameExists &&
				validationObligation.getName().equals(tfVOName.getText()) &&
				validationObligation.getRequirement().equals(cbLinkRequirementChoice.getValue().getName()) &&
				validationObligation.getExpression().equals(cbVOExpression.getValue())) {
			warnAlreadyExists();
			return;
		}
		validationObligation.setData(tfVOName.getText(), cbVOExpression.getValue(), cbLinkRequirementChoice.getValue().getName());
	}

	private void warnNotValid() {
		stageManager.makeAlert(Alert.AlertType.INFORMATION, "vomanager.warnings.vo.notValid.header", "vomanager.warnings.vo.notValid.content").show();
	}

	private void warnAlreadyExists() {
		stageManager.makeAlert(Alert.AlertType.INFORMATION, "vomanager.warnings.vo.alreadyExists.header", "vomanager.warnings.vo.alreadyExists.content").show();
	}

	public void setVoManagerStage(VOManagerStage voManagerStage) {
		this.voManagerStage = voManagerStage;
	}

	public void updateLinkedMachines(List<Machine> machines) {
		cbVOLinkMachineChoice.getItems().clear();
		cbVOLinkMachineChoice.getItems().addAll(machines);
	}

	public Requirement getLinkedRequirement() {
		return cbLinkRequirementChoice.getValue();
	}

	public Machine getLinkedMachine() {
		return cbVOLinkMachineChoice.getValue();
	}

}
