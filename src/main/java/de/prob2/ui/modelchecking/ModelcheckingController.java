package de.prob2.ui.modelchecking;

import java.io.IOException;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.check.ConsistencyChecker;
import de.prob.check.ModelChecker;
import de.prob.check.ModelCheckingOptions;
import de.prob.statespace.StateSpace;
import de.prob2.ui.events.OpenFileEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Accordion;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

@Singleton
public class ModelcheckingController extends ScrollPane {

	@FXML
	private AnchorPane statsPane;
	
//	private boolean errorFoundBefore;
	private ModelChecker checker;

	@Inject
	private ModelcheckingController(FXMLLoader loader, EventBus bus) {
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
	
	@FXML
	public void initialize() {
		showStats(new ModelCheckStats(new FXMLLoader()));
	}
	
	void startModelchecking(ModelCheckingOptions options, StateSpace currentStateSpace) {
		ModelCheckStats stats = new ModelCheckStats(new FXMLLoader());
		checker = new ModelChecker(new ConsistencyChecker(currentStateSpace, options, null, stats));
		stats.addJob(checker.getJobId(), checker);
		
		showStats(stats);
		//Accordion accordion = ((Accordion) this.getParent());
		//accordion.setExpandedPane(this);
		
		checker.start();
		//TODO add ModelCheckingHistoryList
	}
	
	void cancelModelchecking() {
		checker.cancel();
	}

	private void showStats(ModelCheckStats stats) {
		statsPane.getChildren().clear();
		statsPane.getChildren().add(stats);
	}

	@Subscribe
	public void resetView(OpenFileEvent event) {
		showStats(new ModelCheckStats(new FXMLLoader()));
	}

//TODO remove the following method, find a better way to do this
//	@Subscribe
//	public void showStats(ModelCheckStatsEvent event) {
//		
//		String res = event.getResult();
//		Boolean searchForNewErrors = event.getSearchForNewErrors();
//		
////		if(!searchForNewErrors) {
////			errorFoundBefore = false;
////		} 
//		if (res.equals("danger")) {
//			errorFoundBefore = true;
//		}
//		
//		Platform.runLater(() -> {
//			if (res.equals("success") && errorFoundBefore && searchForNewErrors) {
//				Alert alert = new Alert(AlertType.WARNING);
//				alert.setTitle("Note");
//				alert.setHeaderText("Some previously explored nodes do contain errors."
//						+ "\nTurn off \u0027Search for New Errors\u0027 and re-run the model checker to find the errors.");
//				alert.showAndWait();
//				return;
//			}
//		});
//	}
}
