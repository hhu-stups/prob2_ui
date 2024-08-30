package de.prob2.ui.simulation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.FormalismType;
import de.prob.statespace.Trace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.csv.CSVWriter;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.CheckingStatusCell;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

@FXMLInjected
@Singleton
public final class SimulationTracesView extends Stage {
	public static final class SimulationTraceItem {
		private final SimulationItem parent;

		private final Trace trace;

		private final List<Integer> timestamps;

		private final CheckingStatus status;

		private final int traceLength;

		private final double estimatedValue;

		private final int index;

		public SimulationTraceItem(SimulationItem parent, Trace trace, List<Integer> timestamps, CheckingStatus status, int traceLength, double estimatedValue, int index) {
			this.parent = parent;
			this.trace = trace;
			this.timestamps = timestamps;
			this.status = status;
			this.traceLength = traceLength;
			this.estimatedValue = estimatedValue;
			this.index = index;
		}

		public SimulationItem getParent() {
			return parent;
		}

		public Trace getTrace() {
			return trace;
		}

		public List<Integer> getTimestamps() {
			return timestamps;
		}

		public int getIndex() {
			return index;
		}

		public CheckingStatus getStatus() {
			return status;
		}

		// Used via PropertyValueFactory
		public int getTraceLength() {
			return traceLength;
		}

		public double getEstimatedValue() {
			return estimatedValue;
		}
	}

	@FXML
	private TableView<SimulationTraceItem> traceTableView;
	@FXML
	private TableColumn<SimulationTraceItem, CheckingStatus> statusColumn;
	@FXML
	private TableColumn<SimulationTraceItem, String> traceColumn;
	@FXML
	private TableColumn<SimulationTraceItem, Float> estimatedValueColumn;
	@FXML
	private TableColumn<SimulationTraceItem, Integer> traceLengthColumn;
	@FXML
	private SplitPane splitPane;

	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final CurrentTrace currentTrace;
	private final I18n i18n;
	private final SimulationScenarioHandler simulationScenarioHandler;


	@Inject
	public SimulationTracesView(final StageManager stageManager, final FileChooserManager fileChooserManager, final CurrentTrace currentTrace, final I18n i18n, final SimulationScenarioHandler simulationScenarioHandler) {
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.currentTrace = currentTrace;
		this.i18n = i18n;
		this.simulationScenarioHandler = simulationScenarioHandler;
		stageManager.loadFXML(this, "simulation_generated_traces.fxml");
	}

	@FXML
	private void initialize() {
		initTableColumns();
		initTableRows();
		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);
		traceTableView.disableProperty().bind(partOfDisableBinding.or(currentTrace.stateSpaceProperty().isNull()));
	}

	public void setItems(SimulationItem item, List<Trace> traces, List<List<Integer>> timestamps, List<CheckingStatus> status, List<Double> estimatedValues) {
		ObservableList<SimulationTraceItem> items = FXCollections.observableArrayList();
		if(!estimatedValues.isEmpty()) {
			estimatedValueColumn.setVisible(true);
		}
		for (int i = 0; i < traces.size(); i++) {
			items.add(new SimulationTraceItem(item, traces.get(i), timestamps.get(i), status.get(i), traces.get(i).size(), estimatedValues.isEmpty() ? 0 : estimatedValues.get(i), i+1));
		}
		traceTableView.setItems(items);
	}

	private void initTableColumns() {
		statusColumn.setCellFactory(col -> new CheckingStatusCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		traceColumn.setCellValueFactory(features -> i18n.translateBinding("simulation.traces.view.name", features.getValue().getIndex()));
		traceLengthColumn.setCellValueFactory(new PropertyValueFactory<>("traceLength"));
		estimatedValueColumn.setCellValueFactory(new PropertyValueFactory<>("estimatedValue"));
	}

	private void initTableRows() {
		this.traceTableView.setRowFactory(param -> {
			final TableRow<SimulationTraceItem> row = new TableRow<>();

			final MenuItem loadTraceItem = new MenuItem(i18n.translate("simulation.contextMenu.loadTrace"));
			loadTraceItem.setOnAction(e -> simulationScenarioHandler.loadTrace(row.getItem()));

			final MenuItem playTraceItem = new MenuItem(i18n.translate("simulation.contextMenu.play"));
			playTraceItem.setOnAction(e -> simulationScenarioHandler.playTrace(row.getItem()));

			final MenuItem saveTraceItem = new MenuItem(i18n.translate("simulation.contextMenu.saveTrace"));
			saveTraceItem.setOnAction(e -> simulationScenarioHandler.saveTrace(row.getItem()));

			final MenuItem saveTimedTraceItem = new MenuItem(i18n.translate("simulation.contextMenu.saveTimedTrace"));
			saveTimedTraceItem.setOnAction(e -> simulationScenarioHandler.saveTimedTrace(row.getItem()));

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(loadTraceItem, playTraceItem, saveTraceItem, saveTimedTraceItem)));

			row.setOnMouseClicked(event -> {
				SimulationTraceItem item = row.getItem();
				if (item == null) {
					return;
				}
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					this.currentTrace.set(item.getTrace());
				}
			});

			return row;
		});
	}

	public void refresh() {
		this.traceTableView.refresh();
	}

	@FXML
	private void exportCSV() throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("simulation.generatedTraces.stage.title"));
		fileChooser.setInitialFileName("SimulationStatistics.csv");
		fileChooser.getExtensionFilters().add(fileChooserManager.getCsvFilter());
		Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.SIMULATION, stageManager.getCurrent());
		if (path != null) {
			try (CSVWriter csvWriter = new CSVWriter(Files.newBufferedWriter(path))) {
				csvWriter.header("Status", "Trace", "Trace Length", "Estimated Value");

				int i = 1;
				for (SimulationTraceItem traceItem : traceTableView.getItems()) {
					csvWriter.record(traceItem.getStatus(), String.format("Trace %s", i), traceItem.getTraceLength(), traceItem.getEstimatedValue());
				}
			}
		}
	}
}
