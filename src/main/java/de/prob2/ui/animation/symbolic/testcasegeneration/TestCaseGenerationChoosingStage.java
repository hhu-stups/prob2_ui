package de.prob2.ui.animation.symbolic.testcasegeneration;

import com.google.inject.Inject;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class TestCaseGenerationChoosingStage extends Stage {
	@FXML
	private ChoiceBox<TestCaseGenerationType> testChoice;

	@FXML
	private VBox input;

	@FXML
	private MCDCInputView mcdcInputView;

	@FXML
	private OperationCoverageInputView operationCoverageInputView;

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
				this.select(to);
				if (this.getTestCaseGenerationType() == TestCaseGenerationType.MCDC) {
					mcdcInputView.setItem((MCDCItem) to);
				} else if (this.getTestCaseGenerationType() == TestCaseGenerationType.COVERED_OPERATIONS) {
					operationCoverageInputView.setItem((OperationCoverageItem) to);
				}
			}
		});
		input.visibleProperty().bind(testChoice.getSelectionModel().selectedItemProperty().isNotNull());
		testChoice.getItems().setAll(TestCaseGenerationType.values());
		testChoice.setConverter(i18n.translateConverter());
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
		return switch (type) {
			case MCDC -> new MCDCItem(mcdcInputView.getDepth(), mcdcInputView.getLevel());
			case COVERED_OPERATIONS -> new OperationCoverageItem(operationCoverageInputView.getDepth(), operationCoverageInputView.getOperations());
		};
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
