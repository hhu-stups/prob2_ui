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
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelChecker;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.ModelCheckingOptions.Options;
import de.prob.check.StateSpaceStats;
import de.prob.model.representation.AbstractElement;
import de.prob.statespace.StateSpace;
import de.prob2.ui.events.OpenFileEvent;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

@Singleton
public class ModelcheckingController extends ScrollPane implements IModelCheckListener {

	@FXML
	private AnchorPane statsPane;
	@FXML
	private VBox historyBox;

	// private boolean errorFoundBefore;
	private ModelChecker checker;
	private ObservableList<Node> historyNodeList;
	private ModelCheckStats currentStats;
	private ModelCheckingOptions currentOptions;

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
		showStats(new ModelCheckStats(new FXMLLoader(), this));
		historyNodeList = historyBox.getChildren();
	}

	void startModelchecking(ModelCheckingOptions options, StateSpace currentStateSpace) {
		currentOptions = options;
		currentStats = new ModelCheckStats(new FXMLLoader(), this);
		checker = new ModelChecker(new ConsistencyChecker(currentStateSpace, options, null, this));
		currentStats.addJob(checker.getJobId(), checker);
		showStats(currentStats);
		checker.start();	
	}

	private Node toHistoryNode(HistoryItem item) {
		AnchorPane background = new AnchorPane();
		background.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				showStats(item.getStats());
			}
		});
		background.getStyleClass().add("historyItemBackground");
		HBox box = new HBox();
		box.setSpacing(5);
		background.getChildren().add(box);
		AnchorPane.setTopAnchor(box, 2.0);
		AnchorPane.setRightAnchor(box, 4.0);
		AnchorPane.setBottomAnchor(box, 2.0);
		AnchorPane.setLeftAnchor(box, 4.0);
		ImageView imView = new ImageView(selectImage(item.getResult()));
		imView.setFitHeight(15);
		imView.setFitWidth(15);
		Text text = new Text(toPrettyString(item.getOptions()));
		Platform.runLater(() -> {
			text.wrappingWidthProperty().bind(this.widthProperty().subtract(65.0));
		});
		box.getChildren().add(imView);
		box.getChildren().add(text);
		return background;
	}
	
	private Image selectImage(String res) {
		Image image = null;
		switch (res) {
		case "success":
			image = new Image(
					getClass().getResourceAsStream("/glyphicons_free/glyphicons/png/glyphicons-199-ok-circle.png"));
			break;
		case "danger":
			image = new Image(
					getClass().getResourceAsStream("/glyphicons_free/glyphicons/png/glyphicons-198-remove-circle.png"));
			break;
		case "warning":
			image = new Image(
					getClass().getResourceAsStream("/glyphicons_free/glyphicons/png/glyphicons-505-alert.png"));
			break;
		}
		System.out.println("*** " + res);
		return image;
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
		showStats(new ModelCheckStats(new FXMLLoader(), this));
		historyNodeList.clear();
	}

	@Override
	public void updateStats(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
		currentStats.updateStats(jobId, timeElapsed, result, stats);
	}

	@Override
	public void isFinished(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
		currentStats.isFinished(jobId, timeElapsed, result, stats);
		HistoryItem historyItem = new HistoryItem(currentOptions, currentStats);
		Node historyNode = toHistoryNode(historyItem);
		Platform.runLater(() -> {
			historyNodeList.add(historyNode);
		});
	}

	// TODO remove the following method, find a better way to do this
	// @Subscribe
	// public void showStats(ModelCheckStatsEvent event) {
	//
	// String res = event.getResult();
	// Boolean searchForNewErrors = event.getSearchForNewErrors();
	//
	// if(!searchForNewErrors) {
	// errorFoundBefore = false;
	// }
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
