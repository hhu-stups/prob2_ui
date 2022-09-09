package de.prob2.ui.vomanager;

import de.prob.voparser.VOParseException;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
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
	private void refine(){



	}

	@FXML
	private void cancel(){
		this.close();
	}
}
