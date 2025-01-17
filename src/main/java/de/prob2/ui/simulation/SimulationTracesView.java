package de.prob2.ui.simulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.FormalismType;
import de.prob.statespace.Trace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

@FXMLInjected
@Singleton
public final class SimulationTracesView extends Stage {
	public static final class SimulationTraceItem {
		private final SimulationItem parent;
		private final SimulationItem.Result result;
		private final int index;

		public SimulationTraceItem(SimulationItem parent, SimulationItem.Result result, int index) {
			this.parent = Objects.requireNonNull(parent, "parent");
			this.result = Objects.requireNonNull(result, "result");
			this.index = index;
		}

		public SimulationItem getParent() {
			return parent;
		}

		public Trace getTrace() {
			return this.result.getTraces().get(index);
		}

		public List<Integer> getTimestamps() {
			return this.result.getTimestamps().get(this.getIndex());
		}

		public int getIndex() {
			return index;
		}

		public int getDisplayedIndex() {
			return this.getIndex() + 1;
		}

		public CheckingStatus getStatus() {
			return this.result.getStatus();
		}

		// Used via PropertyValueFactory
		public int getTraceLength() {
			return this.getTrace().size();
		}

		public double getEstimatedValue() {
			var estimatedValues = this.result.getStats().getEstimatedValues();
			return estimatedValues.isEmpty() ? 0 : estimatedValues.get(index);
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

	public void setFromItem(SimulationItem item) {
		SimulationItem.Result result = (SimulationItem.Result)item.getResult();
		ObservableList<SimulationTraceItem> items = FXCollections.observableArrayList();
		if (!result.getStats().getEstimatedValues().isEmpty()) {
			estimatedValueColumn.setVisible(true);
		}
		for (int i = 0; i < result.getTraces().size(); i++) {
			items.add(new SimulationTraceItem(item, result, i));
		}
		traceTableView.setItems(items);
	}

	private void initTableColumns() {
		statusColumn.setCellFactory(col -> new CheckingStatusCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		traceColumn.setCellValueFactory(features -> i18n.translateBinding("simulation.traces.view.name", features.getValue().getDisplayedIndex()));
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
			CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
				.setHeader("Status", "Trace", "Trace Length", "Estimated Value")
				.build();
			try (CSVPrinter csvPrinter = csvFormat.print(path, StandardCharsets.UTF_8)) {
				int i = 1;
				for (SimulationTraceItem traceItem : traceTableView.getItems()) {
					csvPrinter.printRecord(traceItem.getStatus(), String.format("Trace %s", i), traceItem.getTraceLength(), traceItem.getEstimatedValue());
				}
			}
		}
	}
}
