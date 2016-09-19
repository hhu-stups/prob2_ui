package de.prob2.ui.modelchecking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
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
	private Logger logger = LoggerFactory.getLogger(ModelcheckingController.class);

	@Inject
	private ModelcheckingController(final AnimationSelector animations, FXMLLoader loader) {
		this.animations = animations;
		try {
			loader.setLocation(getClass().getResource("modelchecking_stats_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
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
		background.setOnMouseClicked(event -> {
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
		});
		updateSelectedItem(background);

		HBox box = new HBox();
		box.setSpacing(5);
		background.getChildren().add(box);
		AnchorPane.setTopAnchor(box, 2.0);
		AnchorPane.setRightAnchor(box, 4.0);
		AnchorPane.setBottomAnchor(box, 2.0);
		AnchorPane.setLeftAnchor(box, 4.0);

		FontAwesomeIconView iconView = selectIcon(item.getResult());
		Text text = new Text(toPrettyString(item.getOptions()));
		Platform.runLater(() -> {
			text.wrappingWidthProperty().bind(this.widthProperty().subtract(70.0));
		});
		box.getChildren().add(iconView);
		box.getChildren().add(text);

		return background;
	}

	private ContextMenu createContextMenu(HistoryItem item) {
		ContextMenu cm = new ContextMenu();
		MenuItem mItem = new MenuItem("Show Trace To Error State");
		mItem.setOnAction(event -> {
			animations.addNewAnimation(item.getStats().getTrace());
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

	private FontAwesomeIconView selectIcon(String res) {
		FontAwesomeIcon icon = null;
		switch (res) {
		case "success":
			icon = FontAwesomeIcon.CHECK_CIRCLE_ALT;
			break;
		case "danger":
			icon = FontAwesomeIcon.TIMES_CIRCLE_ALT;
			break;
		default:
			icon = FontAwesomeIcon.EXCLAMATION_TRIANGLE;
		}
		FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
		iconView.setSize("15");
		return iconView;
	}

	private String toPrettyString(ModelCheckingOptions options) {
		ModelChecker modelChecker = checker;
		AbstractElement main = modelChecker.getStateSpace().getMainComponent();
		List<String> optsList = new ArrayList<>();
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
