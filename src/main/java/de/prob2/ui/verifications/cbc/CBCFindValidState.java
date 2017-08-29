package de.prob2.ui.verifications.cbc;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.cbc.CBCFormulaItem.CBCType;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

public class CBCFindValidState extends AbstractCBCFormulaInputStage {
	
	@FXML
	private TextField tfPredicate;
		
	@Inject
	private CBCFindValidState(final StageManager stageManager, final CBCFormulaHandler cbcHandler,
						final Injector injector) {
		super(cbcHandler, injector);
		stageManager.loadFXML(this, "cbc_findValidState.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	
	@FXML
	public void checkFormula() {
		CBCFormulaFindStateItem item = new CBCFormulaFindStateItem(tfPredicate.getText(), tfPredicate.getText(), 
												CBCType.FIND_VALID_STATE);
		cbcHandler.addFormula(item, true);
		cbcHandler.findValidState(item);
		this.close();
	}
	
	public void changeFormula(CBCFormulaItem item) {
		super.changeFormula(tfPredicate, item);
	}
	
	@FXML
	public void cancel() {
		this.close();
	}

}
