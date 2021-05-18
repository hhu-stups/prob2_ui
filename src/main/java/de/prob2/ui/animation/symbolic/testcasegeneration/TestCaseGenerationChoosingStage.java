package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.StageManager;

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
	
	private final ResourceBundle bundle;
	
	private final TestCaseGenerationItemHandler testCaseGenerationFormulaHandler;
	
	@Inject
	private TestCaseGenerationChoosingStage(final StageManager stageManager, final ResourceBundle bundle, final TestCaseGenerationItemHandler testCaseGenerationFormulaHandler) {
		super();
		
		this.stageManager = stageManager;
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
	
	private void setCheckListeners() {
		btAdd.setOnAction(e -> {
			if (!checkValid()) {
				return;
			}
			this.close();
			testCaseGenerationFormulaHandler.addItem(extractItem());
		});
		btCheck.setOnAction(e -> {
			if (!checkValid()) {
				return;
			}
			this.close();
			final TestCaseGenerationItem newItem = extractItem();
			testCaseGenerationFormulaHandler.addItem(newItem);
			testCaseGenerationFormulaHandler.generateTestCases(newItem);
		});
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
			if (!checkValid()) {
				return;
			}
			//Close stage first so that it does not need to wait for possible Alerts
			this.close();
			final TestCaseGenerationItem newItem = extractItem();
			if (!testCaseGenerationFormulaHandler.replaceItem(item, newItem)) {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.CONFIGURATION);
			}
		});
		
		btCheck.setOnAction(e-> {
			if (!checkValid()) {
				return;
			}
			//Close stage first so that it does not need to wait for possible Alerts
			this.close();
			final TestCaseGenerationItem newItem = extractItem();
			if(testCaseGenerationFormulaHandler.replaceItem(item, newItem)) {
				testCaseGenerationFormulaHandler.generateTestCases(newItem);
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
