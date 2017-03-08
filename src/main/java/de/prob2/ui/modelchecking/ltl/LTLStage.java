package de.prob2.ui.modelchecking.ltl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

@Singleton
public class LTLStage extends Stage{
		
	@FXML
	private TabPane formulaPane; 
	
	@FXML
	private Tab addTab;
	
	@Inject
	public LTLStage(final StageManager stageManager) {
		stageManager.loadFXML(this, "ltlView.fxml");
	}
	
	@FXML
	public void initialize() {
		
	}

}
