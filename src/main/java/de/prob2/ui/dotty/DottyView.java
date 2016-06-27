package de.prob2.ui.dotty;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import com.google.inject.Inject;

import de.prob.statespace.Animations;
import de.prob.statespace.ITraceChangesListener;
import de.prob.statespace.Trace;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class DottyView extends TitledPane implements Initializable, ITraceChangesListener {

	@FXML
	private Button btshowhistory;
	
	@FXML
	private Button btshowexpression;
	
	private Animations animations;
	
	@Inject
	public DottyView(FXMLLoader loader, Animations animations) {
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
		
		btshowexpression.setOnAction(e -> {
			Group root = new Group();
	        
			Stage stage = new Stage();
			
			FormulaGraph g = new FormulaGraph(100, 100, "DATA");
			g.add(new FormulaNode("BOO"));
			g.add(new FormulaNode("FOO"));
			g.add(new FormulaNode("TESTEST"));

			root.getChildren().add(g);
			
			stage.setTitle("Mathematical Expression");
			Scene scene = new Scene(root, 800, 600);
			stage.setScene(scene);
			stage.show();
			
		});

	}
	

	@Override
	public void changed(List<Trace> t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removed(List<UUID> t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void animatorStatus(Set<UUID> busy) {
		// TODO Auto-generated method stub
		
	}
	
	

}
