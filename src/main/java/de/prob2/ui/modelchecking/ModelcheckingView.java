package de.prob2.ui.modelchecking;

import java.io.IOException;

import com.google.inject.Inject;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;

public class ModelcheckingView extends AnchorPane {

	@Inject
	public ModelcheckingView(FXMLLoader loader) {
		try {
			loader.setLocation(getClass().getResource("modelchecking_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
