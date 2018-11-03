package de.prob2.ui.visualisation.magiclayout.editPane;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.animator.command.EvaluateFormulasCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
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
			final CurrentTrace currentTrace) {
		super(stageManager, bundle, currentTrace);
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
		shapeComboBox.getItems().setAll(MagicShape.values());
		shapeComboBox.setCellFactory((ListView<MagicShape> lv) -> new MagicShapeListCell());
		shapeComboBox.setButtonCell(new MagicShapeListCell());
		shapeComboBox.getSelectionModel().selectFirst();

		nodeColorPicker = new ColorPicker();
		flowPane.getChildren().addAll(
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.shape"), shapeComboBox),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.color"), nodeColorPicker));

		// add Sets from Machine as Node Groups
		currentTrace.addListener((observable, from, to) -> {
			if (to != null && to.getStateSpace() != null) {
				this.listView.getItems().clear();
				this.updateValues();
				
				StateSpace stateSpace = currentTrace.getStateSpace();
				
				List<IEvalElement> setEvalElements = stateSpace.getLoadedMachine().getSetEvalElements();
				final EvaluateFormulasCommand setEvalCmd = new EvaluateFormulasCommand(setEvalElements,
						currentTrace.getCurrentState().getId());
				stateSpace.execute(setEvalCmd);
				Map<IEvalElement, AbstractEvalResult> setResultMap = setEvalCmd.getResultMap();
				for(IEvalElement element : setResultMap.keySet()) {
					MagicNodes magicNodes = new MagicNodes(element.toString(), setResultMap.get(element).toString());
					listView.getItems().add(magicNodes);
				}
			}
		});


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

}
