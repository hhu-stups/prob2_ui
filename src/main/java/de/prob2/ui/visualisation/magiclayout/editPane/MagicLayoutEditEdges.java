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
import de.prob2.ui.visualisation.magiclayout.MagicEdges;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Spinner;
import javafx.scene.paint.Color;

public class MagicLayoutEditEdges extends MagicLayoutEditPane {

	private ColorPicker textColorPicker;
	private Spinner<Integer> textSizeSpinner;

	@Inject
	public MagicLayoutEditEdges(final StageManager stageManager, final ResourceBundle bundle,
			final CurrentTrace currentTrace) {
		super(stageManager, bundle, currentTrace);
	}

	@FXML
	public void initialize() {
		super.initialize();

		expressionTextArea.setPromptText("{x,y|...}");

		// add Edge specific controls
		textColorPicker = new ColorPicker();
		textSizeSpinner = new Spinner<>(2, 30, 12);
		textSizeSpinner.setEditable(true);

		flowPane.getChildren().addAll(
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.textcolor"), textColorPicker),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.textsize"), textSizeSpinner));

		// add Constants, Variables from Machine as Edge Groups
		currentTrace.addListener((observable, from, to) -> {
			if (to != null && to.getStateSpace() != null) {
				this.listView.getItems().clear();
				this.updateValues();

				StateSpace stateSpace = currentTrace.getStateSpace();

				List<IEvalElement> constantEvalElements = stateSpace.getLoadedMachine().getConstantEvalElements();
				addEvalElementsAsGroups(constantEvalElements);
				
				List<IEvalElement> variableEvalElements = stateSpace.getLoadedMachine().getVariableEvalElements();
				addEvalElementsAsGroups(variableEvalElements);
			}
		});
	}

	@Override
	void updateValues(MagicComponent selectedComponent) {
		super.updateValues(selectedComponent);

		if (selectedComponent != null) {
			MagicEdges selectedEdges = (MagicEdges) selectedComponent;

			textColorPicker.setValue(selectedEdges.getTextColor());
			selectedEdges.textColorProperty().bind(textColorPicker.valueProperty());

			textSizeSpinner.getValueFactory().setValue(selectedEdges.getTextSize());
			selectedEdges.textSizeProperty().bind(textSizeSpinner.valueProperty());
		} else {
			textColorPicker.setValue(Color.BLACK);
			textSizeSpinner.getValueFactory().setValue(12);
		}
	}

	public void addEdges() {
		MagicEdges edges = new MagicEdges("edges");
		super.addMagicComponent(edges);
	}

	public List<MagicEdges> getEdges() {
		List<MagicEdges> edgesList = new ArrayList<>();
		listView.getItems().forEach(comp -> edgesList.add((MagicEdges) comp));
		return edgesList;
	}
}
