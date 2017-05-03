package de.prob2.ui.verifications.modelchecking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
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
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
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

@Singleton
public final class ModelcheckingController extends ScrollPane implements IModelCheckListener {
	private final class ModelcheckingStageController extends Stage {
		@FXML
		private Button startButton;
		@FXML
		private ChoiceBox<String> selectSearchStrategy;
		@FXML
		private CheckBox findDeadlocks;
		@FXML
		private CheckBox findInvViolations;
		@FXML
		private CheckBox findBAViolations;
		@FXML
		private CheckBox findGoal;
		@FXML
		private CheckBox stopAtFullCoverage;
		@FXML
		private CheckBox searchForNewErrors;

		private ModelcheckingStageController(final StageManager stageManager) {
			stageManager.loadFXML(this, "modelchecking_stage.fxml");
		}

		@FXML
		private void initialize() {
			this.initModality(Modality.APPLICATION_MODAL);
		}

		@FXML
		private void startModelCheck() {
			if (currentTrace.exists()) {
				updateCurrentValues(getOptions(), animations.getCurrentTrace().getStateSpace());
				startModelchecking();
			} else {
				stageManager.makeAlert(Alert.AlertType.ERROR, "No specification file loaded. Cannot run model checker.")
						.showAndWait();
				this.hide();
			}
		}

		private void startModelchecking() {
			stageController.setDisableStart(true);
			jobs.put(currentJob.getJobId(), currentJob);
			currentStats.startJob();
			showStats(currentStats);
			currentJobThread = new Thread(() -> {
				final IModelCheckingResult result;
				try {
					result = currentJob.call();
				} catch (Exception e) {
					LOGGER.error("Exception while running model check job", e);
					Platform.runLater(() -> stageManager
							.makeAlert(Alert.AlertType.ERROR, "Exception while running model check job:\n" + e).show());
					return;
				} finally {
					currentJobThread = null;
					stageController.setDisableStart(false);
				}
				// The consistency checker sometimes doesn't call isFinished, so
				// we call it manually here with some dummy information.
				// If the checker already called isFinished, this call won't do
				// anything - on the first call, the checker was removed from
				// the jobs map, so the second call returns right away.
				isFinished(currentJob.getJobId(), 0, result, new StateSpaceStats(0, 0, 0));
			} , "Model Check Result Waiter " + threadCounter.getAndIncrement());
			currentJobThread.start();
		}

		private ModelCheckingOptions getOptions() {
			ModelCheckingOptions options = new ModelCheckingOptions();
			// TODO: Use selected strategy in modelchecking
			if (selectSearchStrategy.getSelectionModel().isSelected(0)) {
				options = options.breadthFirst(true);
				options = options.depthFirst(false);
			} else {
				options = options.breadthFirst(false);
				options = options.depthFirst(true);
			}
			options = options.checkDeadlocks(findDeadlocks.isSelected());
			options = options.checkInvariantViolations(findInvViolations.isSelected());
			options = options.checkAssertions(findBAViolations.isSelected());
			options = options.checkGoal(findGoal.isSelected());
			options = options.stopAtFullCoverage(stopAtFullCoverage.isSelected());
			options = options.recheckExisting(!searchForNewErrors.isSelected());

			return options;
		}

		@FXML
		private void cancel() {
			if (currentJob != null) {
				currentJob.getStateSpace().sendInterrupt();
			}
			if (currentJobThread != null) {
				currentJobThread.interrupt();
			}
			this.hide();
		}

		private void setDisableStart(final boolean disableStart) {
			Platform.runLater(() -> this.startButton.setDisable(disableStart));
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelcheckingController.class);
	private static final AtomicInteger threadCounter = new AtomicInteger(0);

	@FXML
	private AnchorPane statsPane;
	@FXML
	private VBox historyBox;
	@FXML
	private Button addModelCheckButton;

	private final AnimationSelector animations;
	private final CurrentTrace currentTrace;
	private final StatsView statsView;
	private final ModelcheckingStageController stageController;
	private final StageManager stageManager;

