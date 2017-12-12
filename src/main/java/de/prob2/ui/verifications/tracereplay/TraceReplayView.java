package de.prob2.ui.verifications.tracereplay;

import java.io.File;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.verifications.tracereplay.ReplayTrace.Status;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

@Singleton
public class TraceReplayView extends ScrollPane {

	@FXML
	private TableView<ReplayTraceItem> traceTableView;
	@FXML
	private TableColumn<ReplayTraceItem, FontAwesomeIconView> statusColumn;
	@FXML
	private TableColumn<ReplayTraceItem, String> nameColumn;
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
	private final TraceChecker traceChecker;
	private final Injector injector;
	private final ResourceBundle bundle;

	@Inject
	private TraceReplayView(final StageManager stageManager, final CurrentProject currentProject,
			final TraceChecker traceChecker, final Injector injector, final ResourceBundle bundle) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.traceChecker = traceChecker;
		this.injector = injector;
		this.bundle = bundle;
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}

	@FXML
	private void initialize() {
		helpButton.setHelpContent("Verification.md.html");
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusIcon"));
		statusColumn.setStyle("-fx-alignment: CENTER;");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

		this.traceChecker.getReplayTraces().addListener((MapChangeListener<File, ReplayTrace>) c -> {
			if (c.wasAdded()) {
				traceTableView.getItems().add(new ReplayTraceItem(c.getValueAdded(), c.getKey()));
			}
			if (c.wasRemoved()) {
				traceTableView.getItems().setAll(traceTableView.getItems().stream()
						.filter(traceItem -> !c.getKey().equals(traceItem.getLocation())).collect(Collectors.toList()));
			}
		});

		initTableRows();

		currentProject.currentMachineProperty()
				.addListener((observable, from, to) -> loadTraceButton.setDisable(to == null));
		traceChecker.currentJobThreadsProperty()
				.addListener((observable, from, to) -> cancelButton.setDisable(to.isEmpty()));
		traceTableView.itemsProperty().get()
				.addListener((ListChangeListener<ReplayTraceItem>) c -> checkButton.setDisable(c.getList().isEmpty()));

		bindIconSizeToFontSize();

	}

	private void initTableRows() {
		this.traceTableView.setRowFactory(param -> {
			final TableRow<ReplayTraceItem> row = new TableRow<>();

			final MenuItem replayTraceItem = new MenuItem(
					bundle.getString("verifications.tracereplay.contextMenu.replayTrace"));
			replayTraceItem.setOnAction(event -> this.traceChecker.replayTrace(row.getItem().getLocation(), true));

			final MenuItem showErrorItem = new MenuItem(
					bundle.getString("verifications.tracereplay.contextMenu.showError"));
			showErrorItem.setOnAction(
					event -> stageManager.makeExceptionAlert("", row.getItem().getTrace().getError()).showAndWait());
			showErrorItem.setDisable(true);

			final MenuItem deleteTraceItem = new MenuItem(
					bundle.getString("verifications.tracereplay.contextMenu.deleteTrace"));
			deleteTraceItem.setOnAction(
					event -> currentProject.getCurrentMachine().removeTraceFile(row.getItem().getLocation()));

			final ContextMenu menu = new ContextMenu(replayTraceItem, showErrorItem, deleteTraceItem);
			row.setContextMenu(menu);

			row.itemProperty().addListener((o, f, t) -> {
				if (t == null) {
					return;
				}
				t.getTrace().getStatus().addListener((observable, from, to) -> {
					if (to == Status.FAILED) {
						showErrorItem.setDisable(false);
					} else {
						showErrorItem.setDisable(true);
					}
				});
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
		fileChooser.setInitialDirectory(currentProject.getLocation());
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Trace (*.trace)", "*.trace"));
		File traceFile = fileChooser.showOpenDialog(stageManager.getCurrent());
		if (traceFile != null) {
			currentProject.getCurrentMachine().addTraceFile(traceFile);
		}
	}

	private void bindIconSizeToFontSize() {
		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (checkButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
		((FontAwesomeIconView) (cancelButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
		statusColumn.minWidthProperty().bind(fontsize.multiply(6.0));
		statusColumn.maxWidthProperty().bind(fontsize.multiply(6.0));
	}
}
