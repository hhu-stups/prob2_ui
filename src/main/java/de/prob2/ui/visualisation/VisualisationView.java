package de.prob2.ui.visualisation;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

@Singleton
public class VisualisationView extends StackPane {
	private Group group;
	
	@Inject
	public VisualisationView() {
		group = new Group();
		this.getChildren().add(group);
		group.getChildren().add(new ImageView(getClass().getResource("prob_logo.gif").toExternalForm()));
	}
}
