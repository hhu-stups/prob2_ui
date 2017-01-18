package de.prob2.ui.modelchecking;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelChecker;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.StateSpaceStats;
import de.prob.model.representation.AbstractElement;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.stats.StatsView;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class ModelcheckingController extends ScrollPane implements IModelCheckListener {
	private final class ModelcheckingStageController extends Stage {
		@FXML private CheckBox findDeadlocks;
		@FXML private CheckBox findInvViolations;
		@FXML private CheckBox findBAViolations;
		@FXML private CheckBox findGoal;
		@FXML private CheckBox stopAtFullCoverage;
		@FXML private CheckBox searchForNewErrors;
		
		private ModelcheckingStageController(final StageManager stageManager) {
			stageManager.loadFXML(this, "modelchecking_stage.fxml");
		}
		
		@FXML
		private void initialize() {
			this.initModality(Modality.APPLICATION_MODAL);
		}
		
		@FXML
		private void startModelCheck(ActionEvent event) {
			if (currentTrace.exists()) {
				startModelchecking(getOptions(), animations.getCurrentTrace().getStateSpace());
			} else {
				stageManager.makeAlert(Alert.AlertType.ERROR, "No specification file loaded. Cannot run model checker.").showAndWait();
				this.hide();
			}
		}
		
		private ModelCheckingOptions getOptions() {
			ModelCheckingOptions options = new ModelCheckingOptions();
			options = options.breadthFirst(true);
			options = options.checkDeadlocks(findDeadlocks.isSelected());
			options = options.checkInvariantViolations(findInvViolations.isSelected());
			options = options.checkAssertions(findBAViolations.isSelected());
			options = options.checkGoal(findGoal.isSelected());
			options = options.stopAtFullCoverage(stopAtFullCoverage.isSelected());
			options = options.recheckExisting(!searchForNewErrors.isSelected());
			
			return options;
		}
		
		@FXML
		void cancel(ActionEvent event) {
			cancelModelchecking();
			this.hide();
		}
	}
	
	@FXML private AnchorPane statsPane;
	@FXML private VBox historyBox;
	@FXML private Button addModelCheckButton;
	
	private final AnimationSelector animations;
	private final CurrentTrace currentTrace;
	private final StatsView statsView;
	private final ModelcheckingStageController stageController;
	private final StageManager stageManager;

	private ModelChecker checker;
	private ObservableList<Node> historyNodeList;
	private ModelCheckStats currentStats;
	private ModelCheckingOptions currentOptions;

	@Inject
	private ModelcheckingController(
		final AnimationSelector animations,
		final CurrentTrace currentTrace,
		final StageManager stageManager,
		final StatsView statsView
	) {
		this.animations = animations;
		this.currentTrace = currentTrace;
		this.statsView = statsView;
		this.stageManager = stageManager;
		
		stageManager.loadFXML(this, "modelchecking_stats_view.fxml");
		
		this.stageController = new ModelcheckingStageController(stageManager);
	}

	@FXML
	public void initialize() {
		showStats(new ModelCheckStats(stageManager, this, statsView));
		historyNodeList = historyBox.getChildren();
		addModelCheckButton.disableProperty().bind(currentTrace.existsProperty().not());
	}

	@FXML
	public void addModelCheck() {
		if(!stageController.isShowing()) {
			this.stageController.showAndWait();
		}
	}

	void startModelchecking(ModelCheckingOptions options, StateSpace currentStateSpace) {
		currentOptions = options;
		currentStats = new ModelCheckStats(stageManager, this, statsView);
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
			if (event.getButton() == MouseButton.PRIMARY) {
				showStats(item.getStats());
				updateSelectedItem(background);
			}
			if (event.getButton() == MouseButton.SECONDARY) {
				cm.show(background, event.getScreenX(), event.getScreenY());
				if (item.getResult() != ModelCheckStats.Result.DANGER) {
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
		Platform.runLater(() -> text.wrappingWidthProperty().bind(this.widthProperty().subtract(70.0)));
		box.getChildren().add(iconView);
		box.getChildren().add(text);

		return background;
	}

	private ContextMenu createContextMenu(HistoryItem item) {
		ContextMenu cm = new ContextMenu();
		MenuItem mItem = new MenuItem("Show Trace To Error State");
		mItem.setOnAction(event -> animations.addNewAnimation(item.getStats().getTrace()));
		cm.getItems().add(mItem);
		return cm;
	}

	private void updateSelectedItem(Node selected) {
		for (Node node : historyNodeList) {
			node.getStyleClass().remove("historyItemBackgroundSelected");
			node.getStyleClass().add("historyItemBackground");
		}
		selected.getStyleClass().add("historyItemBackgroundSelected");
	}

	private FontAwesomeIconView selectIcon(ModelCheckStats.Result res) {
		FontAwesomeIcon icon;
		switch (res) {
			case SUCCESS:
				icon = FontAwesomeIcon.CHECK_CIRCLE_ALT;
				break;
			
			case DANGER:
				icon = FontAwesomeIcon.TIMES_CIRCLE_ALT;
				break;
			
			case WARNING:
				icon = FontAwesomeIcon.EXCLAMATION_TRIANGLE;
				break;
			
			default:
				throw new IllegalArgumentException("Invalid result: " + res);
		}
		FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
		iconView.setSize("15");
		return iconView;
	}

	private String toPrettyString(ModelCheckingOptions options) {
		ModelChecker modelChecker = checker;
		AbstractElement main = modelChecker.getStateSpace().getMainComponent();
		List<String> optsList = new ArrayList<>();
		for (ModelCheckingOptions.Options opts : options.getPrologOptions()) {
			optsList.add(opts.getDescription());
		}
		String name = main == null ? "Model Check" : main.toString();
		if (!optsList.isEmpty()) {
			name += " with " + String.join(", ", optsList);
		}
		return name;
	}

	void cancelModelchecking() {
		if (checker != null) {
			checker.cancel();
		}
	}

	private void showStats(ModelCheckStats stats) {
		statsPane.getChildren().setAll(stats);
		AnchorPane.setTopAnchor(stats, 0.0);
		AnchorPane.setRightAnchor(stats, 0.0);
		AnchorPane.setBottomAnchor(stats, 0.0);
		AnchorPane.setLeftAnchor(stats, 0.0);
	}

	public void resetView() {
		showStats(new ModelCheckStats(stageManager, this, statsView));
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
			this.stageController.hide();
		});
	}
}
