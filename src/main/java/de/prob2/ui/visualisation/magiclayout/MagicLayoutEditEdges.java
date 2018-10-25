package de.prob2.ui.visualisation.magiclayout;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Spinner;

public class MagicLayoutEditEdges extends MagicLayoutEditPane {

	private ColorPicker textColorPicker;
	private Spinner<Integer> textSizeSpinner;
	
	@Inject
	public MagicLayoutEditEdges(StageManager stageManager) {
		super(stageManager);
	}

	@FXML
	public void initialize() {
		super.initialize();
		
		expressionTextArea.setPromptText("{x,y|...}");

		// add DummyData
		listView.getItems().addAll(new MagicEdge("edges1", "{x,y|...}"));

		// add Edge specific controls
		textColorPicker = new ColorPicker();
		textSizeSpinner = new Spinner<>(2, 30, 12);
		textSizeSpinner.setEditable(true);
		
		flowPane.getChildren().addAll(wrapInVBox("Textcolor:", textColorPicker),
				wrapInVBox("Textsize:", textSizeSpinner));
	}
	
	@Override
	void updateValues(MagicComponent selectedComponent) {
		super.updateValues(selectedComponent);
		
		MagicEdge selectedEdge = (MagicEdge) selectedComponent;
		
		textColorPicker.setValue(selectedEdge.getTextColor());
		selectedEdge.textColorProperty().bind(textColorPicker.valueProperty());
		
		textSizeSpinner.getValueFactory().setValue(selectedEdge.getTextSize());
		selectedEdge.textSizeProperty().bind(textSizeSpinner.valueProperty());
	}

}
