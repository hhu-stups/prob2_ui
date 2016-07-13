package de.prob2.ui.modelchecking;

import java.io.IOException;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

import de.prob2.ui.events.ModelCheckStatsEvent;
import de.prob2.ui.events.OpenFileEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class ModelCheckStatsView extends TitledPane {

	@FXML
	private VBox statsBox;
	@FXML
	private AnchorPane resultBackground;
	@FXML
	private Text resultText;
	
	private boolean errorFoundBefore;

	@Inject
	public ModelCheckStatsView(FXMLLoader loader, EventBus bus) {
		bus.register(this);
		try {
			loader.setLocation(getClass().getResource("modelchecking_stats_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Subscribe
	public void showStats(ModelCheckStatsEvent event) {
		String res = event.getResult();
		String message = event.getMessage();
		ModelCheckStats stats = event.getModelCheckStats();
		Boolean searchForNewErrors = event.getSearchForNewErrors();
		
//		if(!searchForNewErrors) {
//			errorFoundBefore = false;
//		} 
		if (res.equals("danger")) {
			errorFoundBefore = true;
		}
		
		Platform.runLater(() -> {
			resultText.setText(message);
			if (res == "success") {
				resultBackground.getStyleClass().clear();
				resultBackground.getStyleClass().add("mcheckSuccess");
				resultText.setFill(Color.web("#5e945e"));
			} else if (res == "danger") {
				resultBackground.getStyleClass().clear();
				resultBackground.getStyleClass().add("mcheckDanger");
				resultText.setFill(Color.web("#b95050"));
			} else if (res == "warning") {
				resultBackground.getStyleClass().clear();
				resultBackground.getStyleClass().add("mcheckWarning");
				resultText.setFill(Color.web("#96904e"));
			}
			if (!statsBox.getChildren().contains(stats))
				statsBox.getChildren().add(stats);
			
			if (res.equals("success") && errorFoundBefore && searchForNewErrors) {
				Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle("Note");
				alert.setHeaderText("Some previously explored nodes do contain errors."
						+ "\nTurn off \u0027Search for New Errors\u0027 and re-run the model checker to find the errors.");
				alert.showAndWait();
				return;
			}
		});
		Accordion accordion = ((Accordion) this.getParent());
		accordion.setExpandedPane(this);
	}
	
	@Subscribe
	public void resetView(OpenFileEvent event) {
		errorFoundBefore = false;
		resultText.setText("No Model Checking Job done.");
		resultBackground.getStyleClass().clear();
		resultBackground.getStyleClass().add("mcheckNoCheck");
		resultText.setFill(Color.web("#8a8a8a"));
		if(statsBox.getChildren().size() > 1) {
			statsBox.getChildren().remove(1);
		}
	}
}
