package de.prob2.ui.modelchecking;

import java.io.IOException;

import com.google.inject.Inject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class ModelCheckStatsView extends TitledPane{
	
    @FXML
    private VBox statsBox;
	private ModelCheckStats modelCheckStatsController;
	
	@Inject
	public ModelCheckStatsView(FXMLLoader loader, ModelCheckStats modelCheckStatsController) {
		this.modelCheckStatsController = modelCheckStatsController;
		try {
			loader.setLocation(getClass().getResource("modelchecking_stats_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	@FXML
	public void initialize() {
		statsBox.getChildren().add(modelCheckStatsController);
	}

}
