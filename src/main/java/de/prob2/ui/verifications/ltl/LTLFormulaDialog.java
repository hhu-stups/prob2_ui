package de.prob2.ui.verifications.ltl;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

public class LTLFormulaDialog extends Dialog<LTLFormulaItem> {
	
	@FXML
	private TextField tfName;
	
	@FXML
	private TextArea taDescription;
	
	@FXML
	private TextArea taFormula;

	@Inject
	public LTLFormulaDialog(final StageManager stageManager, final Injector injector) {
		super();
		this.setResultConverter(type -> {
			if(type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				return new LTLFormulaItem(this, tfName.getText(), taDescription.getText(), taFormula.getText());
			}
		});
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "ltlformula_dialog.fxml");
	}
	
	public void loadDialog(String name, String description, String formula) {
		tfName.setText(name);
		taDescription.setText(description);
		taFormula.setText(formula);
	}
			
}
