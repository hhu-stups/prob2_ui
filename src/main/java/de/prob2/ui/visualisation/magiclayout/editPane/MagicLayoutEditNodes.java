package de.prob2.ui.visualisation.magiclayout.editPane;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.IEvalElement;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visualisation.magiclayout.MagicComponent;
import de.prob2.ui.visualisation.magiclayout.MagicGraphI;
import de.prob2.ui.visualisation.magiclayout.MagicNodes;
import de.prob2.ui.visualisation.magiclayout.MagicShape;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;

public class MagicLayoutEditNodes extends MagicLayoutEditPane {

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
	public void initialize() {
		super.initialize();

		expressionTextArea.setPromptText("{x|...}");

		// add Node specific controls
		clusterCheckBox = new CheckBox(bundle.getString("visualisation.magicLayout.editPane.labels.cluster"));
		setMargin(clusterCheckBox, new Insets(0, 5, 0, 10));
		this.getChildren().add(2, clusterCheckBox);

		shapeComboBox = new ComboBox<>();
		shapeComboBox.getItems().setAll(this.magicGraph.getPossibleShapes());
		shapeComboBox.setCellFactory((ListView<MagicShape> lv) -> new MagicShapeListCell());
		shapeComboBox.setButtonCell(new MagicShapeListCell());
		shapeComboBox.getSelectionModel().selectFirst();

		nodeColorPicker = new ColorPicker(Color.WHITE);
		
		flowPane.getChildren().addAll(
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.shape"), shapeComboBox),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.color"), nodeColorPicker));

		// add Sets from Machine as Node Groups
		addMachineElementsAsNodeGroups();
		currentTrace.addListener((observable, from, to) -> addMachineElementsAsNodeGroups());
	}

	private void addMachineElementsAsNodeGroups() {
		if (currentTrace.getStateSpace() != null) {
			List<IEvalElement> setEvalElements = currentTrace.getStateSpace().getLoadedMachine().getSetEvalElements();
			addEvalElementsAsGroups(setEvalElements);
		}
	}

	@Override
	void updateValues(MagicComponent selectedComponent) {
		super.updateValues(selectedComponent);

		if (selectedComponent != null) {
			MagicNodes selectedNodes = (MagicNodes) selectedComponent;

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

	public void addNodes() {
		MagicNodes nodes = new MagicNodes("nodes");
		super.addMagicComponent(nodes);
	}

	public List<MagicNodes> getNodes() {
		List<MagicNodes> nodesList = new ArrayList<>();
		listView.getItems().forEach(comp -> nodesList.add((MagicNodes) comp));
		return nodesList;
	}

}
