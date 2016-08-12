package de.prob2.ui.animations;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;

@Singleton
public class AnimationsView extends AnchorPane {
	
	@Inject
	private AnimationsView(final FXMLLoader loader) {
		try {
			loader.setLocation(getClass().getResource("animations_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
