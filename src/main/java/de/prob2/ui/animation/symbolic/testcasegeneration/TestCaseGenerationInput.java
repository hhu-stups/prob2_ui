package de.prob2.ui.animation.symbolic.testcasegeneration;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.statespace.LoadedMachine;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationSettingsHandler;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@FXMLInjected
@Singleton
public class TestCaseGenerationInput extends VBox {
	
	
	private final TestCaseGenerationItemHandler testCaseGenerationFormulaHandler;
	
	private final TestCaseGenerationSettingsHandler testCaseGenerationSettingsHandler;
	
	@FXML
	protected Button btAdd;
	
	@FXML
	protected Button btCheck;
	
	@FXML
	protected MCDCInputView mcdcInputView;

	@FXML
	protected OperationCoverageInputView operationCoverageInputView;

	protected final StageManager stageManager;

	protected final Injector injector;
	
	protected final ResourceBundle bundle;
	
	protected final CurrentTrace currentTrace;
	
	protected ArrayList<String> events;
	
	protected final CurrentProject currentProject;
	
	@Inject
	private TestCaseGenerationInput(final StageManager stageManager, final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle,
										 final CurrentTrace currentTrace, final TestCaseGenerationItemHandler testCaseGenerationFormulaHandler, final TestCaseGenerationSettingsHandler testCaseGenerationSettingsHandler) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.events = new ArrayList<>();
		this.injector = injector;
		this.bundle = bundle;
		this.testCaseGenerationFormulaHandler = testCaseGenerationFormulaHandler;
		this.testCaseGenerationSettingsHandler = testCaseGenerationSettingsHandler;
		stageManager.loadFXML(this, "test_case_generation_input.fxml");
	}
	
	@FXML
	public void initialize() {
		this.update();
		currentTrace.addListener((observable, from, to) -> update());
		setCheckListeners();
	}
	
	protected void update() {
		events.clear();
		final Map<String, String> items = new LinkedHashMap<>();
		if (currentTrace.get() != null) {
			final LoadedMachine loadedMachine = currentTrace.getStateSpace().getLoadedMachine();
			if (loadedMachine != null) {
				events.addAll(loadedMachine.getOperationNames());
				loadedMachine.getConstantNames().forEach(s -> items.put(s, ""));
				loadedMachine.getVariableNames().forEach(s -> items.put(s, ""));
			}
		}
		operationCoverageInputView.setTable(events);
	}

	private boolean updateFormula(TestCaseGenerationItem item, TestCaseGenerationView view, TestCaseGenerationChoosingStage choosingStage) {
		Machine currentMachine = currentProject.getCurrentMachine();
		String formula = extractFormula(choosingStage);
		Map<String, Object> additionalInformation = testCaseGenerationSettingsHandler.extractAdditionalInformation(choosingStage, mcdcInputView, operationCoverageInputView);
		boolean valid = testCaseGenerationSettingsHandler.isValid(choosingStage, mcdcInputView, operationCoverageInputView);
		TestCaseGenerationItem newItem = new TestCaseGenerationItem(formula, choosingStage.getTestCaseGenerationType(), additionalInformation);
		if(!currentMachine.getSymbolicAnimationFormulas().contains(newItem)) {
			if(valid) {
				TestCaseGenerationType type = choosingStage.getTestCaseGenerationType();
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
	
	public void changeType(final TestCaseExecutionItem item) {
		TestCaseGenerationType type = item.getExecutionType();
		this.getChildren().removeAll(mcdcInputView, operationCoverageInputView);
		switch (type) {
			case MCDC:
				this.getChildren().add(0, mcdcInputView);
				break;
			case COVERED_OPERATIONS:
				this.getChildren().add(0, operationCoverageInputView);
				break;
			default:
				throw new AssertionError("Unhandled type: " + type);
		}
	}
	
	public void reset() {
		btAdd.setText(bundle.getString("common.buttons.add"));
		btCheck.setText(bundle.getString("symbolic.formulaInput.buttons.addAndCheck"));
		setCheckListeners();
		mcdcInputView.reset();
		operationCoverageInputView.reset();
	}

	public void checkFormula() {
		TestCaseGenerationType testCaseGenerationType = injector.getInstance(TestCaseGenerationChoosingStage.class).getTestCaseGenerationType();
		TestCaseGenerationItem item = null;
		addFormula(true);
		switch (testCaseGenerationType) {
			case MCDC: {
				if(!testCaseGenerationSettingsHandler.checkMCDCSettings(mcdcInputView.getLevel(), mcdcInputView.getDepth())) {
					return;
				}
				item = new TestCaseGenerationItem(Integer.parseInt(mcdcInputView.getDepth()), Integer.parseInt(mcdcInputView.getLevel()));
				testCaseGenerationFormulaHandler.generateTestCases(item, false);
				break;
			}
			case COVERED_OPERATIONS: {
				if(!testCaseGenerationSettingsHandler.checkOperationCoverageSettings(operationCoverageInputView.getOperations(), operationCoverageInputView.getDepth())) {
					return;
				}
				item = new TestCaseGenerationItem(Integer.parseInt(operationCoverageInputView.getDepth()), operationCoverageInputView.getOperations());
				testCaseGenerationFormulaHandler.generateTestCases(item, false);
				break;
			}
			default:
				break;
		}
		injector.getInstance(TestCaseGenerationChoosingStage.class).close();
	}

	public void addFormula(boolean checking) {
		TestCaseGenerationType type = injector.getInstance(TestCaseGenerationChoosingStage.class).getTestCaseGenerationType();
		switch(type) {
			case MCDC: {
				if(!testCaseGenerationSettingsHandler.checkMCDCSettings(mcdcInputView.getLevel(), mcdcInputView.getDepth())) {
					stageManager.makeAlert(Alert.AlertType.ERROR,
							"animation.symbolic.alerts.testcasegeneration.invalid",
							"animation.symbolic.alerts.testcasegeneration.mcdc.invalid")
							.showAndWait();
					return;
				}
				testCaseGenerationFormulaHandler.addFormula(Integer.parseInt(mcdcInputView.getDepth()), Integer.parseInt(mcdcInputView.getLevel()), checking);
				break;
			}
			case COVERED_OPERATIONS: {
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
				testCaseGenerationFormulaHandler.addFormula(Integer.parseInt(operationCoverageInputView.getDepth()), operations, checking);
				break;
			}
			default:
				break;
		}
		injector.getInstance(TestCaseGenerationChoosingStage.class).close();
	}
	
	protected String extractFormula(TestCaseGenerationChoosingStage choosingStage) {
		String formula;
		if(choosingStage.getTestCaseGenerationType() == TestCaseGenerationType.MCDC) {
			String level = mcdcInputView.getLevel();
			String depth = mcdcInputView.getDepth();
			formula = "MCDC:" + level + "/" + "DEPTH:" + depth;
		} else if(choosingStage.getTestCaseGenerationType() == TestCaseGenerationType.COVERED_OPERATIONS) {
			List<String> operations = operationCoverageInputView.getOperations();
			String depth = operationCoverageInputView.getDepth();
			formula = "OPERATION:" + String.join(",", operations) + "/" + "DEPTH:" + depth;
		} else {
			formula = choosingStage.getTestCaseGenerationType().getName();
		}
		return formula;
	}
	
	public void changeFormula(TestCaseGenerationItem item, TestCaseGenerationView view, TestCaseGenerationResultHandler resultHandler, TestCaseGenerationChoosingStage stage) {
		btAdd.setText(bundle.getString("symbolic.formulaInput.buttons.change"));
		btCheck.setText(bundle.getString("symbolic.formulaInput.buttons.changeAndCheck"));
		setChangeListeners(item, view, resultHandler, stage);
		stage.select(item);
		if(stage.getTestCaseGenerationType() == TestCaseGenerationType.MCDC) {
			mcdcInputView.setItem(item);
		} else if(stage.getTestCaseGenerationType() == TestCaseGenerationType.COVERED_OPERATIONS) {
			operationCoverageInputView.setItem(item);
		}
		stage.show();
	}
	
	protected void setChangeListeners(TestCaseGenerationItem item, TestCaseGenerationView view, TestCaseGenerationResultHandler resultHandler, TestCaseGenerationChoosingStage stage) {
		btAdd.setOnAction(e -> {
			if(updateFormula(item, view, stage)) {
				addFormula(false);
			} else {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
			stage.close();
		});
		
		btCheck.setOnAction(e-> {
			if(updateFormula(item, view, stage)) {
				checkFormula();
			} else {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
			stage.close();
		});
	}
		
	@FXML
	public void cancel() {
		injector.getInstance(TestCaseGenerationChoosingStage.class).close();
	}

}
