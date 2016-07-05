package de.prob2.ui.dotty;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import com.google.inject.Inject;

import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.Trace;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

public class DottyView extends TitledPane implements Initializable, IAnimationChangeListener {

	@FXML
	private Button btshowhistory;
		
	private AnimationSelector animations;
	
	@Inject
	public DottyView(FXMLLoader loader, AnimationSelector animations) {
		this.animations = animations;
		animations.registerAnimationChangeListener(this);

		try {
			loader.setLocation(getClass().getResource("dotty_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void initialize(URL location, ResourceBundle resources) {
		
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
	


	@Override
	public void traceChange(Trace currentTrace, boolean currentAnimationChanged) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void animatorStatus(boolean busy) {
		// TODO Auto-generated method stub
		
	}
	
	

}
