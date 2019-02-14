package de.prob2.ui.visualisation.magiclayout.editpane;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visualisation.magiclayout.MagicComponent;
import de.prob2.ui.visualisation.magiclayout.MagicEdgegroup;
import de.prob2.ui.visualisation.magiclayout.MagicGraphI;
import de.prob2.ui.visualisation.magiclayout.MagicLayoutSettings;

import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.paint.Color;

@FXMLInjected
public class MagicLayoutEditEdges extends MagicLayoutEditPane<MagicEdgegroup> {

	private Spinner<Integer> textSizeSpinner;

	@Inject
	public MagicLayoutEditEdges(final StageManager stageManager, final ResourceBundle bundle,
			final CurrentTrace currentTrace, final MagicGraphI magicGraph) {
		super(stageManager, bundle, currentTrace, magicGraph);
	}

	@FXML
	@Override
	public void initialize() {
		super.initialize();

		expressionTextArea.setPromptText("{x,y|...}");

		// add Edge specific controls
		textSizeSpinner = new Spinner<>(2, 30, 12);
		textSizeSpinner.setEditable(true);

		flowPane.getChildren().add(
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.textsize"), textSizeSpinner));

		disableControls(true);
		addMachineElements();
	}

	void addMachineElements() {
		StateSpace stateSpace = currentTrace.getStateSpace();
		if (stateSpace != null) {
			List<String> constantNames = stateSpace.getLoadedMachine().getConstantNames();
			constantNames.forEach(name -> {
				MagicEdgegroup edgegroup = new MagicEdgegroup(name, name);
				edgegroup.lineColorProperty().set(Color.hsb(new Random().nextDouble() * 360, 0.7, 0.5));
				listView.getItems().add(edgegroup);
			});
			List<String> variableNames = stateSpace.getLoadedMachine().getVariableNames();
			variableNames.forEach(name -> {
				MagicEdgegroup edgegroup = new MagicEdgegroup(name, name);
				edgegroup.lineColorProperty().set(Color.hsb(new Random().nextDouble() * 360, 0.7, 0.5));

				listView.getItems().add(edgegroup);
			});
		}
		updateValues();
	}

	@Override
	void updateValues(MagicComponent selectedComponent) {
		super.updateValues(selectedComponent);

		if (selectedComponent != null) {
			MagicEdgegroup selectedEdges = (MagicEdgegroup) selectedComponent;

			textSizeSpinner.getValueFactory().setValue(selectedEdges.getTextSize());
			selectedEdges.textSizeProperty().bind(textSizeSpinner.valueProperty());
		} else {
			textSizeSpinner.getValueFactory().setValue(12);
		}
	}
	
	@Override
	void disableControls(boolean disable) {
		super.disableControls(disable);
		textSizeSpinner.setDisable(disable);
	}

	public void addNewEdgegroup() {
		MagicEdgegroup edges = new MagicEdgegroup("edges");
		int i = 1;
		while (listView.getItems().contains(edges)) {
			edges = new MagicEdgegroup("edges" + i);
			i++;
		}
		addMagicComponent(edges);
	}

	public List<MagicEdgegroup> getEdgegroups() {
		return new ArrayList<>(listView.getItems());
	}

	@Override
	protected MagicEdgegroup getInstance(MagicEdgegroup edges) {
		return new MagicEdgegroup(edges);
	}

	@Override
	public void openLayoutSettings(MagicLayoutSettings layoutSettings) {
		listView.getItems().setAll(layoutSettings.getEdgegroups());
	}
}
