package de.prob2.ui.visualisation.magiclayout.editPane;

import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visualisation.magiclayout.MagicComponent;
import de.prob2.ui.visualisation.magiclayout.MagicEdge;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Spinner;

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
		listView.getItems().addAll(new MagicEdge("edges1", "{x,y|...}"));

		// add Edge specific controls
		textColorPicker = new ColorPicker();
		textSizeSpinner = new Spinner<>(2, 30, 12);
		textSizeSpinner.setEditable(true);
		
		flowPane.getChildren().addAll(wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.textcolor"), textColorPicker),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.textsize"), textSizeSpinner));
	}
	
	@Override
	void updateValues(MagicComponent selectedComponent) {
		super.updateValues(selectedComponent);
		
		MagicEdge selectedEdge = (MagicEdge) selectedComponent;
		
		textColorPicker.setValue(selectedEdge.getTextColor());
		selectedEdge.textColorProperty().bind(textColorPicker.valueProperty());
		
		textSizeSpinner.getValueFactory().setValue(selectedEdge.getTextSize());
		selectedEdge.textSizeProperty().bind(textSizeSpinner.valueProperty());
	}

}
