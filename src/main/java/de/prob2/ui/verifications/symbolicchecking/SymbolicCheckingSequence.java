package de.prob2.ui.verifications.symbolicchecking;

import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

public class SymbolicCheckingSequence extends AbstractSymbolicCheckingFormulaInputStage {

	@FXML
	private TextField tfSequence;
		
	@Inject
	private SymbolicCheckingSequence(final StageManager stageManager, final SymbolicCheckingFormulaHandler cbcHandler, final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle) {
		super(cbcHandler, currentProject, injector, bundle);
		stageManager.loadFXML(this, "symbolic_checking_sequence.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void addFormula() {
		addFormula(false);
	}
	
	private void addFormula(boolean checking) {
		symbolicCheckingHandler.addFormula(tfSequence.getText(), tfSequence.getText(), SymbolicCheckingFormulaItem.SymbolicCheckingType.SEQUENCE,
								checking);
		this.close();
	}

	@FXML
	public void checkFormula() {
		addFormula(true);
		symbolicCheckingHandler.checkSequence(tfSequence.getText());
		this.close();
	}
	
	public void changeFormula(SymbolicCheckingFormulaItem item) {
		super.changeFormula(tfSequence, item, invisibles);
	}

	@FXML
	public void cancel() {
		this.close();
	}
	
	
}
