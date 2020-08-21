package de.prob2.ui.verifications.symbolicchecking;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicChoosingStage;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaInput;
import de.prob2.ui.symbolic.SymbolicGUIType;

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
		super(stageManager, currentProject, injector, bundle, currentTrace);
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
		SymbolicCheckingFormulaItem formulaItem = null;
		addFormula();
		switch(checkingType) {
			case INVARIANT:
				String selectedEvent = cbOperations.getSelectionModel().getSelectedItem();
				formulaItem = new SymbolicCheckingFormulaItem(selectedEvent, selectedEvent, SymbolicExecutionType.INVARIANT);
				symbolicCheckingFormulaHandler.handleInvariant(formulaItem, false);
				break;
			case CHECK_ALL_OPERATIONS:
				for (final String event : events) {
					final SymbolicCheckingFormulaItem item = new SymbolicCheckingFormulaItem(event, event, SymbolicExecutionType.INVARIANT);
					symbolicCheckingFormulaHandler.handleInvariant(item, true);
				}
				break;
			case DEADLOCK:
				final String predicate = predicateBuilderView.getPredicate();
				formulaItem = new SymbolicCheckingFormulaItem(predicate, predicate, SymbolicExecutionType.DEADLOCK);
				symbolicCheckingFormulaHandler.handleDeadlock(formulaItem, false);
				break;
			default:
				formulaItem = new SymbolicCheckingFormulaItem(checkingType.name(), checkingType.name(), checkingType);
				switch(checkingType) {
					case CHECK_STATIC_ASSERTIONS:
						symbolicCheckingFormulaHandler.handleStaticAssertions(formulaItem, false);
						break;
					case CHECK_DYNAMIC_ASSERTIONS:
						symbolicCheckingFormulaHandler.handleDynamicAssertions(formulaItem, false);
						break;
					case CHECK_WELL_DEFINEDNESS:
						symbolicCheckingFormulaHandler.handleWellDefinedness(formulaItem, false);
						break;
					case CHECK_REFINEMENT:
						symbolicCheckingFormulaHandler.handleRefinement(formulaItem, false);
						break;
					case FIND_REDUNDANT_INVARIANTS:
						symbolicCheckingFormulaHandler.findRedundantInvariants(formulaItem, false);
						break;
					default:
						SymbolicModelcheckCommand.Algorithm algorithm = checkingType.getAlgorithm();
						if(algorithm != null) {
							symbolicCheckingFormulaHandler.handleSymbolic(formulaItem, algorithm, false);
						}
						break;
				}
		}
		injector.getInstance(SymbolicCheckingChoosingStage.class).close();
	}

	@Override
	protected void addFormula() {
		SymbolicExecutionType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getExecutionType();
		if(checkingType == SymbolicExecutionType.INVARIANT && cbOperations.getSelectionModel().getSelectedItem() == null) {
			return;
		}
		SymbolicGUIType guiType = injector.getInstance(SymbolicCheckingChoosingStage.class).getGUIType();
		switch(guiType) {
			case CHOICE_BOX:
				if (checkingType == SymbolicExecutionType.INVARIANT) {
					String item = cbOperations.getSelectionModel().getSelectedItem();
					symbolicCheckingFormulaHandler.addFormula(item, item, SymbolicExecutionType.INVARIANT);
					break;
				} else {
					throw new AssertionError("Unhandled checking type: " + checkingType);
				}
			case TEXT_FIELD:
				symbolicCheckingFormulaHandler.addFormula(tfFormula.getText(), tfFormula.getText(), checkingType);
				break;
			case PREDICATE:
				final String predicate = predicateBuilderView.getPredicate();
				symbolicCheckingFormulaHandler.addFormula(predicate, predicate, checkingType);
				break;
			case NONE:
				if(checkingType == SymbolicExecutionType.CHECK_ALL_OPERATIONS) {
					for(String event : events) {
						symbolicCheckingFormulaHandler.addFormula(event, event, SymbolicExecutionType.INVARIANT);
					}
				} else {
					symbolicCheckingFormulaHandler.addFormula(checkingType.name(), checkingType.name(), checkingType);
				}
				break;
			default:
				throw new AssertionError("Unhandled GUI type: " + guiType);
		}
		injector.getInstance(SymbolicCheckingChoosingStage.class).close();
	}
		
	@FXML
	public void cancel() {
		injector.getInstance(SymbolicCheckingChoosingStage.class).close();
	}

}
