package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;
import java.util.stream.Collectors;

@FXMLInjected
@Singleton
public class RequirementsEditingBox extends VBox {

	@FXML
	private TextField tfName;

	@FXML
	private TextArea taRequirement;

	@FXML
	private ChoiceBox<RequirementType> cbRequirementChoice;

	@FXML
	private Button applyButton;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final VOManager voManager;

	private final ResourceBundle bundle;

	private VOManagerStage voManagerStage;

	@Inject
	public RequirementsEditingBox(final StageManager stageManager, final CurrentProject currentProject, final VOManager voManager,
								  final ResourceBundle bundle) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.voManager = voManager;
		this.bundle = bundle;
		stageManager.loadFXML(this, "requirements_editing_box.fxml");
	}

	@FXML
	private void initialize() {
		applyButton.visibleProperty().bind(cbRequirementChoice.getSelectionModel().selectedItemProperty().isNotNull());
	}

	@FXML
	public void applyRequirement() {
		if(voManager.requirementIsValid(tfName.getText(), taRequirement.getText())) {
			boolean nameExists = currentProject.getRequirements().stream()
					.map(Requirement::getName)
					.collect(Collectors.toList())
					.contains(tfName.getText());
			VOManagerStage.EditType editType = voManagerStage.getEditType();
			if(editType == VOManagerStage.EditType.ADD) {
				addRequirement(nameExists);
			} else if(editType == VOManagerStage.EditType.EDIT) {
				editRequirement(nameExists);
			}
			updateVOManagerStage();
		} else {
			warnNotValid();
		}
	}

	private void addRequirement(boolean nameExists) {
		if(nameExists) {
			warnAlreadyExists();
			return;
		}
		currentProject.getRequirements().add(new Requirement(tfName.getText(), cbRequirementChoice.getValue(), taRequirement.getText()));
	}

	private void editRequirement(boolean nameExists) {
		Requirement requirement = (Requirement) voManagerStage.getSelectedRequirement();
		if(nameExists && !requirement.getName().equals(tfName.getText())) {
			warnAlreadyExists();
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

	private void updateVOManagerStage() {
		// TODO: Replace refresh?
		voManagerStage.switchMode(VOManagerStage.EditType.NONE, VOManagerStage.Mode.NONE);
		voManagerStage.clearRequirementsSelection();
		voManagerStage.updateRequirementsTable();
		voManagerStage.refreshRequirementsTable();
	}

	public void resetRequirementEditing() {
		cbRequirementChoice.getSelectionModel().clearSelection();
		taRequirement.clear();
		tfName.clear();
		voManagerStage.clearRequirementsSelection();
	}

	public void showRequirement(Requirement requirement, boolean edit) {
		if(edit) {
			voManagerStage.switchMode(VOManagerStage.EditType.EDIT, VOManagerStage.Mode.REQUIREMENT);
		}
		if(requirement == null) {
			return;
		}
		tfName.setText(requirement.getName());
		cbRequirementChoice.getSelectionModel().select(requirement.getType());
		taRequirement.setText(requirement.getText());
	}

	private void warnNotValid() {
		stageManager.makeAlert(Alert.AlertType.INFORMATION, bundle.getString("vomanager.warnings.requirement.notValid.header"), bundle.getString("vomanager.warnings.requirement.notValid.content")).show();
	}

	private void warnAlreadyExists() {
		stageManager.makeAlert(Alert.AlertType.INFORMATION, bundle.getString("vomanager.warnings.requirement.alreadyExists.header"), bundle.getString("vomanager.warnings.requirement.alreadyExists.content")).show();
	}

	public void setVoManagerStage(VOManagerStage voManagerStage) {
		this.voManagerStage = voManagerStage;
	}
}
