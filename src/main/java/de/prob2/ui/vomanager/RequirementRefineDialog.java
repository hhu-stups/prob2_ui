package de.prob2.ui.vomanager;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.util.Collections;
import java.util.stream.Collectors;

public class RequirementRefineDialog extends Stage {



	@FXML
	private TextArea oldRequirement;

	@FXML
	private TextArea newRequirement;

	@FXML
	private ComboBox<String> targetMenu;

	@FXML
	private CheckBox checkBox;

	private final Requirement oldReq;

	private final CurrentProject currentProject;

	private final RequirementHandler requirementHandler;

	public RequirementRefineDialog(CurrentProject currentProject, Requirement oldReq, RequirementHandler requirementHandler){
		this.currentProject = currentProject;
		this.oldReq = oldReq;
		this.requirementHandler = requirementHandler;

	}

	public void initialize(){
		oldRequirement.setText(oldReq.getText());
		targetMenu.getItems().addAll(currentProject.getMachines().stream()
				.map(Machine::getName)
				.filter(entry -> !entry.equals(currentProject.getCurrentMachine().getName()))
				.collect(Collectors.toList()));
		targetMenu.getSelectionModel().selectFirst();
	}


	@FXML
	private void refine(){

		Requirement newReq = new Requirement(oldReq.getName()+"_refined", targetMenu.getValue(), oldReq.getType(), oldRequirement.getText(), Collections.emptyList(), oldReq);

		currentProject.addRequirement(newReq);
		if(checkBox.selectedProperty().get()){
			Machine target = currentProject.getMachines().stream().filter(entry -> entry.getName().equals(targetMenu.getValue())).collect(Collectors.toList()).get(0);
			target.getValidationObligations().addAll(currentProject.getCurrentMachine().getValidationObligations());
		}

		this.close();
	}

	@FXML
	private void cancel(){
		this.close();
	}
}
