package de.prob2.ui.bmotion;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

@Singleton
public class BMotionView extends StackPane {
	
	private Group group;
	
	@Inject
	public BMotionView(CurrentTrace currentTrace) {
		group = new Group();
		this.getChildren().add(group);
		ImageView image = new ImageView();
		group.getChildren().add(new ImageView(getClass().getResource("prob_logo.gif").toExternalForm()));
		//System.out.println(currentTrace.getStateSpace().);
	}

}
