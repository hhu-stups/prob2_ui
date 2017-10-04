package de.prob2.ui.verifications.tracereplay;

import java.io.File;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
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

	private final CurrentProject currentProject;
	private final TraceLoader traceLoader;

	@Inject
	private TraceReplayView(final StageManager stageManager, final CurrentProject currentProject,
			final TraceLoader traceLoader) {
		this.currentProject = currentProject;
		this.traceLoader = traceLoader;
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}

	@FXML
	private void initialize() {
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusIcon"));
		statusColumn.setStyle( "-fx-alignment: CENTER;");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		currentProject.currentMachineProperty().addListener((observable, from, to) -> updateTraceTableView(to));
	}

	private void updateTraceTableView(Machine machine) {
		traceTableView.getItems().clear();
		if (machine != null) {
			for (File traceFile : machine.getTraces()) {
				ReplayTrace trace = traceLoader.loadTrace(traceFile);
				traceTableView.getItems().add(new ReplayTraceItem(trace, traceFile.getName()));
			}
		}
	}
}
