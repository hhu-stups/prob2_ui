package de.prob2.ui.vomanager;

import de.prob2.ui.vomanager.ast.IValidationExpression;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;



public class VORefineDialog extends Stage {



	@FXML
	private TextArea oldVOExpression;

	@FXML
	private TextArea newVOExpression;

	private final IValidationExpression oldExpression;

	public VORefineDialog(IValidationExpression oldExpression){
		this.oldExpression = oldExpression;
	}

	@FXML
	public void initialize() {
		oldVOExpression.setText(oldExpression.toString());

	}

	@FXML
	private void refine(){

	}

	@FXML
	private void cancel(){

	}
}
