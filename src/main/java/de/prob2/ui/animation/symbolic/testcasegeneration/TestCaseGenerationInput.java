package de.prob2.ui.animation.symbolic.testcasegeneration;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.ResourceBundle;

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
	
	private final CurrentProject currentProject;
	
	@Inject
	private TestCaseGenerationInput(final StageManager stageManager, final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle,
			final TestCaseGenerationItemHandler testCaseGenerationFormulaHandler, final TestCaseGenerationSettingsHandler testCaseGenerationSettingsHandler) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.injector = injector;
		this.bundle = bundle;
		this.testCaseGenerationFormulaHandler = testCaseGenerationFormulaHandler;
		this.testCaseGenerationSettingsHandler = testCaseGenerationSettingsHandler;
		stageManager.loadFXML(this, "test_case_generation_input.fxml");
	}
	
	@FXML
	public void initialize() {
		setCheckListeners();
	}
	
	private boolean updateItem(TestCaseGenerationItem item, TestCaseGenerationChoosingStage choosingStage) {
		Machine currentMachine = currentProject.getCurrentMachine();
		TestCaseGenerationType type = choosingStage.getTestCaseGenerationType();
		int maxDepth = Integer.parseInt(testCaseGenerationSettingsHandler.extractDepth(choosingStage, mcdcInputView, operationCoverageInputView));
		boolean valid = testCaseGenerationSettingsHandler.isValid(choosingStage, mcdcInputView, operationCoverageInputView);
		TestCaseGenerationItem newItem;
		if (type == TestCaseGenerationType.MCDC) {
			newItem = new TestCaseGenerationItem(maxDepth, Integer.parseInt(mcdcInputView.getLevel()));
		} else if (type == TestCaseGenerationType.COVERED_OPERATIONS) {
			newItem = new TestCaseGenerationItem(maxDepth, operationCoverageInputView.getOperations());
		} else {
			throw new AssertionError("Unhandled type: " + type);
		}
		if(currentMachine.getTestCases().stream().noneMatch(newItem::settingsEqual)) {
			if(valid) {
				currentMachine.getTestCases().set(currentMachine.getTestCases().indexOf(item), newItem);
			}
			return true;
		}
		return false;
	}
	
	private void setCheckListeners() {
		btAdd.setOnAction(e -> addItem());
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
		addItem();
		switch (testCaseGenerationType) {
			case MCDC: {
				if(!testCaseGenerationSettingsHandler.checkMCDCSettings(mcdcInputView.getLevel(), mcdcInputView.getDepth())) {
					return;
				}
				item = new TestCaseGenerationItem(Integer.parseInt(mcdcInputView.getDepth()), Integer.parseInt(mcdcInputView.getLevel()));
				testCaseGenerationFormulaHandler.generateTestCases(item);
				break;
			}
			case COVERED_OPERATIONS: {
				if(!testCaseGenerationSettingsHandler.checkOperationCoverageSettings(operationCoverageInputView.getOperations(), operationCoverageInputView.getDepth())) {
					return;
				}
				item = new TestCaseGenerationItem(Integer.parseInt(operationCoverageInputView.getDepth()), operationCoverageInputView.getOperations());
				testCaseGenerationFormulaHandler.generateTestCases(item);
				break;
			}
			default:
				break;
		}
		injector.getInstance(TestCaseGenerationChoosingStage.class).close();
	}

	public void addItem() {
		TestCaseGenerationType type = injector.getInstance(TestCaseGenerationChoosingStage.class).getTestCaseGenerationType();
		switch(type) {
			case MCDC: {
				if(!testCaseGenerationSettingsHandler.checkMCDCSettings(mcdcInputView.getLevel(), mcdcInputView.getDepth())) {
					final Alert alert = stageManager.makeAlert(Alert.AlertType.ERROR,
							"animation.alerts.testcasegeneration.invalid",
							"animation.alerts.testcasegeneration.mcdc.invalid");
					alert.initOwner(this.getScene().getWindow());
					alert.showAndWait();
					return;
				}
				testCaseGenerationFormulaHandler.addItem(Integer.parseInt(mcdcInputView.getDepth()), Integer.parseInt(mcdcInputView.getLevel()));
				break;
			}
			case COVERED_OPERATIONS: {
				List<String> operations = operationCoverageInputView.getOperations();
				if(operations.isEmpty()) {
					final Alert alert = stageManager.makeAlert(Alert.AlertType.ERROR,
							"animation.alerts.testcasegeneration.operations.header",
							"animation.alerts.testcasegeneration.operations.content");
					alert.initOwner(this.getScene().getWindow());
					alert.showAndWait();
					return;
				}
				if(!testCaseGenerationSettingsHandler.checkInteger(operationCoverageInputView.getDepth())) {
					final Alert alert = stageManager.makeAlert(Alert.AlertType.ERROR,
							"animation.alerts.testcasegeneration.invalid",
							"animation.alerts.testcasegeneration.coveredoperations.invalid");
					alert.initOwner(this.getScene().getWindow());
					alert.showAndWait();
					return;
				}
				testCaseGenerationFormulaHandler.addItem(Integer.parseInt(operationCoverageInputView.getDepth()), operations);
				break;
			}
			default:
				break;
		}
		injector.getInstance(TestCaseGenerationChoosingStage.class).close();
	}
	
	public void changeItem(TestCaseGenerationItem item, TestCaseGenerationResultHandler resultHandler, TestCaseGenerationChoosingStage stage) {
		btAdd.setText(bundle.getString("testcase.input.buttons.change"));
		btCheck.setText(bundle.getString("testcase.input.buttons.changeAndGenerate"));
		setChangeListeners(item, resultHandler, stage);
		stage.select(item);
		if(stage.getTestCaseGenerationType() == TestCaseGenerationType.MCDC) {
			mcdcInputView.setItem(item);
		} else if(stage.getTestCaseGenerationType() == TestCaseGenerationType.COVERED_OPERATIONS) {
			operationCoverageInputView.setItem(item);
		}
		stage.show();
	}
	
	private void setChangeListeners(TestCaseGenerationItem item, TestCaseGenerationResultHandler resultHandler, TestCaseGenerationChoosingStage stage) {
		btAdd.setOnAction(e -> {
			//Close stage first so that it does not need to wait for possible Alerts
			stage.close();
			if(updateItem(item, stage)) {
				addItem();
			} else {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.CONFIGURATION);
			}
		});
		
		btCheck.setOnAction(e-> {
			//Close stage first so that it does not need to wait for possible Alerts
			stage.close();
			if(updateItem(item, stage)) {
				checkItem();
			} else {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.CONFIGURATION);
			}
		});
	}
		
	@FXML
	public void cancel() {
		injector.getInstance(TestCaseGenerationChoosingStage.class).close();
	}

}
