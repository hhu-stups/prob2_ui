package de.prob2.ui.bmotion;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

@Singleton
public class BMotionView extends StackPane {
	private Group group;
	
	@Inject
	public BMotionView() {
		group = new Group();
		this.getChildren().add(group);
		group.getChildren().add(new ImageView(getClass().getResource("prob_logo.gif").toExternalForm()));
	}
}
