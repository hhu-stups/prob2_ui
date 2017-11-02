package de.prob2.ui.verifications.tracereplay;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

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

	private final CurrentProject currentProject;
	private final TraceLoader traceLoader;
	private final TraceChecker traceChecker;
	private final Injector injector;

	@Inject
	private TraceReplayView(final StageManager stageManager, final CurrentProject currentProject,
			final TraceLoader traceLoader, final TraceChecker traceChecker, final Injector injector) {
		this.currentProject = currentProject;
		this.traceLoader = traceLoader;
		this.traceChecker = traceChecker;
		this.injector = injector;
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}

	@FXML
	private void initialize() {
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusIcon"));
		statusColumn.setStyle("-fx-alignment: CENTER;");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if (to == null) {
				return;
			}
			updateTraceTableView(to);
			to.getTraces().addListener((ListChangeListener<File>) c -> {
				while (c.next()) {
					addToTraceTableView(c.getAddedSubList());
					removeFromTraceTableView(c.getRemoved());
				}
			});
		});

		this.traceTableView.setRowFactory(param -> {

			final ContextMenu menu = new ContextMenu();
			final TableRow<ReplayTraceItem> row = new TableRow<>();
			row.setContextMenu(menu);
			//
			final MenuItem item = new MenuItem("Replay Trace"); // TODO: i18n
			menu.getItems().add(item);
			//
			item.setOnAction(event -> this.traceChecker.replayTrace(row.getItem().getTrace(), true));
			//
			return row;
		});

		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (checkButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
		((FontAwesomeIconView) (cancelButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
		statusColumn.minWidthProperty().bind(fontsize.multiply(6.0));
		statusColumn.maxWidthProperty().bind(fontsize.multiply(6.0));

		traceChecker.currentJobThreadsProperty().addListener((observable, from, to) -> {
			if (to.isEmpty()) {
				cancelButton.setDisable(true);
			} else {
				cancelButton.setDisable(false);
			}
		});
	}

    private void updateTraceTableView(Machine machine) {
		traceTableView.getItems().clear();
		checkButton.setDisable(true);
		if (machine != null) {
			addToTraceTableView(machine.getTraces());
			checkButton.setDisable(false);
		}
	}

	private void addToTraceTableView(List<? extends File> traceFiles) {
		for (File traceFile : traceFiles) {
			ReplayTrace trace = traceLoader.loadTrace(traceFile);
			traceTableView.getItems().add(new ReplayTraceItem(trace, traceFile));
		}
	}

	private void removeFromTraceTableView(List<? extends File> traceFiles) {
		traceTableView.getItems().stream().filter(traceItem -> !traceFiles.contains(traceItem.getLocation()))
				.collect(Collectors.toList());
		for (ReplayTraceItem traceItem : traceTableView.getItems()) {
			for (File traceFile : traceFiles) {
				if (traceItem.getLocation().equals(traceFile)) {

				}
			}
		}
	}

	@FXML
	private void checkMachine() {
		traceChecker.checkMachine(traceTableView.getItems());
	}

	@FXML
	public synchronized void cancel() {
		traceChecker.cancelReplay();
	}
}
