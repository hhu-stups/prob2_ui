package de.prob2.ui.dotty;

import java.io.IOException;

import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DottyView extends VBox {
	@FXML
	private Button btshowhistory;
		
	@Inject
	public DottyView(FXMLLoader loader) {
		try {
			loader.setLocation(getClass().getResource("dotty_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	public void initialize() {
		btshowhistory.setOnAction(e -> {
			String url = getClass().getResource("/glyphicons_free/glyphicons/png/glyphicons-9-film.png").toString();
			ScrollPane pane = new ScrollPane();
			Stage stage = new Stage();
			ImageView graph = new ImageView(new Image(url));
			graph.setFitHeight(1000);
			graph.setFitWidth(1000);
			graph.setOnMouseClicked(graphe -> {
				if(graphe.getButton() == MouseButton.PRIMARY) {
					graph.setFitHeight(graph.getFitHeight() * 2);
					graph.setFitWidth(graph.getFitWidth() * 2);
				} else if(graphe.getButton() == MouseButton.SECONDARY) {
					graph.setFitHeight(graph.getFitHeight() * 0.5);
					graph.setFitWidth(graph.getFitWidth() * 0.5);
				}
			    pane.setContent(graph); 
			});
			
			pane.setContent(graph); 
			stage.setTitle("Dotty");
			Scene scene = new Scene(pane, 800, 600);
			stage.setScene(scene);
			stage.show();
		});
		
	}
}
