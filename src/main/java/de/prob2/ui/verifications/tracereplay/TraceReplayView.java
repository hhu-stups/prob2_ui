package de.prob2.ui.verifications.tracereplay;

import java.io.File;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
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
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusIcon"));
		statusColumn.setStyle("-fx-alignment: CENTER;");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

		this.traceChecker.getReplayTraces().addListener((MapChangeListener<File,ReplayTrace>) c -> {
			System.out.print(c.getKey() + ",");
			if (c.wasAdded()) {
				traceTableView.getItems().add(new ReplayTraceItem(c.getValueAdded(), c.getKey()));
			}
			if (c.wasRemoved()) {
				traceTableView.getItems().stream().filter(traceItem -> !c.getKey().equals(traceItem.getLocation()))
				.collect(Collectors.toList());
			}
		});

		this.traceTableView.setRowFactory(param -> {
			final ContextMenu menu = new ContextMenu();
			final TableRow<ReplayTraceItem> row = new TableRow<>();
			row.setContextMenu(menu);
			//
			final MenuItem item = new MenuItem("Replay Trace"); // TODO: i18n
			menu.getItems().add(item);
			//
			item.setOnAction(event -> this.traceChecker.replayTrace(row.getItem().getLocation(), true));
			//
			return row;
		});

		currentProject.currentMachineProperty()
				.addListener((observable, from, to) -> loadTraceButton.setDisable(to == null));
		traceChecker.currentJobThreadsProperty()
				.addListener((observable, from, to) -> cancelButton.setDisable(to.isEmpty()));
		traceTableView.itemsProperty().get()
				.addListener((ListChangeListener<ReplayTraceItem>) c -> checkButton.setDisable(c.getList().isEmpty()));

		bindIconSizeToFontSize();
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
		currentProject.getCurrentMachine().addTraceFile(traceFile);
	}

	private void bindIconSizeToFontSize() {
		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (checkButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
		((FontAwesomeIconView) (cancelButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
		statusColumn.minWidthProperty().bind(fontsize.multiply(6.0));
		statusColumn.maxWidthProperty().bind(fontsize.multiply(6.0));
	}
}
