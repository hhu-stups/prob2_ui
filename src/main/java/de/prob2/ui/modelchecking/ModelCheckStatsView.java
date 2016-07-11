package de.prob2.ui.modelchecking;

import java.io.IOException;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

import de.prob2.ui.events.ModelCheckStatsEvent;
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
		String note = event.getNote();
		ModelCheckStats stats = event.getModelCheckStats();
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
			if (note != null) {
				Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle("Note");
				alert.setHeaderText(note);
				alert.showAndWait();
				return;
			}
		});
		Accordion accordion = ((Accordion) this.getParent());
		accordion.setExpandedPane(this);
	}
}
