package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.statespace.LoadedMachine;
import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

@FXMLInjected
@Singleton
public class TestCaseGenerationInput extends VBox {
	
	
	private final TestCaseGenerationItemHandler testCaseGenerationFormulaHandler;
	
	private final TestCaseGenerationSettingsHandler testCaseGenerationSettingsHandler;
	
	@FXML
	private Button btAdd;
	
	@FXML
	private Button btCheck;
	
	@FXML
	private MCDCInputView mcdcInputView;

	@FXML
	private OperationCoverageInputView operationCoverageInputView;

	private final StageManager stageManager;

	private final Injector injector;
	
	private final ResourceBundle bundle;
	
	private final CurrentTrace currentTrace;
	
	private ArrayList<String> events;
	
	private final CurrentProject currentProject;
	
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
	
	private void update() {
		events.clear();
		if (currentTrace.get() != null) {
			final LoadedMachine loadedMachine = currentTrace.getStateSpace().getLoadedMachine();
			if (loadedMachine != null) {
				events.addAll(loadedMachine.getOperationNames());
			}
		}
		operationCoverageInputView.setTable(events);
	}

	private boolean updateItem(TestCaseGenerationItem item, TestCaseGenerationView view, TestCaseGenerationChoosingStage choosingStage) {
		Machine currentMachine = currentProject.getCurrentMachine();
		String formula = extractItem(choosingStage);
		TestCaseGenerationType type = choosingStage.getTestCaseGenerationType();
		int maxDepth = Integer.parseInt(testCaseGenerationSettingsHandler.extractDepth(choosingStage, mcdcInputView, operationCoverageInputView));
		Map<String, Object> additionalInformation = testCaseGenerationSettingsHandler.extractAdditionalInformation(choosingStage, mcdcInputView, operationCoverageInputView);
		boolean valid = testCaseGenerationSettingsHandler.isValid(choosingStage, mcdcInputView, operationCoverageInputView);
		TestCaseGenerationItem newItem = new TestCaseGenerationItem(formula, type, additionalInformation);
		if(!currentMachine.getTestCases().contains(newItem)) {
			if(valid) {
				item.setData(formula, type.getName(), "", type, maxDepth, additionalInformation);
				item.initialize();
				view.refresh();
			}
			return true;
		}
		return false;
	}
	
	private void setCheckListeners() {
		btAdd.setOnAction(e -> addItem(false));
		btCheck.setOnAction(e -> checkItem());
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
		btCheck.setText(bundle.getString("testcase.input.buttons.addAndGenerate"));
		setCheckListeners();
		mcdcInputView.reset();
		operationCoverageInputView.reset();
	}

	public void checkItem() {
		TestCaseGenerationType testCaseGenerationType = injector.getInstance(TestCaseGenerationChoosingStage.class).getTestCaseGenerationType();
		TestCaseGenerationItem item = null;
		addItem(true);
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

	public void addItem(boolean checking) {
		TestCaseGenerationType type = injector.getInstance(TestCaseGenerationChoosingStage.class).getTestCaseGenerationType();
		switch(type) {
			case MCDC: {
				if(!testCaseGenerationSettingsHandler.checkMCDCSettings(mcdcInputView.getLevel(), mcdcInputView.getDepth())) {
					stageManager.makeAlert(Alert.AlertType.ERROR,
							"animation.alerts.testcasegeneration.invalid",
							"animation.alerts.testcasegeneration.mcdc.invalid")
							.showAndWait();
					return;
				}
				testCaseGenerationFormulaHandler.addItem(Integer.parseInt(mcdcInputView.getDepth()), Integer.parseInt(mcdcInputView.getLevel()), checking);
				break;
			}
			case COVERED_OPERATIONS: {
				List<String> operations = operationCoverageInputView.getOperations();
				if(operations.isEmpty()) {
					stageManager.makeAlert(Alert.AlertType.ERROR,
							"animation.alerts.testcasegeneration.operations.header",
							"animation.alerts.testcasegeneration.operations.content").showAndWait();
					return;
				}
				if(!testCaseGenerationSettingsHandler.checkInteger(operationCoverageInputView.getDepth())) {
					stageManager.makeAlert(Alert.AlertType.ERROR,
							"animation.alerts.testcasegeneration.invalid",
							"animation.alerts.testcasegeneration.coveredoperations.invalid")
							.showAndWait();
					return;
				}
				testCaseGenerationFormulaHandler.addItem(Integer.parseInt(operationCoverageInputView.getDepth()), operations, checking);
				break;
			}
			default:
				break;
		}
		injector.getInstance(TestCaseGenerationChoosingStage.class).close();
	}
	
	private String extractItem(TestCaseGenerationChoosingStage choosingStage) {
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
	
	public void changeItem(TestCaseGenerationItem item, TestCaseGenerationView view, TestCaseGenerationResultHandler resultHandler, TestCaseGenerationChoosingStage stage) {
		btAdd.setText(bundle.getString("testcase.input.buttons.change"));
		btCheck.setText(bundle.getString("testcase.input.buttons.changeAndGenerate"));
		setChangeListeners(item, view, resultHandler, stage);
		stage.select(item);
		if(stage.getTestCaseGenerationType() == TestCaseGenerationType.MCDC) {
			mcdcInputView.setItem(item);
		} else if(stage.getTestCaseGenerationType() == TestCaseGenerationType.COVERED_OPERATIONS) {
			operationCoverageInputView.setItem(item);
		}
		stage.show();
	}
	
	private void setChangeListeners(TestCaseGenerationItem item, TestCaseGenerationView view, TestCaseGenerationResultHandler resultHandler, TestCaseGenerationChoosingStage stage) {
		btAdd.setOnAction(e -> {
			if(updateItem(item, view, stage)) {
				addItem(false);
			} else {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.CONFIGURATION);
			}
			stage.close();
		});
		
		btCheck.setOnAction(e-> {
			if(updateItem(item, view, stage)) {
				checkItem();
			} else {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.CONFIGURATION);
			}
			stage.close();
		});
	}
		
	@FXML
	public void cancel() {
		injector.getInstance(TestCaseGenerationChoosingStage.class).close();
	}

}
