package de.prob2.ui.visualisation.magiclayout.editPane;

import java.util.ResourceBundle;

import com.google.inject.Inject;

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
	public MagicLayoutEditEdges(final StageManager stageManager, final ResourceBundle bundle) {
		super(stageManager, bundle);
	}

	@FXML
	public void initialize() {
		super.initialize();

		expressionTextArea.setPromptText("{x,y|...}");

		// add DummyData
		listView.getItems().addAll(new MagicEdges("edges1", "{x,y|...}"));

		// add Edge specific controls
		textColorPicker = new ColorPicker();
		textSizeSpinner = new Spinner<>(2, 30, 12);
		textSizeSpinner.setEditable(true);

		flowPane.getChildren().addAll(
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.textcolor"), textColorPicker),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.textsize"), textSizeSpinner));
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

}