	private final Map<String, IModelCheckJob> jobs;
	private IModelCheckJob currentJob;
	private Thread currentJobThread;
	private ObservableList<Node> historyNodeList;
	private ModelCheckStats currentStats;
	private ModelCheckingOptions currentOptions;

	@Inject
	private ModelcheckingController(final AnimationSelector animations, final CurrentTrace currentTrace,
			final StageManager stageManager, final StatsView statsView) {
		this.animations = animations;
		this.currentTrace = currentTrace;
		this.statsView = statsView;
		this.stageManager = stageManager;

		stageManager.loadFXML(this, "modelchecking_stats_view.fxml");

		this.stageController = new ModelcheckingStageController(stageManager);
		this.jobs = new HashMap<>();
		this.currentJob = null;
		this.currentJobThread = null;
	}

	@FXML
	public void initialize() {
		showStats(new ModelCheckStats(stageManager, this, statsView));
		historyNodeList = historyBox.getChildren();
		addModelCheckButton.disableProperty().bind(currentTrace.existsProperty().not());
	}

	@FXML
	public void addModelCheck() {
		if (!stageController.isShowing()) {
			this.stageController.showAndWait();
		}
	}

	private Node toHistoryNode(ModelCheckingItem item) {
		ContextMenu cm = createContextMenu(item);

		AnchorPane background = new AnchorPane();
		VBox.setMargin(background, new Insets(2.5, 5, 2.5, 5));
		background.setOnMouseClicked(event -> {
			if (event.getButton() == MouseButton.PRIMARY) {
				showStats(item.getStats());
				updateSelectedItem(background);
				if (event.getClickCount() >= 2 && item.getResult() == ModelCheckStats.Result.DANGER) {
					if (currentTrace.exists()) {
						this.animations.removeTrace(currentTrace.get());
					}
					animations.addNewAnimation(item.getStats().getTrace());
				}
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

	private ContextMenu createContextMenu(ModelCheckingItem item) {
		ContextMenu cm = new ContextMenu();
		MenuItem mItem = new MenuItem("Show Trace To Error State");
		mItem.setOnAction(event -> {
			if (currentTrace.exists()) {
				this.animations.removeTrace(currentTrace.get());
			}
			animations.addNewAnimation(item.getStats().getTrace());
		});
		cm.getItems().add(mItem);
		return cm;
	}
	
	public void updateCurrentValues(ModelCheckingOptions options, StateSpace stateSpace) {
		currentOptions = options;
		currentStats = new ModelCheckStats(stageManager, this, statsView);
		currentJob = new ConsistencyChecker(stateSpace, options, null, this);
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
		AbstractElement main = currentJob.getStateSpace().getMainComponent();
		List<String> optsList = options.getPrologOptions().stream().map(ModelCheckingOptions.Options::getDescription).collect(Collectors.toList());
		String name = main == null ? "Model Check" : main.toString();
		if (!optsList.isEmpty()) {
			name += " with " + String.join(", ", optsList);
		}
		return name;
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
		try {
			final IModelCheckJob job = jobs.get(jobId);
			if (job == null) {
				LOGGER.error("Model check job for ID {} is missing or null", jobId);
				return;
			}
			currentStats.updateStats(job, timeElapsed, stats);
		} catch (RuntimeException e) {
			LOGGER.error("Exception in updateStats", e);
			Platform.runLater(
					() -> stageManager.makeAlert(Alert.AlertType.ERROR, "Exception in updateStats:\n" + e).show());
		}
	}

	@Override
	public void isFinished(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
		try {
			final IModelCheckJob job = jobs.remove(jobId);
			if (job == null) {
				// isFinished was already called for this job
				return;
			}
			currentStats.isFinished(job, timeElapsed, result);
			ModelCheckingItem modelcheckingItem = new ModelCheckingItem(currentOptions, currentStats);
			Node historyNode = toHistoryNode(modelcheckingItem);
			Platform.runLater(() -> {
				historyNodeList.add(historyNode);
				this.stageController.hide();
			});
		} catch (RuntimeException e) {
			LOGGER.error("Exception in isFinished", e);
			Platform.runLater(
					() -> stageManager.makeAlert(Alert.AlertType.ERROR, "Exception in isFinished:\n" + e).show());
		}
	}
}
