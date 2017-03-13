package de.prob2.ui.modelchecking.ltl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@Singleton
public class LTLStage extends Stage{
	
	private final Injector injector;
	
	@Inject
	public LTLStage(final StageManager stageManager, final Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "ltlView.fxml");
	}
	
	@FXML
	public void initialize() {
		
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(AddLTLFormulaDialog.class).show();
	}

}
