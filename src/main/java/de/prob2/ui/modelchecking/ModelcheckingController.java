package de.prob2.ui.modelchecking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
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
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
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

	private ModelChecker checker;
	private ObservableList<Node> historyNodeList;
	private ModelCheckStats currentStats;
	private ModelCheckingOptions currentOptions;
	private AnimationSelector animations;

	@Inject
	private ModelcheckingController(final AnimationSelector animations, FXMLLoader loader) {
		this.animations = animations;
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
		ContextMenu cm = createContextMenu(item);

		AnchorPane background = new AnchorPane();
		VBox.setMargin(background, new Insets(2.5, 5, 2.5, 5));
		background.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton().equals(MouseButton.PRIMARY)) {
					showStats(item.getStats());
					updateSelectedItem(background);
				}
				if (event.getButton().equals(MouseButton.SECONDARY)) {
					cm.show(background, event.getScreenX(), event.getScreenY());
					if (!item.getResult().equals("danger")) {
						cm.getItems().get(0).setDisable(true);
					}
				}
			}
		});
		updateSelectedItem(background);

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
			text.wrappingWidthProperty().bind(this.widthProperty().subtract(70.0));
		});
		box.getChildren().add(imView);
		box.getChildren().add(text);

		return background;
	}

	private ContextMenu createContextMenu(HistoryItem item) {
		ContextMenu cm = new ContextMenu();
		MenuItem mItem = new MenuItem("Show Trace To Error State");
		mItem.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				animations.addNewAnimation(item.getStats().getTrace());
			}
		});
		cm.getItems().add(mItem);
		return cm;
	}

	protected void updateSelectedItem(Node selected) {
		for (Node node : historyNodeList) {
			node.getStyleClass().remove("historyItemBackgroundSelected");
			node.getStyleClass().add("historyItemBackground");
		}
		selected.getStyleClass().add("historyItemBackgroundSelected");
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
		return image;
	}

	private String toPrettyString(ModelCheckingOptions options) {
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
		if (checker != null) {
			checker.cancel();
		}
	}

	private void showStats(ModelCheckStats stats) {
		statsPane.getChildren().clear();
		statsPane.getChildren().add(stats);
		AnchorPane.setTopAnchor(stats, 0.0);
		AnchorPane.setRightAnchor(stats, 0.0);
		AnchorPane.setBottomAnchor(stats, 0.0);
		AnchorPane.setLeftAnchor(stats, 0.0);
	}

	public void resetView() {
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
}
