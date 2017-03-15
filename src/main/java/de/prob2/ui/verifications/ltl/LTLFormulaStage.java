package de.prob2.ui.verifications.ltl;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class LTLFormulaStage extends Stage {
	
	@FXML
	private TextArea ta_formula;
	
	@Inject
	private LTLFormulaStage(StageManager stageManager) {
		stageManager.loadFXML(this, "ltlFormulaStage.fxml");
	}
	
	@FXML
	private void handleSave() {
		
	}
	
	@FXML
	private void handleClose() {
		this.close();
	}
	
	@FXML
	private void checkFormula() {
		
	}
	
	private String getFormula() {
		return ta_formula.getText();
	}
	
}
