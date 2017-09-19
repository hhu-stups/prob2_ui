package de.prob2.ui.verifications.modelchecking;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ModelcheckingItemFullValueStage extends Stage {
	
	@FXML
	private TextField tfStrategy;
	
	@FXML
	private TextArea taDescription;
	
	@Inject
	private ModelcheckingItemFullValueStage(StageManager stageManager) {
		stageManager.loadFXML(this, "modelchecking_item_fullvalue.fxml", this.getClass().getName());
	}
	
	public void setValues(String strategy, String description) {
		tfStrategy.setText(strategy);
		taDescription.setText(description);
	}
	
	
}
