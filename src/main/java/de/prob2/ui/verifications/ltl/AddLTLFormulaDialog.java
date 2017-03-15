package de.prob2.ui.verifications.ltl;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

public class AddLTLFormulaDialog extends Dialog<LTLFormula> {
	
	@FXML
	private TextField tf_name;

	@Inject
	public AddLTLFormulaDialog(final StageManager stageManager) {
		super();
		this.setResultConverter(type -> {
			if(type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				return new LTLFormula(tf_name.getText());
			}
		});
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "ltlformula_dialog.fxml");
	}
	
}
