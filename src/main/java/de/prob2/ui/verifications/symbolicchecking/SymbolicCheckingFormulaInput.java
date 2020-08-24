package de.prob2.ui.verifications.symbolicchecking;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicChoosingStage;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaInput;

import javafx.fxml.FXML;

@FXMLInjected
@Singleton
public class SymbolicCheckingFormulaInput extends SymbolicFormulaInput<SymbolicCheckingFormulaItem> {
	
	private final SymbolicCheckingFormulaHandler symbolicCheckingFormulaHandler;
	
	@Inject
	public SymbolicCheckingFormulaInput(final StageManager stageManager,
										final SymbolicCheckingFormulaHandler symbolicCheckingFormulaHandler,
										final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle,
										final CurrentTrace currentTrace) {
		super(currentProject, injector, bundle, currentTrace);
		this.symbolicCheckingFormulaHandler = symbolicCheckingFormulaHandler;
		stageManager.loadFXML(this, "symbolic_checking_formula_input.fxml");
	}

	@Override
	protected boolean updateFormula(SymbolicCheckingFormulaItem item, SymbolicChoosingStage<SymbolicCheckingFormulaItem> choosingStage) {
		Machine currentMachine = currentProject.getCurrentMachine();
		String formula = extractFormula(choosingStage);
		final SymbolicCheckingFormulaItem newItem = new SymbolicCheckingFormulaItem(formula, formula, choosingStage.getExecutionType());
		if(choosingStage.getExecutionType() == SymbolicExecutionType.CHECK_ALL_OPERATIONS || (choosingStage.getExecutionType() == SymbolicExecutionType.INVARIANT && cbOperations.getSelectionModel().getSelectedItem() == null)) {
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
		SymbolicExecutionType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getExecutionType();
		if(checkingType == SymbolicExecutionType.INVARIANT && cbOperations.getSelectionModel().getSelectedItem() == null) {
			return;
		}
		final String formula = extractFormula(injector.getInstance(SymbolicCheckingChoosingStage.class));
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
		injector.getInstance(SymbolicCheckingChoosingStage.class).close();
	}

	@Override
	protected void addFormula() {
		SymbolicExecutionType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getExecutionType();
		if(checkingType == SymbolicExecutionType.INVARIANT && cbOperations.getSelectionModel().getSelectedItem() == null) {
			return;
		}
		final String formula = extractFormula(injector.getInstance(SymbolicCheckingChoosingStage.class));
		final SymbolicCheckingFormulaItem item = new SymbolicCheckingFormulaItem(formula, formula, checkingType);
		if(checkingType == SymbolicExecutionType.CHECK_ALL_OPERATIONS) {
			for(String event : events) {
				symbolicCheckingFormulaHandler.addFormula(event, event, SymbolicExecutionType.INVARIANT);
			}
		} else {
			symbolicCheckingFormulaHandler.addFormula(item);
		}
		injector.getInstance(SymbolicCheckingChoosingStage.class).close();
	}
		
	@FXML
	public void cancel() {
		injector.getInstance(SymbolicCheckingChoosingStage.class).close();
	}

}
