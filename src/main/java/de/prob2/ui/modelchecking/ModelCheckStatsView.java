package de.prob2.ui.modelchecking;

import java.io.IOException;

import com.google.inject.Inject;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.TitledPane;

public class ModelCheckStatsView extends TitledPane{
	
	@Inject
	public ModelCheckStatsView(FXMLLoader loader) {
		try {
			loader.setLocation(getClass().getResource("modelchecking_stats_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
