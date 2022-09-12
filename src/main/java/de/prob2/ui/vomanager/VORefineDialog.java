package de.prob2.ui.vomanager;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class VORefineDialog extends Stage {



	@FXML
	private TextArea oldVOExpression;

	@FXML
	private TextArea newVOExpression;

	private final String oldExpression;

	public VORefineDialog(String oldExpression){
		this.oldExpression = oldExpression;
	}

	@FXML
	public void initialize() {
		oldVOExpression.setText(oldExpression);

	}

	@FXML
	private void refine(){

	}

	@FXML
	private void cancel(){

	}
}
