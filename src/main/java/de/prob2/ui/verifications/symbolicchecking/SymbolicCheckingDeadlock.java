package de.prob2.ui.verifications.symbolicchecking;

import java.util.Arrays;
import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem.SymbolicCheckingType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

public class SymbolicCheckingDeadlock extends AbstractSymbolicCheckingFormulaInputStage {
	
	
	@FXML
	private TextField tfFormula;
	
	@FXML
	private Button findDeadlockButton;
	
	@FXML
	private Button findValidStateButton;
	
	@Inject
	private SymbolicCheckingDeadlock(final StageManager stageManager, final SymbolicCheckingFormulaHandler cbcHandler,
						final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle) {
		super(cbcHandler, currentProject, injector, bundle);
		stageManager.loadFXML(this, "symbolic_checking_deadlock.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void initialize() {
		this.invisibles.addAll(Arrays.asList(findDeadlockButton, findValidStateButton));
	}
			
	@FXML
	public void addFormula() {
		addFormula(false);
	}
	
	private void addFormula(boolean checking) {
		symbolicCheckingHandler.addFormula(tfFormula.getText(), tfFormula.getText(), SymbolicCheckingFormulaItem.SymbolicCheckingType.DEADLOCK,
								checking);
		this.close();
	}
	
	@FXML
	public void checkFormula() {
		addFormula(true);
		symbolicCheckingHandler.checkDeadlock(tfFormula.getText());
		this.close();
	}
	
	@FXML
	public void findDeadlock() {
		symbolicCheckingHandler.addFormula("FIND DEADLOCK", "FIND DEADLOCK", SymbolicCheckingFormulaItem.SymbolicCheckingType.FIND_DEADLOCK, true);
		symbolicCheckingHandler.findDeadlock();
		this.close();
	}
	
	@FXML
	public void findValidState() {
		SymbolicCheckingFormulaItem item = new SymbolicCheckingFormulaItem(tfFormula.getText(), tfFormula.getText(), 
				SymbolicCheckingType.FIND_VALID_STATE);
		symbolicCheckingHandler.addFormula(item, true);
		symbolicCheckingHandler.findValidState(item);
		this.close();
	}
	
	public void changeFormula(SymbolicCheckingFormulaItem item) {
		super.changeFormula(tfFormula, item, invisibles);
	}
	
	
	@FXML
	public void cancel() {
		this.close();
	}
		
}
