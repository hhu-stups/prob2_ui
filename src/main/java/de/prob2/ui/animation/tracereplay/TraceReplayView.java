package de.prob2.ui.animation.tracereplay;

import java.nio.file.Path;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.config.FileChooserManager.Kind;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

@FXMLInjected
@Singleton
public class TraceReplayView extends ScrollPane {
	private static final String TRACE_FILE_ENDING = "*.prob2trace";

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
	@FXML
	private TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final TraceChecker traceChecker;
	private final ResourceBundle bundle;
	private final FileChooserManager fileChooserManager;

	@Inject
	private TraceReplayView(final StageManager stageManager, final CurrentProject currentProject,
			final CurrentTrace currentTrace, final TraceChecker traceChecker, final ResourceBundle bundle,
			final FileChooserManager fileChooserManager) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.traceChecker = traceChecker;
		this.bundle = bundle;
		this.fileChooserManager = fileChooserManager;
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}

	private static void updateStatusIcon(final FontAwesomeIconView iconView, final Checked status) {
		iconView.getStyleClass().add("status-icon");
		switch (status) {
		case SUCCESS:
			iconView.setIcon(FontAwesomeIcon.CHECK);
			iconView.setFill(Color.GREEN);
			break;

		case FAIL:
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

		initTableColumns();
		initTableRows();

		final SetChangeListener<Path> listener = c -> {
			if (c.wasAdded()) {
				traceTableView.getItems().add(new ReplayTrace(c.getElementAdded()));
			}
			if (c.wasRemoved()) {
				removeFromTraceTableView(c.getElementRemoved());
			}
		};

		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if (from != null) {
				from.getTraceFiles().removeListener(listener);
			}
			traceTableView.getItems().clear();
			if (to != null) {
				to.getTraceFiles().forEach(tracePath -> {
					traceTableView.getItems().add(new ReplayTrace(tracePath));
					if (!tracePath.toString().endsWith(TRACE_FILE_ENDING.substring(1))) {
						stageManager.makeAlert(AlertType.WARNING, "",
								"animation.tracereplay.view.alerts.wrongFileExtensionWarning.content",
								TRACE_FILE_ENDING, tracePath).showAndWait();
					}
				});
				to.getTraceFiles().addListener(listener);
			}
		});

		loadTraceButton.disableProperty().bind(currentProject.currentMachineProperty().isNull());
		cancelButton.disableProperty().bind(traceChecker.currentJobThreadsProperty().emptyProperty());
		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if (to != null) {
				checkButton.disableProperty()
						.bind(currentTrace.stateSpaceProperty().isNull()
								.or(traceChecker.currentJobThreadsProperty().emptyProperty().not())
								.or(to.tracesProperty().emptyProperty()));
			}
		});
		traceTableView.disableProperty().bind(currentTrace.stateSpaceProperty().isNull());
	}

	private void initTableColumns() {
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(CheckingType.REPLAY));
		CheckBox selectAll = new CheckBox();
		selectAll.setSelected(true);
		selectAll.selectedProperty().addListener((observable, from, to) -> {
			for (ReplayTrace item : traceTableView.getItems()) {
				item.setSelected(to);
				traceTableView.refresh();
			}
		});
		shouldExecuteColumn.setGraphic(selectAll);

		statusColumn.setCellValueFactory(features -> {
			final ReplayTrace trace = features.getValue();

			final FontAwesomeIconView statusIcon = new FontAwesomeIconView();
			trace.statusProperty().addListener((o, from, to) -> updateStatusIcon(statusIcon, to));
			updateStatusIcon(statusIcon, trace.getChecked());

			final ProgressIndicator replayProgress = new ProgressBar();
			replayProgress.progressProperty().bind(trace.progressProperty());

			return Bindings.when(trace.progressProperty().isEqualTo(-1)).<Node>then(statusIcon)
					.otherwise(replayProgress);
		});

		nameColumn.setCellValueFactory(
				features -> new SimpleStringProperty(features.getValue().getLocation().getFileName().toString()));
	}

	private void initTableRows() {
		this.traceTableView.setRowFactory(param -> {
			final TableRow<ReplayTrace> row = new TableRow<>();

			final MenuItem replayTraceItem = new MenuItem(
					bundle.getString("animation.tracereplay.view.contextMenu.replayTrace"));
			replayTraceItem.setOnAction(event -> this.traceChecker.check(row.getItem(), true));
			replayTraceItem.setDisable(true);

			final MenuItem showErrorItem = new MenuItem(
					bundle.getString("animation.tracereplay.view.contextMenu.showError"));
			showErrorItem.setOnAction(event -> stageManager
					.makeAlert(AlertType.ERROR, "animation.tracereplay.alerts.traceReplayError.header",
							row.getItem().getErrorMessageBundleKey(), row.getItem().getErrorMessageParams())
					.showAndWait());
			showErrorItem.setDisable(true);

			final MenuItem deleteTraceItem = new MenuItem(
					bundle.getString("animation.tracereplay.view.contextMenu.removeTrace"));
			deleteTraceItem.setOnAction(
					event -> currentProject.getCurrentMachine().removeTraceFile(row.getItem().getLocation()));

			final ContextMenu menu = new ContextMenu(replayTraceItem, showErrorItem, deleteTraceItem);
			row.setContextMenu(menu);

			row.itemProperty().addListener((observable, from, to) -> {
				showErrorItem.disableProperty().unbind();
				if (to != null) {
					replayTraceItem.disableProperty()
							.bind(row.emptyProperty().or(traceChecker.currentJobThreadsProperty().emptyProperty().not())
									.or(row.getItem().selectedProperty().not()));
					showErrorItem.disableProperty().bind(to.statusProperty().isNotEqualTo(Checked.FAIL));
					row.setTooltip(new Tooltip(row.getItem().getLocation().toString()));
				}
			});

			row.setOnMouseClicked(event -> {
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					this.traceChecker.check(row.getItem(), true);
				}
			});

			return row;
		});
	}

	@FXML
	private void checkMachine() {
		traceChecker.checkAll(traceTableView.getItems());
	}

	@FXML
	public synchronized void cancel() {
		traceChecker.cancelReplay();
	}

	@FXML
	private void loadTraceFromFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("animation.tracereplay.fileChooser.loadTrace.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
		fileChooser.getExtensionFilters()
		.add(new ExtensionFilter(
				String.format(bundle.getString("common.fileChooser.fileTypes.proB2Trace"), TRACE_FILE_ENDING),
				TRACE_FILE_ENDING));
		Path traceFile = fileChooserManager.showOpenDialog(fileChooser, Kind.TRACES, stageManager.getCurrent());
		if (traceFile != null) {
			Path relative = currentProject.getLocation().relativize(traceFile);
			currentProject.getCurrentMachine().addTraceFile(relative);
		}
	}
	
	public void refresh() {
		ObservableList<ReplayTrace> traces = FXCollections.observableArrayList(traceTableView.getItems());
		traceTableView.getItems().clear();
		traceChecker.cancelReplay();
		traces.forEach(trace -> trace.setChecked(Checked.NOT_CHECKED));
		traceTableView.setItems(traces);
		traceTableView.refresh();
	}

	private void removeFromTraceTableView(Path tracePath) {
		traceTableView.getItems().removeIf(trace -> trace.getLocation().equals(tracePath));
	}
}
