package de.prob2.ui.modelchecking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;

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

import de.prob2.ui.internal.IComponents;
import de.prob2.ui.prob2fx.CurrentStage;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.stats.StatsView;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModelcheckingController extends ScrollPane implements IModelCheckListener, IComponents {
	private final class ModelcheckingStageController {
		@FXML private Stage mcheckStage;
		@FXML private CheckBox findDeadlocks;
		@FXML private CheckBox findInvViolations;
		@FXML private CheckBox findBAViolations;
		@FXML private CheckBox findGoal;
		@FXML private CheckBox stopAtFullCoverage;
		@FXML private CheckBox searchForNewErrors;
		
		@FXML
		public void initialize() {
			currentStage.register(this.mcheckStage, null);
		}
		
		@FXML
		private void startModelCheck(ActionEvent event) {
			if (currentTrace.exists()) {
				startModelchecking(getOptions(), animations.getCurrentTrace().getStateSpace());
			} else {
				final Alert alert = new Alert(Alert.AlertType.ERROR, "No specification file loaded. Cannot run model checker.");
				alert.setHeaderText("Specification file missing");
				alert.getDialogPane().getStylesheets().add("prob.css");
				alert.showAndWait();
				this.mcheckStage.close();
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
			this.mcheckStage.getScene().getWindow().hide();
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(ModelcheckingController.class);
	
	@FXML private AnchorPane statsPane;
	@FXML private VBox historyBox;
	@FXML private Button addModelCheckButton;
	
	private final Injector injector;
	private final AnimationSelector animations;
	private final CurrentTrace currentTrace;
	private final CurrentStage currentStage;
	private final StatsView statsView;
	private final ModelcheckingStageController stageController;

	private ModelChecker checker;
	private ObservableList<Node> historyNodeList;
	private ModelCheckStats currentStats;
	private ModelCheckingOptions currentOptions;

	@Inject
	private ModelcheckingController(
		final Injector injector,
		final AnimationSelector animations,
		final CurrentTrace currentTrace,
		final CurrentStage currentStage,
		final StatsView statsView
	) {
		this.injector = injector;
		this.animations = animations;
		this.currentTrace = currentTrace;
		this.currentStage = currentStage;
		this.statsView = statsView;
		
		final FXMLLoader mainLoader = injector.getInstance(FXMLLoader.class);
		mainLoader.setLocation(getClass().getResource("modelchecking_stats_view.fxml"));
		mainLoader.setRoot(this);
		mainLoader.setController(this);
		try {
			mainLoader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
		
		final FXMLLoader stageLoader = injector.getInstance(FXMLLoader.class);
		stageLoader.setLocation(getClass().getResource("modelchecking_stage.fxml"));
		this.stageController = new ModelcheckingStageController();
		stageLoader.setController(this.stageController);
		try {
			stageLoader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
	}

	@FXML
	public void initialize() {
		showStats(new ModelCheckStats(injector.getInstance(FXMLLoader.class), this, statsView));
		historyNodeList = historyBox.getChildren();
		addModelCheckButton.disableProperty().bind(currentTrace.existsProperty().not());
	}

	@FXML
	public void addModelCheck() {
		if(!stageController.mcheckStage.isShowing()) {
			this.stageController.mcheckStage.initModality(Modality.APPLICATION_MODAL);
			this.stageController.mcheckStage.showAndWait();
		}
	}

	void startModelchecking(ModelCheckingOptions options, StateSpace currentStateSpace) {
		currentOptions = options;
		currentStats = new ModelCheckStats(injector.getInstance(FXMLLoader.class), this, statsView);
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
		showStats(new ModelCheckStats(injector.getInstance(FXMLLoader.class), this, statsView));
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
			this.stageController.mcheckStage.close();
		});
	}
}
