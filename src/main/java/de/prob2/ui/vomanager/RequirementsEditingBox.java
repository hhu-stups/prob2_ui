package de.prob2.ui.vomanager;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.voparser.VOParseException;
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
	private ChoiceBox<Machine> cbRequirementLinkMachineChoice;

	@FXML
	private Button applyButton;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final VOChecker voChecker;

	private final VOErrorHandler voErrorHandler;

	private final RequirementHandler requirementHandler;

	private VOManagerStage voManagerStage;

	@Inject
	public RequirementsEditingBox(final StageManager stageManager, final CurrentProject currentProject, final VOChecker voChecker,
								  final VOErrorHandler voErrorHandler, final RequirementHandler requirementHandler) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.voChecker = voChecker;
		this.voErrorHandler = voErrorHandler;
		this.requirementHandler = requirementHandler;
		stageManager.loadFXML(this, "requirements_editing_box.fxml");
	}

	@FXML
	private void initialize() {
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
		ArrayList<Requirement> parents = new ArrayList<>(oldRequirement.getPreviousVersions());
		parents.add(oldRequirement);
		final Requirement newRequirement = new Requirement(tfName.getText(), cbRequirementLinkMachineChoice.getValue().toString(), cbRequirementChoice.getValue(), taRequirement.getText(), parents);
		currentProject.replaceRequirement(oldRequirement, newRequirement);

		updateVOs(oldRequirement, newRequirement);
	}

	private void updateVOs(Requirement oldRequirement, Requirement newRequirement) {
		// Update validation obligations, this means update VO of ids that are affected
		for (Machine machine : currentProject.getMachines()) {
			for (ListIterator<ValidationObligation> iterator = machine.getValidationObligations().listIterator(); iterator.hasNext();) {
				final ValidationObligation oldVo = iterator.next();
				if (oldVo.getRequirement().equals(oldRequirement.getName())) {
					try {
						ValidationObligation validationObligation = oldVo.changeRequirement(tfName.getText());
						voChecker.parseAndCheckVOExpression(validationObligation, false);
						iterator.set(validationObligation);
						requirementHandler.initListenerForVO(newRequirement, validationObligation);
					} catch (VOParseException e) {
						voErrorHandler.handleError(this.getScene().getWindow(), e);
					}
				}
			}
		}
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
		currentProject.addRequirement(new Requirement(tfName.getText(), cbRequirementLinkMachineChoice.getValue().toString(), cbRequirementChoice.getValue(), taRequirement.getText()));
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
		String machineName = requirement.getIntroducedAt();
		Machine linkedMachine = currentProject.getMachines().stream()
				.filter(mch -> mch.getName().equals(machineName))
				.findAny()
				.orElse(null);
		tfName.setText(requirement.getName());
		cbRequirementLinkMachineChoice.getSelectionModel().select(linkedMachine);
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
		cbRequirementLinkMachineChoice.getItems().clear();
		cbRequirementLinkMachineChoice.getItems().addAll(machines);
	}

}
