package de.prob2.ui.animation.symbolic.testcasegeneration;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

@FXMLInjected
public class MCDCInputView extends VBox {

	@FXML
	private TextField levelField;

	@FXML
	private TextField depthField;

	@Inject
	private MCDCInputView(final StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "test_case_generation_mcdc.fxml");
	}

	@FXML
	private void initialize() {
		this.reset();
	}

	public String getLevel() {
		return levelField.getText();
	}

	public String getDepth() {
		return depthField.getText();
	}

	public void reset() {
		levelField.setText("2");
		depthField.setText("5");
	}

	public void setItem(TestCaseGenerationItem item) {
		levelField.setText(String.valueOf(item.getMcdcLevel()));
		depthField.setText(String.valueOf(item.getMaxDepth()));
	}

}
