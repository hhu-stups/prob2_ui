package de.prob2.ui.vomanager;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

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

	public RequirementRefineDialog(CurrentProject currentProject, Requirement oldReq) {
		this.currentProject = currentProject;
		this.oldReq = oldReq;
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
		final Set<ValidationObligation> refinedVos = checkBox.isSelected() ? oldReq.getValidationObligations() : Collections.emptySet();
		Requirement newReq = new Requirement(oldReq.getName()+"_refined", targetMenu.getValue(), oldReq.getType(), oldRequirement.getText(), refinedVos, Collections.emptyList(), oldReq);

		currentProject.addRequirement(newReq);

		this.close();
	}

	@FXML
	private void cancel(){
		this.close();
	}
}
