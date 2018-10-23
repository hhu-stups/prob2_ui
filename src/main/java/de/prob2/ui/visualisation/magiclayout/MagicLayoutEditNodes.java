package de.prob2.ui.visualisation.magiclayout;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;

public class MagicLayoutEditNodes extends MagicLayoutEditPane {

	@Inject
	public MagicLayoutEditNodes(StageManager stageManager) {
		super(stageManager);
	}

	@FXML
	public void initialize() {
		super.initialize();
		
		// add DummyData
		listView.getItems().addAll("nodes1", "nodes2", "nodes3", "nodes4", "nodes5", "nodes6", "nodes7", "nodes8");

		CheckBox clusterCheckBox = new CheckBox("Cluster");
		setMargin(clusterCheckBox, new Insets(0, 5, 0, 10));
		this.getChildren().add(2, clusterCheckBox);
		
		flowPane.getChildren().addAll(new Button("shape"), new Button("color"));
	}

}
