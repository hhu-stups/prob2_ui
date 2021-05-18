package de.prob2.ui.animation.symbolic.testcasegeneration;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

@Singleton
public class TestCaseGenerationChoosingStage extends Stage {
	
	@FXML
	private TestCaseGenerationInput input;
	
	@FXML
	private ChoiceBox<TestCaseGenerationType> testChoice;
	
	@Inject
	private TestCaseGenerationChoosingStage(final StageManager stageManager) {
		super();
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "test_case_generation_choice.fxml");
	}
	
	
	@FXML
	public void initialize() {
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
			input.changeType(to);
			this.sizeToScene();
		});
		this.sizeToScene();
	}
	
	public TestCaseGenerationType getTestCaseGenerationType() {
		return testChoice.getSelectionModel().getSelectedItem();
	}
	
	public void select(TestCaseGenerationItem item) {
		testChoice.getSelectionModel().select(item.getType());
	}
	
	public void reset() {
		input.reset();
		testChoice.getSelectionModel().clearSelection();
	}
}
