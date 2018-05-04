package de.prob2.ui.verifications.tracereplay;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.config.FileChooserManager.Kind;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

@Singleton
public class TraceReplayView extends ScrollPane {

	@FXML
	private TableView<ReplayTrace> traceTableView;
	@FXML
	private TableColumn<ReplayTrace, Node> statusColumn;
	@FXML
	private TableColumn<ReplayTrace, String> nameColumn;
	@FXML
	private Button checkButton;
	@FXML
	private Button cancelButton;
	@FXML
	private Button loadTraceButton;
	@FXML
	private HelpButton helpButton;

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final TraceChecker traceChecker;
	private final ResourceBundle bundle;
	private final FileChooserManager fileChooserManager;

	@Inject
	private TraceReplayView(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace,
			final TraceChecker traceChecker, final ResourceBundle bundle, final FileChooserManager fileChooserManager) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.traceChecker = traceChecker;
		this.bundle = bundle;
		this.fileChooserManager = fileChooserManager;
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}

	private static void updateStatusIcon(final FontAwesomeIconView iconView, final ReplayTrace.Status status) {
		iconView.getStyleClass().add("status-icon");
		switch (status) {
		case SUCCESSFUL:
			iconView.setIcon(FontAwesomeIcon.CHECK);
			iconView.setFill(Color.GREEN);
			break;

		case FAILED:
			iconView.setIcon(FontAwesomeIcon.REMOVE);
			iconView.setFill(Color.RED);
			break;

		case NOT_CHECKED:
			iconView.setIcon(FontAwesomeIcon.QUESTION_CIRCLE);
			iconView.setFill(Color.BLUE);
			break;

		default:
			throw new AssertionError("Unhandled status: " + status);
		}
	}

	@FXML
	private void initialize() {
		helpButton.setHelpContent(this.getClass());
		traceTableView.disableProperty().bind(currentTrace.existsProperty().not());
		statusColumn.setCellValueFactory(features -> {
			final ReplayTrace trace = features.getValue();

			final FontAwesomeIconView statusIcon = new FontAwesomeIconView();
			trace.statusProperty().addListener((o, from, to) -> updateStatusIcon(statusIcon, to));
			updateStatusIcon(statusIcon, trace.getStatus());

			final ProgressIndicator replayProgress = new ProgressBar();
			if (trace.getStoredTrace() != null) {
				replayProgress.progressProperty().bind(
						trace.progressProperty().divide((double) trace.getStoredTrace().getTransitionList().size()));
			}

			return Bindings.when(trace.progressProperty().isEqualTo(-1)).<Node>then(statusIcon)
					.otherwise(replayProgress);
		});
		nameColumn.setCellValueFactory(
				features -> new SimpleStringProperty(features.getValue().getLocation().toString()));

		this.traceChecker.getReplayTraces().addListener(
				(MapChangeListener<Path, ReplayTrace>) c -> traceTableView.getItems().setAll(c.getMap().values()));

		initTableRows();
		loadTraceButton.disableProperty().bind(currentProject.currentMachineProperty().isNull());
		cancelButton.disableProperty().bind(traceChecker.currentJobThreadsProperty().emptyProperty());
		checkButton.disableProperty().bind(
				Bindings.createBooleanBinding(() -> traceTableView.getItems().isEmpty(), traceTableView.getItems()).or(currentTrace.existsProperty().not()));
	}

	private void initTableRows() {
		this.traceTableView.setRowFactory(param -> {
			final TableRow<ReplayTrace> row = new TableRow<>();

			final MenuItem replayTraceItem = new MenuItem(
					bundle.getString("verifications.tracereplay.contextMenu.replayTrace"));
			replayTraceItem.setOnAction(event -> this.traceChecker.replayTrace(row.getItem().getLocation(), true));

			final MenuItem showErrorItem = new MenuItem(
					bundle.getString("verifications.tracereplay.contextMenu.showError"));
			showErrorItem
					.setOnAction(event -> stageManager.makeExceptionAlert("", row.getItem().getError()).showAndWait());
			showErrorItem.setDisable(true);

			final MenuItem deleteTraceItem = new MenuItem(
					bundle.getString("verifications.tracereplay.contextMenu.deleteTrace"));
			deleteTraceItem.setOnAction(
					event -> currentProject.getCurrentMachine().removeTraceFile(row.getItem().getLocation()));

			final ContextMenu menu = new ContextMenu(replayTraceItem, showErrorItem, deleteTraceItem);
			row.setContextMenu(menu);

			row.itemProperty().addListener((o, f, t) -> {
				showErrorItem.disableProperty().unbind();
				if (t != null) {
					showErrorItem.disableProperty().bind(t.statusProperty().isNotEqualTo(ReplayTrace.Status.FAILED));
					if (t.getStoredTrace() == null) {
						row.setDisable(true);
						traceTableView.refresh();
					}
				}
			});

			row.setOnMouseClicked(event -> {
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					this.traceChecker.replayTrace(row.getItem().getLocation(), true);
				}
			});
			return row;
		});
	}

	@FXML
	private void checkMachine() {
		traceChecker.checkMachine();
	}

	@FXML
	public synchronized void cancel() {
		traceChecker.cancelReplay();
	}

	@FXML
	private void loadTraceFromFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("verifications.tracereplay.traceLoader.dialog.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
		final List<String> allExts = new ArrayList<>();
		allExts.add("*.json");
		allExts.add("*.trace");
		allExts.sort(String::compareTo);
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("All supported trace formats", allExts),
				new ExtensionFilter("Trace (*.trace)", "*.trace"), new ExtensionFilter("Trace (*.json)", "*.json"));
		File traceFile = fileChooserManager.showOpenDialog(fileChooser, Kind.TRACES, stageManager.getCurrent());
		if (traceFile != null) {
			currentProject.getCurrentMachine().addTraceFile(traceFile.toPath());
		}
	}
}
