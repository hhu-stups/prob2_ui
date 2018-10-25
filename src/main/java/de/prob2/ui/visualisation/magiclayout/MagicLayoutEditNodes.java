package de.prob2.ui.visualisation.magiclayout;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;

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

		// add Node specific controls
		CheckBox clusterCheckBox = new CheckBox("Cluster");
		setMargin(clusterCheckBox, new Insets(0, 5, 0, 10));
		this.getChildren().add(2, clusterCheckBox);
		
		ComboBox<String> shapeComboBox = new ComboBox<>();
		shapeComboBox.getItems().addAll("rectangle", "circle", "triangle");
		shapeComboBox.getSelectionModel().selectFirst();
		flowPane.getChildren().addAll(wrapInVBox("Shape:", shapeComboBox), wrapInVBox("Color:", new ColorPicker()));
	}

	

}
