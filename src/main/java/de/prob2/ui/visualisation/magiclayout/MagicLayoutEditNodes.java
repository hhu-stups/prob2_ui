package de.prob2.ui.visualisation.magiclayout;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;

public class MagicLayoutEditNodes extends MagicLayoutEditPane {
	
	private CheckBox clusterCheckBox;
	private ComboBox<String> shapeComboBox;
	private ColorPicker nodeColorPicker;

	@Inject
	public MagicLayoutEditNodes(StageManager stageManager) {
		super(stageManager);
	}

	@FXML
	public void initialize() {
		super.initialize();
		
		expressionTextArea.setPromptText("{x|...}");

		// add DummyData
		listView.getItems().addAll(new MagicNode("nodes1", "{x1|...}"), new MagicNode("nodes2", "{x2|...}"),
				new MagicNode("nodes3", "{x3|...}"), new MagicNode("nodes4", "{x4|...}"),
				new MagicNode("nodes5", "{x5|...}"), new MagicNode("nodes6", "{x6|...}"),
				new MagicNode("nodes7", "{x7|...}"), new MagicNode("nodes8", "{x8|...}"));
		
		// add Node specific controls
		clusterCheckBox = new CheckBox("Cluster");
		setMargin(clusterCheckBox, new Insets(0, 5, 0, 10));
		this.getChildren().add(2, clusterCheckBox);

		shapeComboBox = new ComboBox<>();
		shapeComboBox.getItems().addAll("rectangle", "circle", "triangle");
		shapeComboBox.getSelectionModel().selectFirst();
		nodeColorPicker = new ColorPicker();
		flowPane.getChildren().addAll(wrapInVBox("Shape:", shapeComboBox), wrapInVBox("Color:", nodeColorPicker));
	}
	
	@Override
	void updateValues(MagicComponent selectedComponent) {
		super.updateValues(selectedComponent);
		
		MagicNode selectedNode = (MagicNode) selectedComponent;
		
		clusterCheckBox.setSelected(selectedNode.isCluster());
		selectedNode.clusterProperty().bind(clusterCheckBox.selectedProperty());
		
		shapeComboBox.setValue(selectedNode.getShape());
		selectedNode.shapeProperty().bind(shapeComboBox.valueProperty());
		
		nodeColorPicker.setValue(selectedNode.getNodeColor());
		selectedNode.nodeColorProperty().bind(nodeColorPicker.valueProperty());
	}

}
