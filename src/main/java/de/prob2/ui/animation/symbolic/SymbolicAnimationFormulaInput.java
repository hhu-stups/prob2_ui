package de.prob2.ui.animation.symbolic;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationSettingsHandler;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicChoosingStage;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaInput;
import de.prob2.ui.symbolic.SymbolicGUIType;
import de.prob2.ui.symbolic.SymbolicView;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@FXMLInjected
@Singleton
public class SymbolicAnimationFormulaInput extends SymbolicFormulaInput<SymbolicAnimationFormulaItem> {
	
	
	private final SymbolicAnimationFormulaHandler symbolicAnimationFormulaHandler;
	
	private final TestCaseGenerationSettingsHandler testCaseGenerationSettingsHandler;

	@Inject
	private SymbolicAnimationFormulaInput(final StageManager stageManager, final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle,
										 final CurrentTrace currentTrace, final SymbolicAnimationFormulaHandler symbolicAnimationFormulaHandler, final TestCaseGenerationSettingsHandler testCaseGenerationSettingsHandler) {
		super(stageManager, currentProject, injector, bundle, currentTrace);
		this.symbolicAnimationFormulaHandler = symbolicAnimationFormulaHandler;
		this.testCaseGenerationSettingsHandler = testCaseGenerationSettingsHandler;
		stageManager.loadFXML(this, "symbolic_animation_formula_input.fxml");
	}

	@Override
	protected boolean updateFormula(SymbolicAnimationFormulaItem item, SymbolicView<SymbolicAnimationFormulaItem> view, SymbolicChoosingStage<SymbolicAnimationFormulaItem> choosingStage) {
		Machine currentMachine = currentProject.getCurrentMachine();
		String formula = extractFormula(choosingStage);
		Map<String, Object> additionalInformation = testCaseGenerationSettingsHandler.extractAdditionalInformation(choosingStage, mcdcInputView, operationCoverageInputView);
		boolean valid = testCaseGenerationSettingsHandler.isValid(choosingStage, mcdcInputView, operationCoverageInputView);
		SymbolicAnimationFormulaItem newItem = new SymbolicAnimationFormulaItem(formula, choosingStage.getExecutionType(), additionalInformation);
		if(!currentMachine.getSymbolicAnimationFormulas().contains(newItem)) {
			if(valid) {
				SymbolicExecutionType type = choosingStage.getExecutionType();
				item.setData(formula, type.getName(), formula, type, additionalInformation);
				item.reset();
				view.refresh();
			}
			return true;
		}
		return false;
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
				if(!testCaseGenerationSettingsHandler.checkMCDCSettings(mcdcInputView.getLevel(), mcdcInputView.getDepth())) {
					return;
				}
				formulaItem = new SymbolicAnimationFormulaItem(Integer.parseInt(mcdcInputView.getDepth()), Integer.parseInt(mcdcInputView.getLevel()));
				symbolicAnimationFormulaHandler.generateTestCases(formulaItem, false);
				break;
			}
			case COVERED_OPERATIONS: {
				if(!testCaseGenerationSettingsHandler.checkOperationCoverageSettings(operationCoverageInputView.getOperations(), operationCoverageInputView.getDepth())) {
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
				if(!testCaseGenerationSettingsHandler.checkMCDCSettings(mcdcInputView.getLevel(), mcdcInputView.getDepth())) {
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
				if(!testCaseGenerationSettingsHandler.checkInteger(operationCoverageInputView.getDepth())) {
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
