package de.prob2.ui.visualisation.magiclayout.editPane;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visualisation.magiclayout.MagicComponent;
import de.prob2.ui.visualisation.magiclayout.MagicEdgegroup;
import de.prob2.ui.visualisation.magiclayout.MagicGraphI;
import de.prob2.ui.visualisation.magiclayout.MagicLayoutSettings;
import javafx.fxml.FXML;
import javafx.scene.control.Spinner;

public class MagicLayoutEditEdges extends MagicLayoutEditPane<MagicEdgegroup> {
	
	private Spinner<Integer> textSizeSpinner;

	MagicGraphI magicGraph;

	@Inject
	public MagicLayoutEditEdges(final StageManager stageManager, final ResourceBundle bundle,
			final CurrentTrace currentTrace, final MagicGraphI magicGraph) {
		super(stageManager, bundle, currentTrace, magicGraph);
	}

	@FXML
	public void initialize() {
		super.initialize();

		expressionTextArea.setPromptText("{x,y|...}");

		// add Edge specific controls
		textSizeSpinner = new Spinner<>(2, 30, 12);
		textSizeSpinner.setEditable(true);

		flowPane.getChildren().add(
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.textsize"), textSizeSpinner));

		// add Constants, Variables from Machine as Edge Groups
		addMachineElementsAsEdgeGroups();
		currentTrace.addListener((observable, from, to) -> addMachineElementsAsEdgeGroups());
	}

	private void addMachineElementsAsEdgeGroups() {
		StateSpace stateSpace = currentTrace.getStateSpace();

		if (stateSpace != null) {
			List<IEvalElement> constantEvalElements = stateSpace.getLoadedMachine().getConstantEvalElements();
			addEvalElementsAsGroups(constantEvalElements);

			List<IEvalElement> variableEvalElements = stateSpace.getLoadedMachine().getVariableEvalElements();
			addEvalElementsAsGroups(variableEvalElements);
		}
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

	public void addNewEdgegroup() {
		MagicEdgegroup edges = new MagicEdgegroup("edges");
		int i = 1;
		while(listView.getItems().contains(edges)) {
			edges = new MagicEdgegroup("edges" + i);
			i++;
		}
		super.addMagicComponent(edges);
	}

	public List<MagicEdgegroup> getEdgegroups() {
		List<MagicEdgegroup> edgesList = new ArrayList<>();
		listView.getItems().forEach(comp -> edgesList.add((MagicEdgegroup) comp));
		return edgesList;
	}

	@Override
	MagicEdgegroup getInstance(String name, String expression) {
		return new MagicEdgegroup(name, expression);
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
