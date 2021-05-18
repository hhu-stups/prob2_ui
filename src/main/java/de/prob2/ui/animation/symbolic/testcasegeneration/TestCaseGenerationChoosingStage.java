package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

@Singleton
public class TestCaseGenerationChoosingStage extends Stage {
	@FXML
	private ChoiceBox<TestCaseGenerationType> testChoice;
	
	@FXML
	private VBox input;
	
	@FXML
	private MCDCInputView mcdcInputView;
	
	@FXML
	private OperationCoverageInputView operationCoverageInputView;
	
	@FXML
	private Button btAdd;
	
	@FXML
	private Button btCheck;
	
	private final StageManager stageManager;
	
	private final CurrentProject currentProject;
	
	private final ResourceBundle bundle;
	
	private final TestCaseGenerationItemHandler testCaseGenerationFormulaHandler;
	
	@Inject
	private TestCaseGenerationChoosingStage(final StageManager stageManager, final CurrentProject currentProject, final ResourceBundle bundle, final TestCaseGenerationItemHandler testCaseGenerationFormulaHandler) {
		super();
		
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.bundle = bundle;
		this.testCaseGenerationFormulaHandler = testCaseGenerationFormulaHandler;
		
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "test_case_generation_choice.fxml");
	}
	
	
	@FXML
	public void initialize() {
		setCheckListeners();
		input.visibleProperty().bind(testChoice.getSelectionModel().selectedItemProperty().isNotNull());
		testChoice.getItems().setAll(TestCaseGenerationType.values());
		testChoice.setConverter(new StringConverter<TestCaseGenerationType>() {
			@Override
			public String toString(final TestCaseGenerationType object) {
				return object.getName();
			}
			
			@Override
			public TestCaseGenerationType fromString(final String string) {
				throw new UnsupportedOperationException("Conversion from String not supported");
			}
		});
		testChoice.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if(to == null) {
				return;
			}
			this.changeType(to);
			this.sizeToScene();
		});
		this.sizeToScene();
	}
	
	private boolean checkValid() {
		if (this.getTestCaseGenerationType() == TestCaseGenerationType.COVERED_OPERATIONS && this.operationCoverageInputView.getOperations().isEmpty()) {
			final Alert alert = stageManager.makeAlert(
				Alert.AlertType.ERROR,
				"animation.alerts.testcasegeneration.operations.header",
				"animation.alerts.testcasegeneration.operations.content"
			);
			alert.initOwner(this.getScene().getWindow());
			alert.showAndWait();
			return false;
		}
		return true;
	}
	
	private TestCaseGenerationItem extractItem() {
		final TestCaseGenerationType type = this.getTestCaseGenerationType();
		switch (type) {
			case MCDC:
				return new MCDCItem(mcdcInputView.getDepth(), mcdcInputView.getLevel());
			
			case COVERED_OPERATIONS:
				return new OperationCoverageItem(operationCoverageInputView.getDepth(), operationCoverageInputView.getOperations());
			
			default:
				throw new AssertionError("Unhandled test case generation type: " + type);
		}
	}
	
	private boolean updateItem(TestCaseGenerationItem item) {
		if (!checkValid()) {
			return true;
		}
		Machine currentMachine = currentProject.getCurrentMachine();
		final TestCaseGenerationItem newItem = extractItem();
		if(currentMachine.getTestCases().stream().noneMatch(newItem::settingsEqual)) {
			currentMachine.getTestCases().set(currentMachine.getTestCases().indexOf(item), newItem);
			return true;
		}
		return false;
	}
	
	private void setCheckListeners() {
		btAdd.setOnAction(e -> addItem());
		btCheck.setOnAction(e -> checkItem());
	}
	
	public void changeType(final TestCaseGenerationType type) {
		input.getChildren().removeAll(mcdcInputView, operationCoverageInputView);
		switch (type) {
			case MCDC:
				input.getChildren().add(0, mcdcInputView);
				break;
			case COVERED_OPERATIONS:
				input.getChildren().add(0, operationCoverageInputView);
				break;
			default:
				throw new AssertionError("Unhandled type: " + type);
		}
	}
	
	public TestCaseGenerationType getTestCaseGenerationType() {
		return testChoice.getSelectionModel().getSelectedItem();
	}
	
	public void select(TestCaseGenerationItem item) {
		testChoice.getSelectionModel().select(item.getType());
	}
	
	public void reset() {
		btAdd.setText(bundle.getString("common.buttons.add"));
		btCheck.setText(bundle.getString("testcase.input.buttons.addAndGenerate"));
		setCheckListeners();
		mcdcInputView.reset();
		operationCoverageInputView.reset();
		testChoice.getSelectionModel().clearSelection();
	}

	public void checkItem() {
		addItem();
		testCaseGenerationFormulaHandler.generateTestCases(extractItem());
		this.close();
	}

	public void addItem() {
		if (!checkValid()) {
			return;
		}
		testCaseGenerationFormulaHandler.addItem(extractItem());
		this.close();
	}
	
	public void changeItem(TestCaseGenerationItem item, TestCaseGenerationResultHandler resultHandler) {
		btAdd.setText(bundle.getString("testcase.input.buttons.change"));
		btCheck.setText(bundle.getString("testcase.input.buttons.changeAndGenerate"));
		setChangeListeners(item, resultHandler);
		this.select(item);
		if(this.getTestCaseGenerationType() == TestCaseGenerationType.MCDC) {
			mcdcInputView.setItem((MCDCItem)item);
		} else if(this.getTestCaseGenerationType() == TestCaseGenerationType.COVERED_OPERATIONS) {
			operationCoverageInputView.setItem((OperationCoverageItem)item);
		}
		this.show();
	}
	
	private void setChangeListeners(TestCaseGenerationItem item, TestCaseGenerationResultHandler resultHandler) {
		btAdd.setOnAction(e -> {
			//Close stage first so that it does not need to wait for possible Alerts
			this.close();
			if(updateItem(item)) {
				addItem();
			} else {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.CONFIGURATION);
			}
		});
		
		btCheck.setOnAction(e-> {
			//Close stage first so that it does not need to wait for possible Alerts
			this.close();
			if(updateItem(item)) {
				checkItem();
			} else {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.CONFIGURATION);
			}
		});
	}
		
	@FXML
	public void cancel() {
		this.close();
	}
}
