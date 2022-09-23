package de.prob2.ui.vomanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
	private ChoiceBox<String> cbRequirementLinkMachineChoice;

	@FXML
	private Button applyButton;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private VOManagerStage voManagerStage;

	private final ObservableList<String> linkedMachineNames;

	@Inject
	public RequirementsEditingBox(final StageManager stageManager, final CurrentProject currentProject) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;

		this.linkedMachineNames = FXCollections.observableArrayList();

		stageManager.loadFXML(this, "requirements_editing_box.fxml");
	}

	@FXML
	private void initialize() {
		cbRequirementLinkMachineChoice.setItems(linkedMachineNames);
		applyButton.visibleProperty().bind(cbRequirementChoice.getSelectionModel().selectedItemProperty().isNotNull());
	}


	@FXML
	public void replaceRequirement(){
		boolean nameExists = nameExists();
		final Requirement oldRequirement = (Requirement) voManagerStage.getSelectedRequirement();

		//If another requirement has the name we have chosen we should not allow the change
		if(nameExists && !oldRequirement.getName().equals(tfName.getText())) {
			warnAlreadyExists();
			return;
		}
		ArrayList<Requirement> predecessors = new ArrayList<>(oldRequirement.getPreviousVersions());
		predecessors.add(oldRequirement);
		final Requirement newRequirement = new Requirement(tfName.getText(), cbRequirementLinkMachineChoice.getValue(), cbRequirementChoice.getValue(), taRequirement.getText(), oldRequirement.getValidationObligations(), predecessors, null);
		currentProject.replaceRequirement(oldRequirement, newRequirement);
	}

	@FXML
	public void historyRequirement(){
		Stage table = new RequirementHistoryTable((Requirement) voManagerStage.getSelectedRequirement());
		stageManager.loadFXML(table, "requirement_history_box.fxml", this.getClass().getName());
		table.show();
		table.toFront();
	}

	@FXML
	public void addRequirement() {
		if(!tfName.getText().trim().isEmpty() && !taRequirement.getText().trim().isEmpty()) {
			boolean nameExists = nameExists();
			addRequirement(nameExists);
			voManagerStage.closeEditingBox();
		} else {
			warnNotValid();
		}
	}

	private boolean nameExists(){
		return currentProject.getRequirements().stream()
				.map(Requirement::getName)
				.collect(Collectors.toList())
				.contains(tfName.getText());
	}

	private void addRequirement(boolean nameExists) {
		if(nameExists) {
			warnAlreadyExists();
			return;
		}
		currentProject.addRequirement(new Requirement(tfName.getText(), cbRequirementLinkMachineChoice.getValue(), cbRequirementChoice.getValue(), taRequirement.getText(), Collections.emptySet()));
	}

	@FXML
	private void refineRequirement(){
		Stage requirementRefineDialog = new RequirementRefineDialog(currentProject, (Requirement) voManagerStage.getSelectedRequirement());
		stageManager.loadFXML(requirementRefineDialog, "requirements_refine_dialog.fxml", this.getClass().getName());
		requirementRefineDialog.show();
		requirementRefineDialog.toFront();
	}

	public void resetRequirementEditing() {
		cbRequirementChoice.getSelectionModel().clearSelection();
		taRequirement.clear();
		tfName.clear();
		voManagerStage.clearRequirementsSelection();
	}

	public void showRequirement(Requirement requirement, boolean edit) {
		if(edit) {
			voManagerStage.switchMode(VOManagerStage.EditType.MODIFY, VOManagerStage.Mode.REQUIREMENT);
		}
		if(requirement == null) {
			return;
		}
		tfName.setText(requirement.getName());
		cbRequirementLinkMachineChoice.getSelectionModel().select(requirement.getIntroducedAt());
		cbRequirementChoice.getSelectionModel().select(requirement.getType());
		taRequirement.setText(requirement.getText());
	}

	private void warnNotValid() {
		stageManager.makeAlert(Alert.AlertType.INFORMATION, "vomanager.warnings.requirement.notValid.header", "vomanager.warnings.requirement.notValid.content").show();
	}

	private void warnAlreadyExists() {
		stageManager.makeAlert(Alert.AlertType.INFORMATION, "vomanager.warnings.requirement.alreadyExists.header", "vomanager.warnings.requirement.alreadyExists.content").show();
	}

	public void setVoManagerStage(VOManagerStage voManagerStage) {
		this.voManagerStage = voManagerStage;
	}

	public void updateLinkedMachines(List<Machine> machines) {
		this.linkedMachineNames.setAll(machines.stream().map(Machine::getName).collect(Collectors.toList()));
	}

}
