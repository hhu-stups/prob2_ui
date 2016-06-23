package de.prob2.ui.modelchecking;

import java.io.IOException;

import com.google.inject.Inject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class ModelCheckStatsView extends TitledPane{
	
    @FXML
    private VBox statsBox;
    @FXML
    private AnchorPane resultBackground;
    @FXML
    private Label resultLabel;
	
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

	public void showStats(ModelCheckStats modelCheckStats, String res) {
		Platform.runLater(() -> {
			if(res == "success") {
				resultBackground.getStyleClass().clear();;
				resultBackground.getStyleClass().add("mcheckSuccess");
				resultLabel.setText("Model Checking complete. No error nodes found.");
				resultLabel.setTextFill(Color.web("#5e945e"));
			} else if(res == "danger") {
				resultBackground.getStyleClass().clear();;
				resultBackground.getStyleClass().add("mcheckDanger");
				resultLabel.setText("Invalidation violation found!");
				resultLabel.setTextFill(Color.web("#b95050"));
			}
			if(!statsBox.getChildren().contains(modelCheckStats))
				statsBox.getChildren().add(modelCheckStats);
		});
	}
	
}
