package de.prob2.ui.dotty;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.prob2fx.CurrentStage;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DottyStage extends Stage {
	@FXML
	ImageView graph;
	@FXML
	ScrollPane pane;

	private Logger logger = LoggerFactory.getLogger(DottyStage.class);

	@Inject
	private DottyStage(FXMLLoader loader, CurrentStage currentStage) {
		try {
			loader.setLocation(getClass().getResource("dotty_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}

		currentStage.register(this);
	}

	@FXML
	public void initialize() {
		graph.setOnMouseClicked(graphe -> {
			if (graphe.getButton() == MouseButton.PRIMARY) {
				graph.setFitHeight(graph.getFitHeight() * 2);
				graph.setFitWidth(graph.getFitWidth() * 2);
			} else if (graphe.getButton() == MouseButton.SECONDARY) {
				graph.setFitHeight(graph.getFitHeight() * 0.5);
				graph.setFitWidth(graph.getFitWidth() * 0.5);
			}
			pane.setContent(graph);
		});
	}
}
