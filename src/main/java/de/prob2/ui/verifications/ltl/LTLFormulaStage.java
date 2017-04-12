package de.prob2.ui.verifications.ltl;


import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class LTLFormulaStage extends Stage {
	
	
		
	@FXML
	private TextArea ta_formula;
	
	@FXML
	private Button checkFormulaButton;
	
	private final CurrentTrace currentTrace;
	
	private LTLFormulaItem item;
	
	private Injector injector;
		
	@Inject
	private LTLFormulaStage(final StageManager stageManager, final Injector injector, final CurrentTrace currentTrace) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.item = null;
		stageManager.loadFXML(this, "ltlformula_stage.fxml");
	}
	
	@FXML
	public void initialize() {
		ta_formula.textProperty().addListener((observable, oldValue, newValue) -> item.setFormula(newValue));
		checkFormulaButton.disableProperty().bind(currentTrace.existsProperty().not());
	}
	
	public void setItem(LTLFormulaItem item) {
		this.item = item;
		ta_formula.setText(item.getFormula());
	}
	
	@FXML
	private void handleSave() {
		
	}
	
	@FXML
	private void handleClose() {
		this.close();
	}
	
	@FXML
	public void checkFormula() {
		injector.getInstance(LTLView.class).checkFormula(item);
	}
	

}
