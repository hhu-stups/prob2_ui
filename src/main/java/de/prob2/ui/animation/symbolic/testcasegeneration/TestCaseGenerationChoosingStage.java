package de.prob2.ui.animation.symbolic.testcasegeneration;

import com.google.inject.Inject;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class TestCaseGenerationChoosingStage extends Stage {
	@FXML
	private ChoiceBox<ValidationTaskType<?>> testChoice;

	@FXML
	private VBox input;

	@FXML
	private MCDCInputView mcdcInputView;

	@FXML
	private OperationCoverageInputView operationCoverageInputView;

	@FXML
	private TextField idTextField;

	private final StageManager stageManager;

	private final I18n i18n;

	private final ObjectProperty<TestCaseGenerationItem> item;

	@Inject
	private TestCaseGenerationChoosingStage(final StageManager stageManager, final I18n i18n) {
		super();

		this.stageManager = stageManager;
		this.i18n = i18n;

		this.item = new SimpleObjectProperty<>(this, "item", null);

		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "test_case_generation_choice.fxml");
	}


	@FXML
	public void initialize() {
		this.itemProperty().addListener((o, from, to) -> {
			if (to != null) {
				testChoice.getSelectionModel().select(to.getTaskType());
				idTextField.setText(to.getId() == null ? "" : to.getId());
				if (to instanceof MCDCItem mcdcItem) {
					mcdcInputView.setItem(mcdcItem);
				} else if (to instanceof OperationCoverageItem operationCoverageItem) {
					operationCoverageInputView.setItem(operationCoverageItem);
				} else {
					throw new AssertionError("Unhandled test case generation type: " + item.getClass());
				}
			}
		});
		input.visibleProperty().bind(testChoice.getSelectionModel().selectedItemProperty().isNotNull());
		testChoice.setConverter(new StringConverter<>() {
			@Override
			public String toString(ValidationTaskType<?> object) {
				if (object == null) {
					return "";
				} else if (BuiltinValidationTaskTypes.TEST_CASE_GENERATION_MCDC.equals(object)) {
					return i18n.translate("animation.testcase.type.mcdc");
				} else if (BuiltinValidationTaskTypes.TEST_CASE_GENERATION_OPERATION_COVERAGE.equals(object)) {
					return i18n.translate("animation.testcase.type.coveredOperations");
				} else {
					return object.getKey();
				}
			}

			@Override
			public ValidationTaskType<?> fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String to ValidationTaskType not supported");
			}
		});
		testChoice.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if (to == null) {
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

	private boolean checkValid() {
		if (BuiltinValidationTaskTypes.TEST_CASE_GENERATION_OPERATION_COVERAGE.equals(testChoice.getValue()) && this.operationCoverageInputView.getOperations().isEmpty()) {
			final Alert alert = stageManager.makeAlert(
					Alert.AlertType.ERROR,
					"animation.testcase.choice.noOperationsSelected.header",
					"animation.testcase.choice.noOperationsSelected.content"
			);
			alert.initOwner(this.getScene().getWindow());
			alert.showAndWait();
			return false;
		}
		return true;
	}

	private TestCaseGenerationItem extractItem() {
		ValidationTaskType<?> type = testChoice.getValue();
		String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
		if (BuiltinValidationTaskTypes.TEST_CASE_GENERATION_MCDC.equals(type)) {
			return new MCDCItem(id, mcdcInputView.getDepth(), mcdcInputView.getLevel());
		} else if (BuiltinValidationTaskTypes.TEST_CASE_GENERATION_OPERATION_COVERAGE.equals(type)) {
			return new OperationCoverageItem(id, operationCoverageInputView.getDepth(), operationCoverageInputView.getOperations());
		} else {
			throw new AssertionError("Unhandled test case generation type: " + item.getClass());
		}
	}

	public void changeType(ValidationTaskType<?> type) {
		input.getChildren().removeAll(mcdcInputView, operationCoverageInputView);
		if (BuiltinValidationTaskTypes.TEST_CASE_GENERATION_MCDC.equals(type)) {
			input.getChildren().add(0, mcdcInputView);
		} else if (BuiltinValidationTaskTypes.TEST_CASE_GENERATION_OPERATION_COVERAGE.equals(type)) {
			input.getChildren().add(0, operationCoverageInputView);
		} else {
			throw new AssertionError("Unhandled type: " + type);
		}
	}

	@FXML
	private void ok() {
		if (!checkValid()) {
			return;
		}
		this.close();
		this.setItem(extractItem());
	}

	@FXML
	public void cancel() {
		this.close();
		this.setItem(null);
	}
}
