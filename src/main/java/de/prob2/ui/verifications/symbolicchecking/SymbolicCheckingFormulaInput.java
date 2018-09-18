package de.prob2.ui.verifications.symbolicchecking;


import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaInput;
import de.prob2.ui.symbolic.SymbolicGUIType;

import javafx.fxml.FXML;


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
	
	protected void setCheckListeners() {
		btAdd.setOnAction(e -> addFormula(false));
		btCheck.setOnAction(e -> {
			SymbolicExecutionType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getExecutionType();
			SymbolicCheckingFormulaItem formulaItem = null;
			addFormula(true);
			switch(checkingType) {
				case INVARIANT: 
					symbolicCheckingFormulaHandler.handleInvariant(cbOperations.getSelectionModel().getSelectedItem(), false);
					break;
				case CHECK_ALL_OPERATIONS:
					events.forEach(event -> symbolicCheckingFormulaHandler.handleInvariant(event, true));
					break;
				default:
					formulaItem = new SymbolicCheckingFormulaItem(checkingType.name(), checkingType.name(), checkingType);
					switch(checkingType) {
						case CHECK_ASSERTIONS: 
							symbolicCheckingFormulaHandler.handleAssertions(formulaItem, false);
							break;
						case CHECK_REFINEMENT: 
							symbolicCheckingFormulaHandler.handleRefinement(formulaItem, false); 
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
		});
	}
	
	private void addFormula(boolean checking) {
		SymbolicExecutionType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getExecutionType();
		if(checkingType == SymbolicExecutionType.INVARIANT && cbOperations.getSelectionModel().getSelectedItem() == null) {
			injector.getInstance(SymbolicCheckingChoosingStage.class).close();
			return;
		}
		SymbolicGUIType guiType = injector.getInstance(SymbolicCheckingChoosingStage.class).getGUIType();
		switch(guiType) {
			case CHOICE_BOX:
				switch(checkingType) {
					case INVARIANT:
						String item = cbOperations.getSelectionModel().getSelectedItem();
						symbolicCheckingFormulaHandler.addFormula(item, item, SymbolicExecutionType.INVARIANT, checking);
						break;
					case CHECK_ALL_OPERATIONS:
						for(String event : events) {
							symbolicCheckingFormulaHandler.addFormula(event, event, SymbolicExecutionType.INVARIANT, checking);
						}
						break;
					default:
						throw new AssertionError("Unhandled checking type: " + checkingType);
				}
				break;
			case TEXT_FIELD:
				symbolicCheckingFormulaHandler.addFormula(tfFormula.getText(), tfFormula.getText(), checkingType, checking);
				break;
			case PREDICATE:
				final String predicate = predicateBuilderView.getPredicate();
				symbolicCheckingFormulaHandler.addFormula(predicate, predicate, checkingType, checking);
				break;
			case NONE:
				if(checkingType == SymbolicExecutionType.CHECK_ALL_OPERATIONS) {
					for(String event : events) {
						symbolicCheckingFormulaHandler.addFormula(event, event, SymbolicExecutionType.INVARIANT, checking);
					}
				} else {
					symbolicCheckingFormulaHandler.addFormula(checkingType.name(), checkingType.name(), checkingType, checking);
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
