package de.prob2.ui.verifications.symbolicchecking;

import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicChoosingStage;
import de.prob2.ui.symbolic.SymbolicExecutionType;

import javafx.fxml.FXML;

@Singleton
public class SymbolicCheckingChoosingStage extends SymbolicChoosingStage<SymbolicCheckingFormulaItem> {
	private final SymbolicCheckingFormulaHandler symbolicCheckingFormulaHandler;
	
	@Inject
	private SymbolicCheckingChoosingStage(
		final StageManager stageManager,
		final SymbolicCheckingFormulaHandler symbolicCheckingFormulaHandler,
		final ResourceBundle bundle,
		final CurrentProject currentProject,
		final CurrentTrace currentTrace
	) {
		super(bundle, currentProject, currentTrace);
		this.symbolicCheckingFormulaHandler = symbolicCheckingFormulaHandler;
		stageManager.loadFXML(this, "symbolic_checking_choice.fxml");
	}
	
	@Override
	protected boolean updateFormula(SymbolicCheckingFormulaItem item) {
		Machine currentMachine = currentProject.getCurrentMachine();
		String formula = extractFormula();
		final SymbolicCheckingFormulaItem newItem = new SymbolicCheckingFormulaItem(formula, formula, this.getExecutionType());
		if(this.getExecutionType() == SymbolicExecutionType.CHECK_ALL_OPERATIONS || (this.getExecutionType() == SymbolicExecutionType.INVARIANT && cbOperations.getSelectionModel().getSelectedItem() == null)) {
			return true;
		}
		if(currentMachine.getSymbolicCheckingFormulas().stream().noneMatch(newItem::settingsEqual)) {
			currentMachine.getSymbolicCheckingFormulas().set(currentMachine.getSymbolicCheckingFormulas().indexOf(item), newItem);
			return true;
		}
		return false;
	}
	
	@Override
	public void checkFormula() {
		SymbolicExecutionType checkingType = this.getExecutionType();
		if(checkingType == SymbolicExecutionType.INVARIANT && cbOperations.getSelectionModel().getSelectedItem() == null) {
			return;
		}
		final String formula = extractFormula();
		final SymbolicCheckingFormulaItem formulaItem = new SymbolicCheckingFormulaItem(formula, formula, checkingType);
		addFormula();
		if (checkingType == SymbolicExecutionType.CHECK_ALL_OPERATIONS) {
			for (final String event : events) {
				final SymbolicCheckingFormulaItem item = new SymbolicCheckingFormulaItem(event, event, SymbolicExecutionType.INVARIANT);
				symbolicCheckingFormulaHandler.handleInvariant(item, true);
			}
		} else {
			symbolicCheckingFormulaHandler.handleItem(formulaItem, false);
		}
		this.close();
	}
	
	@Override
	protected void addFormula() {
		SymbolicExecutionType checkingType = this.getExecutionType();
		if(checkingType == SymbolicExecutionType.INVARIANT && cbOperations.getSelectionModel().getSelectedItem() == null) {
			return;
		}
		final String formula = extractFormula();
		final SymbolicCheckingFormulaItem item = new SymbolicCheckingFormulaItem(formula, formula, checkingType);
		if(checkingType == SymbolicExecutionType.CHECK_ALL_OPERATIONS) {
			for(String event : events) {
				symbolicCheckingFormulaHandler.addFormula(new SymbolicCheckingFormulaItem(event, event, SymbolicExecutionType.INVARIANT));
			}
		} else {
			symbolicCheckingFormulaHandler.addFormula(item);
		}
		this.close();
	}
	
	@FXML
	public void cancel() {
		this.close();
	}
}
