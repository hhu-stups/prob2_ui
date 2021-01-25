package de.prob2.ui.animation.symbolic.testcasegeneration;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;

@FXMLInjected
public class MCDCInputView extends VBox {

	@FXML
	private Spinner<Integer> levelSpinner;

	@FXML
	private Spinner<Integer> depthSpinner;

	@Inject
	private MCDCInputView(final StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "test_case_generation_mcdc.fxml");
	}

	@FXML
	private void initialize() {
		levelSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE));
		depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE));
		this.reset();
	}

	public int getLevel() {
		return levelSpinner.getValue();
	}

	public int getDepth() {
		return depthSpinner.getValue();
	}

	public void reset() {
		levelSpinner.getValueFactory().setValue(2);
		depthSpinner.getValueFactory().setValue(5);
	}

	public void setItem(TestCaseGenerationItem item) {
		levelSpinner.getValueFactory().setValue(item.getMcdcLevel());
		depthSpinner.getValueFactory().setValue(item.getMaxDepth());
	}

}
