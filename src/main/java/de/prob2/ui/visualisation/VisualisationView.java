package de.prob2.ui.visualisation;

import java.io.FileNotFoundException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
	@FXML
	private Label placeholderLabel;

	private final CurrentTrace currentTrace;
	private final StageManager stageManager;
	private final ResourceBundle bundle;

	@Inject
	public VisualisationView(final CurrentTrace currentTrace, final StageManager stageManager,
			final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
		this.bundle = bundle;
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
				Alert alert = stageManager.makeExceptionAlert(bundle.getString("visualisation.view.alerts.visualisationNotPossible.content"), e);
				alert.setHeaderText(bundle.getString("visualisation.view.alerts.visualisationNotPossible.header"));
				alert.showAndWait();
			}

		});

		currentTrace.addListener((observable, from, to) -> {
			if(to == null) {
				placeholderLabel.setText(bundle.getString("common.noModelLoaded"));
			} else if (currentTrace.getCurrentState().isInitialised()) {
				placeholderLabel.setText(bundle.getString("visualisation.view.noAnimationFunction"));
			} else {
				placeholderLabel.setText(bundle.getString("common.notInitialised"));
			}
		});

	}
}
