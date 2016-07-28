package de.prob2.ui.modelchecking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.check.ConsistencyChecker;
import de.prob.check.ModelChecker;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.ModelCheckingOptions.Options;
import de.prob.model.representation.AbstractElement;
import de.prob.statespace.StateSpace;
import de.prob2.ui.events.OpenFileEvent;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

@Singleton
public class ModelcheckingController extends ScrollPane {

	@FXML
	private AnchorPane statsPane;
	@FXML
	private VBox historyBox;

	// private boolean errorFoundBefore;
	private ModelChecker checker;
	private ObservableList<Node> historyList;

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
		historyList = historyBox.getChildren();
		
	}

	void startModelchecking(ModelCheckingOptions options, StateSpace currentStateSpace) {
		ModelCheckStats stats = new ModelCheckStats(new FXMLLoader());
		checker = new ModelChecker(new ConsistencyChecker(currentStateSpace, options, null, stats));
		stats.addJob(checker.getJobId(), checker);

		showStats(stats);
		historyList.add(toHistoryItem(options));

		checker.start();
	}

	private Node toHistoryItem(ModelCheckingOptions options) {
		AnchorPane background = new AnchorPane();
		background.getStyleClass().add("historyItemBackground");
		HBox box = new HBox();
		background.getChildren().add(box);
		AnchorPane.setTopAnchor(box, 2.0);
		AnchorPane.setRightAnchor(box, 4.0);
		AnchorPane.setBottomAnchor(box, 2.0);
		AnchorPane.setLeftAnchor(box, 4.0);
		Text text = new Text(toPrettyString(options));
		Platform.runLater(() -> {
			text.wrappingWidthProperty().bind(background.widthProperty().subtract(15.0));
		});
		box.getChildren().add(text);
		return background;
	}

	private String toPrettyString(ModelCheckingOptions options) {
		// ModelChecker modelChecker = jobs.get(id).getChecker();
		ModelChecker modelChecker = checker;
		AbstractElement main = modelChecker.getStateSpace().getMainComponent();
		List<String> optsList = new ArrayList<String>();
		for (Options opts : options.getPrologOptions()) {
			optsList.add(opts.getDescription());
		}
		String name = main == null ? "Model Check" : main.toString();
		if (!optsList.isEmpty()) {
			name += " with " + Joiner.on(", ").join(optsList);
		}
		return name;
	}

	void cancelModelchecking() {
		checker.cancel();
	}

	private void showStats(ModelCheckStats stats) {
		statsPane.getChildren().clear();
		statsPane.getChildren().add(stats);
		AnchorPane.setTopAnchor(stats, 0.0);
		AnchorPane.setRightAnchor(stats, 0.0);
		AnchorPane.setBottomAnchor(stats, 0.0);
		AnchorPane.setLeftAnchor(stats, 0.0);
	}

	@Subscribe
	public void resetView(OpenFileEvent event) {
		showStats(new ModelCheckStats(new FXMLLoader()));
	}

	// TODO remove the following method, find a better way to do this
	// @Subscribe
	// public void showStats(ModelCheckStatsEvent event) {
	//
	// String res = event.getResult();
	// Boolean searchForNewErrors = event.getSearchForNewErrors();
	//
	//// if(!searchForNewErrors) {
	//// errorFoundBefore = false;
	//// }
	// if (res.equals("danger")) {
	// errorFoundBefore = true;
	// }
	//
	// Platform.runLater(() -> {
	// if (res.equals("success") && errorFoundBefore && searchForNewErrors) {
	// Alert alert = new Alert(AlertType.WARNING);
	// alert.setTitle("Note");
	// alert.setHeaderText("Some previously explored nodes do contain errors."
	// + "\nTurn off \u0027Search for New Errors\u0027 and re-run the model
	// checker to find the errors.");
	// alert.showAndWait();
	// return;
	// }
	// });
	// }
}
