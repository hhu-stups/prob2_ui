package de.prob2.ui.verifications.symbolic;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;

@Singleton
public class SymbolicModelcheckingView extends AnchorPane {
	
	@FXML
	private Button checkMachineButton;
	
	@FXML
	private Button addFormulaButton;
	
	private final CurrentTrace currentTrace;
	
	@Inject
	public SymbolicModelcheckingView(final StageManager stageManager, final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this, "symbolic_modelchecking_view.fxml");
	}
	
	@FXML
	public void initialize() {
		checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not());
		addFormulaButton.disableProperty().bind(currentTrace.existsProperty().not());
	}

	@FXML
	public void checkMachine() {
		SymbolicModelcheckCommand command = new SymbolicModelcheckCommand(SymbolicModelcheckCommand.Algorithm.TINDUCTION);
		currentTrace.getStateSpace().execute(command);
		System.out.println(command.getResult());
		//System.out.println(command.getCounterExample());
	}
	
	@FXML
	public void addFormula() {
		
	}	
	
	@FXML
	public void cancel() {
		
	}
	
}
