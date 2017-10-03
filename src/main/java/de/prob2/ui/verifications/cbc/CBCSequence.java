package de.prob2.ui.verifications.cbc;

import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

public class CBCSequence extends AbstractCBCFormulaInputStage {

	@FXML
	private TextField tfSequence;
		
	@Inject
	private CBCSequence(final StageManager stageManager, final CBCFormulaHandler cbcHandler, final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle) {
		super(cbcHandler, currentProject, injector, bundle);
		stageManager.loadFXML(this, "cbc_sequence.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void addFormula() {
		addFormula(false);
	}
	
	private void addFormula(boolean checking) {
		cbcHandler.addFormula(tfSequence.getText(), tfSequence.getText(), CBCFormulaItem.CBCType.SEQUENCE,
								checking);
		this.close();
	}

	@FXML
	public void checkFormula() {
		addFormula(true);
		cbcHandler.checkSequence(tfSequence.getText());
		this.close();
	}
	
	public void changeFormula(CBCFormulaItem item) {
		super.changeFormula(tfSequence, item, invisibles);
	}

	@FXML
	public void cancel() {
		this.close();
	}
	
	
}
