package de.prob2.ui.visualisation.magiclayout.editPane;

import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visualisation.magiclayout.MagicComponent;
import de.prob2.ui.visualisation.magiclayout.MagicNodes;
import de.prob2.ui.visualisation.magiclayout.MagicShape;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

public class MagicLayoutEditNodes extends MagicLayoutEditPane {
	
	private class MagicShapeListCell extends ListCell<MagicShape> {
		@Override
		protected void updateItem(MagicShape shape, boolean empty) {
			super.updateItem(shape, empty);
			setText((shape == null || empty)? "" : bundle.getString(shape.getBundleKey()));
		}
	}

	private CheckBox clusterCheckBox;
	private ComboBox<MagicShape> shapeComboBox;
	private ColorPicker nodeColorPicker;

	@Inject
	public MagicLayoutEditNodes(final StageManager stageManager, final ResourceBundle bundle) {
		super(stageManager, bundle);
	}

	@FXML
	public void initialize() {
		super.initialize();

		expressionTextArea.setPromptText("{x|...}");

		// add DummyData
		listView.getItems().addAll(new MagicNodes("nodes1", "{x1|...}"), new MagicNodes("nodes2", "{x2|...}"),
				new MagicNodes("nodes3", "{x3|...}"), new MagicNodes("nodes4", "{x4|...}"),
				new MagicNodes("nodes5", "{x5|...}"), new MagicNodes("nodes6", "{x6|...}"),
				new MagicNodes("nodes7", "{x7|...}"), new MagicNodes("nodes8", "{x8|...}"));

		// add Node specific controls
		clusterCheckBox = new CheckBox(bundle.getString("visualisation.magicLayout.editPane.labels.cluster"));
		setMargin(clusterCheckBox, new Insets(0, 5, 0, 10));
		this.getChildren().add(2, clusterCheckBox);

		shapeComboBox = new ComboBox<>();
		shapeComboBox.getItems().setAll(MagicShape.values());		
		shapeComboBox.setCellFactory((ListView<MagicShape> lv) -> new MagicShapeListCell());
		shapeComboBox.setButtonCell(new MagicShapeListCell());
		shapeComboBox.getSelectionModel().selectFirst();
		
		nodeColorPicker = new ColorPicker();
		flowPane.getChildren().addAll(
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.shape"), shapeComboBox),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.color"), nodeColorPicker));
	}

	@Override
	void updateValues(MagicComponent selectedComponent) {
		super.updateValues(selectedComponent);

		MagicNodes selectedNodes = (MagicNodes) selectedComponent;

		clusterCheckBox.setSelected(selectedNodes.isCluster());
		selectedNodes.clusterProperty().bind(clusterCheckBox.selectedProperty());

		shapeComboBox.setValue(selectedNodes.getShape());
		selectedNodes.shapeProperty().bind(shapeComboBox.valueProperty());

		nodeColorPicker.setValue(selectedNodes.getNodeColor());
		selectedNodes.nodeColorProperty().bind(nodeColorPicker.valueProperty());
	}

	public void addNodes() {
		MagicNodes nodes = new MagicNodes("nodes");
		super.addMagicComponent(nodes);
	}

}
