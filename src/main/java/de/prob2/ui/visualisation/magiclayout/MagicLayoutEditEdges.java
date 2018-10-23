package de.prob2.ui.visualisation.magiclayout;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

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
		
		flowPane.getChildren().addAll(new Button("font"), new Button("fontsize"));
	}

}
