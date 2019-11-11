package de.prob2.ui.animation.symbolic.testcasegeneration;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

@Singleton
public class TestCaseGenerationChoosingStage extends Stage {
	
	@FXML
	private TestCaseGenerationInput input;
	
	@FXML
	private ChoiceBox<TestCaseExecutionItem> testChoice;
	
	@Inject
	private TestCaseGenerationChoosingStage(final StageManager stageManager) {
		super();
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "test_case_generation_choice.fxml");
	}
	
	
	@FXML
	public void initialize() {
		input.visibleProperty().bind(testChoice.getSelectionModel().selectedItemProperty().isNotNull());
		testChoice.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if(to == null) {
				return;
			}
			input.changeType(to);
			this.sizeToScene();
		});
	}
	
	public TestCaseGenerationType getTestCaseGenerationType() {
		return testChoice.getSelectionModel().getSelectedItem().getExecutionType();
	}
	
	public void select(TestCaseGenerationItem item) {
		testChoice.getItems().forEach(choice -> {
			if(item.getType().equals(choice.getExecutionType())) {
				testChoice.getSelectionModel().select(choice);
			}
		});
	}
	
	public void reset() {
		input.reset();
		testChoice.getSelectionModel().clearSelection();
	}
}
