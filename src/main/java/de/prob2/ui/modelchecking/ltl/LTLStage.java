package de.prob2.ui.modelchecking.ltl;

import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

@Singleton
public class LTLStage extends Stage{
	
	@FXML
	private ListView<LTLFormula> lv_formula;
	
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
		Optional<LTLFormula> op = injector.getInstance(AddLTLFormulaDialog.class).showAndWait();
		lv_formula.getItems().add(op.get());
	}

}
