package de.prob2.ui.dotty;

import java.io.IOException;

import com.google.inject.Inject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

public class DottyStage extends Stage {
	@FXML
	ImageView graph;
	@FXML
	ScrollPane pane;

	@Inject
	public DottyStage(FXMLLoader loader) {
		try {
			loader.setLocation(getClass().getResource("dotty_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
