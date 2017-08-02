package de.prob2.ui.visualisation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;

@Singleton
public class VisualisationView extends AnchorPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(VisualisationView.class);

	@FXML
	private StackPane probLogoStackPane;
	@FXML
	private ScrollPane visualisationScrollPane;
	@FXML
	private StateVisualisationView currentStateVisualisation;
	@FXML
	private StateVisualisationView previousStateVisualisation;
	@FXML
	private VBox previousStateVBox;

	private final CurrentTrace currentTrace;
	private final StageManager stageManager;

	@Inject
	public VisualisationView(final CurrentTrace currentTrace, final StageManager stageManager) {
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "visualisation_view.fxml");
	}

	@FXML
	public void initialize() {
		visualisationScrollPane.visibleProperty().bind(probLogoStackPane.visibleProperty().not());
		probLogoStackPane.visibleProperty().bind(currentStateVisualisation.visualisationPossibleProperty().not());
		previousStateVBox.managedProperty().bind(previousStateVisualisation.visualisationPossibleProperty());
		previousStateVBox.visibleProperty().bind(previousStateVBox.managedProperty());

		currentTrace.currentStateProperty().addListener((observable, from, to) -> {
			try {
				currentStateVisualisation.visualiseState(to);
				if (to != null && currentTrace.canGoBack()) {
					previousStateVisualisation.visualiseState(currentTrace.get().getPreviousState());
				}
			} catch (FileNotFoundException e) {
				LOGGER.warn("Failed to open images for visualisation", e);
				Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING, e.getMessage());
				alert.setHeaderText("Visualisation not possible");
				alert.showAndWait();
			}

		});
	}
}
