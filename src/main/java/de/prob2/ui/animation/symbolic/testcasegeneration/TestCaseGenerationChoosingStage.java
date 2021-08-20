package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ResourceBundle;

import javax.inject.Inject;

import de.prob2.ui.internal.StageManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

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
	
	private final ObjectProperty<TestCaseGenerationItem> item;
	
	private final BooleanProperty checkRequested;
	
	@Inject
	private TestCaseGenerationChoosingStage(final StageManager stageManager, final ResourceBundle bundle) {
		super();
		
		this.stageManager = stageManager;
		this.bundle = bundle;
		
		this.item = new SimpleObjectProperty<>(this, "item", null);
		this.checkRequested = new SimpleBooleanProperty(this, "checkRequested", false);
		
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "test_case_generation_choice.fxml");
	}
	
	
	@FXML
	public void initialize() {
		this.itemProperty().addListener((o, from, to) -> {
			if (to != null) {
				this.select(to);
				if(this.getTestCaseGenerationType() == TestCaseGenerationType.MCDC) {
					mcdcInputView.setItem((MCDCItem)to);
				} else if(this.getTestCaseGenerationType() == TestCaseGenerationType.COVERED_OPERATIONS) {
					operationCoverageInputView.setItem((OperationCoverageItem)to);
				}
			}
		});
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
	
	public ObjectProperty<TestCaseGenerationItem> itemProperty() {
		return this.item;
	}
	
	public TestCaseGenerationItem getItem() {
		return this.itemProperty().get();
	}
	
	public void setItem(final TestCaseGenerationItem item) {
		this.itemProperty().set(item);
	}
	
	public ReadOnlyBooleanProperty checkRequestedProperty() {
		return this.checkRequested;
	}
	
	public boolean isCheckRequested() {
		return this.checkRequestedProperty().get();
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
			this.setItem(extractItem());
			this.checkRequested.set(false);
		});
		btCheck.setOnAction(e -> {
			if (!checkValid()) {
				return;
			}
			this.close();
			this.setItem(extractItem());
			this.checkRequested.set(true);
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
	
	public void useChangeButtons() {
		btAdd.setText(bundle.getString("testcase.input.buttons.change"));
		btCheck.setText(bundle.getString("testcase.input.buttons.changeAndGenerate"));
	}
	
	@FXML
	public void cancel() {
		this.close();
		this.setItem(null);
		this.checkRequested.set(false);
	}
}
