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
		levelSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, 2));
		depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 5));
		levelSpinner.getEditor().textProperty().addListener((observable, from, to) -> {
			if(to.isEmpty()) {
				levelSpinner.getEditor().setText("0");
			} else if(!to.matches("\\d+")){
				levelSpinner.getEditor().setText(from);
			}
		});
		depthSpinner.getEditor().textProperty().addListener((observable, from, to) -> {
			if(to.isEmpty()) {
				depthSpinner.getEditor().setText("1");
			} else if(!to.matches("[1-9]\\d*")){
				depthSpinner.getEditor().setText(from);
			}
		});
	}

	public int getLevel() {
		return levelSpinner.getValue();
	}

	public int getDepth() {
		return depthSpinner.getValue();
	}

	public void setItem(MCDCItem item) {
		levelSpinner.getValueFactory().setValue(item.getLevel());
		depthSpinner.getValueFactory().setValue(item.getMaxDepth());
	}

}
