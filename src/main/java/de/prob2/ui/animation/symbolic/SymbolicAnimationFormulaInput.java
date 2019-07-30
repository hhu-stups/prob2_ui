package de.prob2.ui.animation.symbolic;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaInput;
import de.prob2.ui.symbolic.SymbolicGUIType;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.util.List;
import java.util.ResourceBundle;

@FXMLInjected
@Singleton
public class SymbolicAnimationFormulaInput extends SymbolicFormulaInput<SymbolicAnimationFormulaItem> {
	
	
	private final SymbolicAnimationFormulaHandler symbolicAnimationFormulaHandler;

	@Inject
	public SymbolicAnimationFormulaInput(final StageManager stageManager,
										 final SymbolicAnimationFormulaHandler symbolicAnimationFormulaHandler,
										 final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle,
										 final CurrentTrace currentTrace) {
		super(stageManager, currentProject, injector, bundle, currentTrace);
		this.symbolicAnimationFormulaHandler = symbolicAnimationFormulaHandler;
		stageManager.loadFXML(this, "symbolic_animation_formula_input.fxml");
	}
	
	protected void setCheckListeners() {
		btAdd.setOnAction(e -> addFormula(false));
		btCheck.setOnAction(e -> checkFormula());
	}


	@Override
	public void checkFormula() {
		SymbolicExecutionType animationType = injector.getInstance(SymbolicAnimationChoosingStage.class).getExecutionType();
		SymbolicAnimationFormulaItem formulaItem = null;
		addFormula(true);
		switch (animationType) {
			case SEQUENCE:
				formulaItem = new SymbolicAnimationFormulaItem(tfFormula.getText(), SymbolicExecutionType.SEQUENCE);
				symbolicAnimationFormulaHandler.handleSequence(formulaItem, false);
				break;
			case FIND_VALID_STATE:
				formulaItem = new SymbolicAnimationFormulaItem(predicateBuilderView.getPredicate(),
						SymbolicExecutionType.FIND_VALID_STATE);
				symbolicAnimationFormulaHandler.findValidState(formulaItem, false);
				break;
			case MCDC: {
				if(!(checkInteger(mcdcInputView.getLevel()) && checkInteger(mcdcInputView.getDepth()))) {
					return;
				}
				formulaItem = new SymbolicAnimationFormulaItem(Integer.parseInt(mcdcInputView.getDepth()), Integer.parseInt(mcdcInputView.getLevel()));
				symbolicAnimationFormulaHandler.generateTestCases(formulaItem, false);
				break;
			}
			case COVERED_OPERATIONS: {
				if(operationCoverageInputView.getOperations().isEmpty() || !checkInteger(operationCoverageInputView.getDepth())) {
					return;
				}
				formulaItem = new SymbolicAnimationFormulaItem(Integer.parseInt(operationCoverageInputView.getDepth()), operationCoverageInputView.getOperations());
				symbolicAnimationFormulaHandler.generateTestCases(formulaItem, false);
				break;
			}
			default:
				break;
		}
		injector.getInstance(SymbolicAnimationChoosingStage.class).close();
	}

	@Override
	protected void addFormula(boolean checking) {
		SymbolicExecutionType checkingType = injector.getInstance(SymbolicAnimationChoosingStage.class).getExecutionType();
		SymbolicGUIType guiType = injector.getInstance(SymbolicAnimationChoosingStage.class).getGUIType();
		switch(guiType) {
			case TEXT_FIELD:
				symbolicAnimationFormulaHandler.addFormula(tfFormula.getText(), checkingType, checking);
				break;
			case PREDICATE:
				final String predicate = predicateBuilderView.getPredicate();
				symbolicAnimationFormulaHandler.addFormula(predicate, checkingType, checking);
				break;
			case NONE:
				symbolicAnimationFormulaHandler.addFormula(checkingType.name(), checkingType, checking);
				break;
			case MCDC: {
				if(!(checkInteger(mcdcInputView.getLevel()) && checkInteger(mcdcInputView.getDepth()))) {
					stageManager.makeAlert(Alert.AlertType.ERROR,
							"animation.symbolic.alerts.testcasegeneration.invalid",
							"animation.symbolic.alerts.testcasegeneration.mcdc.invalid")
							.showAndWait();
					return;
				}
				symbolicAnimationFormulaHandler.addFormula(Integer.parseInt(mcdcInputView.getDepth()), Integer.parseInt(mcdcInputView.getLevel()), checking);
				break;
			}
			case OPERATIONS: {
				List<String> operations = operationCoverageInputView.getOperations();
				if(operations.isEmpty()) {
					stageManager.makeAlert(Alert.AlertType.ERROR,
							"animation.symbolic.alerts.testcasegeneration.operations.header",
							"animation.symbolic.alerts.testcasegeneration.operations.content").showAndWait();
					return;
				}
				if(!checkInteger(operationCoverageInputView.getDepth())) {
					stageManager.makeAlert(Alert.AlertType.ERROR,
							"animation.symbolic.alerts.testcasegeneration.invalid",
							"animation.symbolic.alerts.testcasegeneration.coveredoperations.invalid")
							.showAndWait();
					return;
				}
				symbolicAnimationFormulaHandler.addFormula(Integer.parseInt(operationCoverageInputView.getDepth()), operations, checking);
				break;
			}
			default:
				break;
		}
		injector.getInstance(SymbolicAnimationChoosingStage.class).close();
	}
		
	@FXML
	public void cancel() {
		injector.getInstance(SymbolicAnimationChoosingStage.class).close();
	}

}
