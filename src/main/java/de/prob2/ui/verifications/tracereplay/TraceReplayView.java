package de.prob2.ui.verifications.tracereplay;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.tracereplay.ReplayTrace.Status;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

@Singleton
public class TraceReplayView extends ScrollPane {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceReplayView.class);

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
	private final Injector injector;

	@Inject
	private TraceReplayView(final StageManager stageManager, final CurrentProject currentProject,
			final TraceLoader traceLoader, final Injector injector) {
		this.currentProject = currentProject;
		this.traceLoader = traceLoader;
		this.injector = injector;
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}

	@FXML
	private void initialize() {
		statusColumn.setCellValueFactory(new PropertyValueFactory<ReplayTraceItem, FontAwesomeIconView>("statusIcon"));
		statusColumn.setStyle("-fx-alignment: CENTER;");
		nameColumn.setCellValueFactory(new PropertyValueFactory<ReplayTraceItem, String>("name"));

		currentProject.currentMachineProperty().addListener((observable, from, to) -> updateTraceTableView(to));

		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (checkButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
		((FontAwesomeIconView) (cancelButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
		statusColumn.minWidthProperty().bind(fontsize.multiply(6.0));
		statusColumn.maxWidthProperty().bind(fontsize.multiply(6.0));
	}

	private void updateTraceTableView(Machine machine) {
		traceTableView.getItems().clear();
		checkButton.setDisable(true);
		if (machine != null) {
			for (File traceFile : machine.getTraces()) {
				ReplayTrace trace = traceLoader.loadTrace(traceFile);
				traceTableView.getItems().add(new ReplayTraceItem(trace, traceFile.getName()));
			}
			checkButton.setDisable(false);
		}
	}

	@FXML
	private void checkMachine() {
		for (ReplayTraceItem traceItem : traceTableView.getItems()) {
			replayTrace(traceItem.getTrace());
		}
	}

	private void replayTrace(ReplayTrace trace) {
		StateSpace stateSpace = injector.getInstance(CurrentTrace.class).getStateSpace();
		Trace t = new Trace(stateSpace);

		try {
			for (ReplayTransition transition : trace.getTransitionList()) {
				t = t.addTransitionWith(transition.getName(), transition.getParameters());
			}
		} catch (IllegalArgumentException e) {
			trace.setStatus(Status.FAILED);
			return;
		}
		trace.setStatus(Status.SUCCESSFUL);
	}
}
