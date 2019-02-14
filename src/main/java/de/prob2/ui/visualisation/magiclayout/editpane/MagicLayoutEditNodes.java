package de.prob2.ui.visualisation.magiclayout.editpane;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visualisation.magiclayout.MagicComponent;
import de.prob2.ui.visualisation.magiclayout.MagicGraphI;
import de.prob2.ui.visualisation.magiclayout.MagicLayoutSettings;
import de.prob2.ui.visualisation.magiclayout.MagicNodegroup;
import de.prob2.ui.visualisation.magiclayout.MagicShape;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;

@FXMLInjected
public class MagicLayoutEditNodes extends MagicLayoutEditPane<MagicNodegroup> {

	private class MagicShapeListCell extends ListCell<MagicShape> {
		@Override
		protected void updateItem(MagicShape shape, boolean empty) {
			super.updateItem(shape, empty);
			setText((shape == null || empty) ? "" : bundle.getString(shape.getBundleKey()));
		}
	}

	private CheckBox clusterCheckBox;
	private ComboBox<MagicShape> shapeComboBox;
	private ColorPicker nodeColorPicker;

	@Inject
	public MagicLayoutEditNodes(final StageManager stageManager, final ResourceBundle bundle,
			final CurrentTrace currentTrace, final MagicGraphI magicGraph) {
		super(stageManager, bundle, currentTrace, magicGraph);
	}

	@FXML
	@Override
	public void initialize() {
		super.initialize();

		expressionTextArea.setPromptText("{x|...}");

		// add Node specific controls
		clusterCheckBox = new CheckBox(bundle.getString("visualisation.magicLayout.editPane.labels.cluster"));
		setMargin(clusterCheckBox, new Insets(0, 5, 0, 10));
		if (magicGraph.supportsClustering()) {
			this.getChildren().add(2, clusterCheckBox);
		}

		shapeComboBox = new ComboBox<>();
		shapeComboBox.getItems().setAll(magicGraph.getSupportedShapes());
		shapeComboBox.setCellFactory((ListView<MagicShape> lv) -> new MagicShapeListCell());
		shapeComboBox.setButtonCell(new MagicShapeListCell());
		shapeComboBox.getSelectionModel().selectFirst();

		nodeColorPicker = new ColorPicker(Color.WHITE);

		flowPane.getChildren().addAll(
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.shape"), shapeComboBox),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.color"), nodeColorPicker));

		disableControls(true);
		addMachineElements();
	}

	void addMachineElements() {
		if (currentTrace.getStateSpace() != null) {
			List<String> setNames = currentTrace.getStateSpace().getLoadedMachine().getSetNames();
			setNames.forEach(name -> {
				MagicNodegroup nodegroup = new MagicNodegroup(name, name, true);
				nodegroup.nodeColorProperty().set(Color.hsb(new Random().nextDouble() * 360, 0.2, 1));
				listView.getItems().add(nodegroup);
			});
		}
		updateValues();
	}

	@Override
	void updateValues(MagicComponent selectedComponent) {
		super.updateValues(selectedComponent);

		if (selectedComponent != null) {
			MagicNodegroup selectedNodes = (MagicNodegroup) selectedComponent;

			clusterCheckBox.setSelected(selectedNodes.isCluster());
			selectedNodes.clusterProperty().bind(clusterCheckBox.selectedProperty());

			shapeComboBox.setValue(selectedNodes.getShape());
			selectedNodes.shapeProperty().bind(shapeComboBox.valueProperty());

			nodeColorPicker.setValue(selectedNodes.getNodeColor());
			selectedNodes.nodeColorProperty().bind(nodeColorPicker.valueProperty());
		} else {
			clusterCheckBox.setSelected(false);
			shapeComboBox.getSelectionModel().selectFirst();
			nodeColorPicker.setValue(Color.WHITE);
		}
	}
	
	@Override
	void disableControls(boolean disable) {
		super.disableControls(disable);
		clusterCheckBox.setDisable(disable);
		shapeComboBox.setDisable(disable);
		nodeColorPicker.setDisable(disable);
	}

	public void addNewNodegroup() {
		MagicNodegroup nodes = new MagicNodegroup("nodes");
		int i = 1;
		while (listView.getItems().contains(nodes)) {
			nodes = new MagicNodegroup("nodes" + i);
			i++;
		}
		addMagicComponent(nodes);
	}

	public List<MagicNodegroup> getNodegroups() {
		return new ArrayList<>(listView.getItems());
	}

	@Override
	protected MagicNodegroup getInstance(MagicNodegroup nodes) {
		return new MagicNodegroup(nodes);
	}

	@Override
	public void openLayoutSettings(MagicLayoutSettings layoutSettings) {
		listView.getItems().setAll(layoutSettings.getNodegroups());
	}

}
