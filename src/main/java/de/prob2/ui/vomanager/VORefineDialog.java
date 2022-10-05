package de.prob2.ui.vomanager;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import de.prob.voparser.VOParseException;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class VORefineDialog extends Stage {



	@FXML
	private TextArea oldVOExpression;

	@FXML
	private TextArea newVOExpression;

	@FXML
	private ComboBox<String> targetMenu;

	private final ValidationObligation oldVO;
	private final CurrentProject currentProject;

	private final VOChecker voChecker;

	private final ChoiceBox<Requirement> cbLinkRequirementChoice;

	private final VOErrorHandler voErrorHandler;

	public VORefineDialog(CurrentProject currentProject, ValidationObligation oldVo, VOChecker voChecker, ChoiceBox<Requirement> cbLinkRequirementChoice, VOErrorHandler voErrorHandler){
		this.currentProject = currentProject;
		this.oldVO = oldVo;
		this.voChecker = voChecker;
		this.cbLinkRequirementChoice = cbLinkRequirementChoice;
		this.voErrorHandler = voErrorHandler;
	}

	@FXML
	public void initialize() {
		oldVOExpression.setText(oldVO.getExpression());
		targetMenu.getItems().addAll(currentProject.getMachines().stream()
				.map(Machine::getName)
				.filter(entry -> !entry.equals(currentProject.getCurrentMachine().getName()))
				.collect(Collectors.toList()));
		targetMenu.getSelectionModel().selectFirst();
	}

	@FXML
	private void refine(){

		try {
			ValidationObligation newVo = new ValidationObligation(oldVO.getMachine(), newVOExpression.getText(), oldVO);
			Machine machine = currentProject.getMachines().stream().filter(entry -> entry.getName().equals(targetMenu.getValue())).collect(Collectors.toList()).get(0);
			voChecker.parseVO(machine, newVo);

			final Requirement oldRequirement = cbLinkRequirementChoice.getValue();
			final Set<ValidationObligation> updatedVos = new HashSet<>(oldRequirement.getValidationObligations());
			updatedVos.add(newVo);
			final Requirement updatedRequirement = new Requirement(oldRequirement.getName(), oldRequirement.getIntroducedAt(), oldRequirement.getType(), oldRequirement.getText(), updatedVos);
			currentProject.replaceRequirement(oldRequirement, updatedRequirement);

			this.close();
		} catch (VOParseException e) {
			voErrorHandler.handleError(this.getScene().getWindow(), e);
		}

		this.close();
	}

	@FXML
	private void cancel(){
		this.close();
	}
}