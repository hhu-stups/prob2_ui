package de.prob2.ui.simulation;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.FormalismType;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.Translatable;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.table.SimulationItem;

import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
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
import javafx.stage.Stage;

@FXMLInjected
@Singleton
public class SimulationTracesView extends Stage {

	public static class SimulationTraceItem implements Translatable {

		private final SimulationItem parent;

		private final Trace trace;

		private final List<Integer> timestamps;

		private final Checked checked;

		private final int index;

		public SimulationTraceItem(SimulationItem parent, Trace trace, List<Integer> timestamps, Checked checked, int index) {
			this.parent = parent;
			this.trace = trace;
			this.timestamps = timestamps;
			this.checked = checked;
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

		public Checked getChecked() {
			return checked;
		}

		@Override
		public String getTranslationKey() {
			return "simulation.traces.view.name";
		}

		@Override
		public Object[] getTranslationArguments() {
			return new Object[]{index};
		}
	}

	@FXML
	private TableView<SimulationTraceItem> traceTableView;
	@FXML
	private TableColumn<SimulationTraceItem, Checked> statusColumn;
	@FXML
	private TableColumn<SimulationTraceItem, String> traceColumn;
	@FXML
	private SplitPane splitPane;

	private final CurrentTrace currentTrace;
	private final I18n i18n;
	private final SimulationScenarioHandler simulationScenarioHandler;


	@Inject
	public SimulationTracesView(final StageManager stageManager, final CurrentTrace currentTrace, final I18n i18n, final SimulationScenarioHandler simulationScenarioHandler) {
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

	public void setItems(SimulationItem item, List<Trace> traces, List<List<Integer>> timestamps, List<Checked> status) {
		ObservableList<SimulationTraceItem> items = FXCollections.observableArrayList();
		for (int i = 0; i < traces.size(); i++) {
			items.add(new SimulationTraceItem(item, traces.get(i), timestamps.get(i), status.get(i), i+1));
		}
		traceTableView.setItems(items);
	}

	private void initTableColumns() {
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		traceColumn.setCellValueFactory(features -> i18n.translateBinding(features.getValue()));
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
}
