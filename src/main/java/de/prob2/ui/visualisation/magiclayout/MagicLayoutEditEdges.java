package de.prob2.ui.visualisation.magiclayout;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Spinner;

public class MagicLayoutEditEdges extends MagicLayoutEditPane {

	@Inject
	public MagicLayoutEditEdges(StageManager stageManager) {
		super(stageManager);
	}

	@FXML
	public void initialize() {
		super.initialize();

		// add DummyData
		listView.getItems().addAll("edges1");

		// add Edge specific controls
		Spinner<Integer> textSizeSpinner = new Spinner<>(2, 30, 12);
		
		flowPane.getChildren().addAll(wrapInVBox("Textcolor:", new ColorPicker()),
				wrapInVBox("Textsize:", textSizeSpinner));
	}

}
