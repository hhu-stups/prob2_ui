package de.prob2.ui.verifications.cbc;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.cbc.CBCFormulaItem.CBCType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

@Deprecated
public class CBCFindValidState extends AbstractCBCFormulaInputStage {
	
	@FXML
	private TextField tfPredicate;
	
	@FXML
	private Button findValidStateButton;
	
	private CurrentTrace currentTrace;
		
	@Inject
	private CBCFindValidState(final StageManager stageManager, final CBCFormulaHandler cbcHandler,
						final Injector injector, final CurrentTrace currentTrace) {
		super(cbcHandler, injector);
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this, "cbc_findValidState.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void initialize() {
		findValidStateButton.disableProperty().bind(currentTrace.existsProperty().not());
	}
	
	
	@FXML
	public void checkFormula() {

	}
	
	public void changeFormula(CBCFormulaItem item) {
		super.changeFormula(tfPredicate, item, invisibles);
	}
	
	@FXML
	public void cancel() {
		this.close();
	}

}
