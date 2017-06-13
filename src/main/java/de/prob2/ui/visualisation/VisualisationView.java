package de.prob2.ui.visualisation;

import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.StateSpace;
import de.prob2.ui.commands.GetImagesForMachineCommand;
import de.prob2.ui.commands.GetImagesForStateCommand;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

@Singleton
public class VisualisationView extends AnchorPane {
	@FXML
	private StackPane probLogoStackPane;
	@FXML
	private GridPane visualisationGridPane;

	private final CurrentTrace currentTrace;

	@Inject
	public VisualisationView(final CurrentTrace currentTrace, final StageManager stageManager) {
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this, "visualisation_view.fxml");
	}

	@FXML
	public void initialize() {
		currentTrace.currentStateProperty().addListener((observable, from, to) -> {
			if (to != null) {
				StateSpace stateSpace = to.getStateSpace();

				GetImagesForMachineCommand getImagesForMachineCommand = new GetImagesForMachineCommand();
				stateSpace.execute(getImagesForMachineCommand);
				Map<Integer, String> images = getImagesForMachineCommand.getImages();

				if (!images.isEmpty()) {
					probLogoStackPane.setVisible(false);
					visualisationGridPane.getChildren().clear();
					GetImagesForStateCommand getImagesForStateCommand = new GetImagesForStateCommand(to.getId());
					stateSpace.execute(getImagesForStateCommand);
					int[][] imageMatrix = getImagesForStateCommand.getMatrix();
					int rowNr = getImagesForStateCommand.getRows();
					int columnNr = getImagesForStateCommand.getColumns();
					for (int r = 0; r < rowNr; r++) {
						for (int c = 0; c < columnNr; c++) {
							Label label = new Label(Integer.toString(imageMatrix[r][c]));
							visualisationGridPane.add(label, c, r);
						}
					}
				} else {
					probLogoStackPane.setVisible(true);
					visualisationGridPane.getChildren().clear();
				}
			} else {
				probLogoStackPane.setVisible(true);
				visualisationGridPane.getChildren().clear();
			}
		});

	}
}
