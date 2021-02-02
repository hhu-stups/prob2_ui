package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.FormalismType;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.simulators.Scheduler;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import java.util.List;
import java.util.ResourceBundle;

@FXMLInjected
@Singleton
public class SimulationTracesView extends Stage {

	private static class SimulationTraceItem {

		private Trace trace;

		private int index;

		public SimulationTraceItem(Trace trace, int index) {
			this.trace = trace;
			this.index = index;
		}

		public Trace getTrace() {
			return trace;
		}

		public int getIndex() {
			return index;
		}

		public String getName() {
			return String.format("Trace %s", index);
		}
	}

	@FXML
	private TableView<SimulationTraceItem> traceTableView;
	@FXML
	private TableColumn<SimulationTraceItem, String> traceColumn;
	@FXML
	private SplitPane splitPane;

	private final CurrentTrace currentTrace;
	private final ResourceBundle bundle;
	private final Injector injector;

	private SimulatorStage simulatorStage;

	@Inject
	public SimulationTracesView(final StageManager stageManager, final CurrentTrace currentTrace, final ResourceBundle bundle, final Injector injector) {
		this.currentTrace = currentTrace;
		this.bundle = bundle;
		this.injector = injector;
		stageManager.loadFXML(this, "simulation_generated_traces.fxml");
	}

	@FXML
	private void initialize() {
		initTableColumns();
		initTableRows();
		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);
		traceTableView.disableProperty().bind(partOfDisableBinding.or(currentTrace.stateSpaceProperty().isNull()));
	}

	public void setItems(List<Trace> traces) {
		ObservableList<SimulationTraceItem> items = FXCollections.observableArrayList();
		for(int i = 1; i <= traces.size(); i++) {
			items.add(new SimulationTraceItem(traces.get(i - 1), i));
		}
		traceTableView.setItems(items);
	}

	private void initTableColumns() {
		traceColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue().getName()));
	}

	private void initTableRows() {
		this.traceTableView.setRowFactory(param -> {
			final TableRow<SimulationTraceItem> row = new TableRow<>();

			final MenuItem loadTraceItem = new MenuItem(bundle.getString("simulation.contextMenu.loadTrace"));

			loadTraceItem.setOnAction(e -> {
				SimulationTraceItem item = row.getItem();
				if(item == null) {
					return;
				}
				this.currentTrace.set(item.getTrace());
			});

			final MenuItem playTraceItem = new MenuItem(bundle.getString("simulation.contextMenu.play"));

			playTraceItem.setOnAction(e -> {
				/*Trace trace = new Trace(currentTrace.getStateSpace());
				PersistentTrace persistentTrace = new PersistentTrace(row.getItem().getTrace(), row.getItem().getTrace().getCurrent().getIndex() + 1);
				TraceSimulator traceSimulator = new TraceSimulator(currentTrace, injector.getInstance(Scheduler.class), trace, persistentTrace);
				if(traceSimulator.isRunning()) {
					traceSimulator.stop();
				}
				simulatorStage.initSimulator(traceSimulator);
				trace.setExploreStateByDefault(false);
				simulatorStage.simulate(traceSimulator);
				trace.setExploreStateByDefault(true);*/
				// TODO: Trace Replay as a special kind of simulation
			});

            row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu) null)
					.otherwise(new ContextMenu(loadTraceItem, playTraceItem)));

			row.setOnMouseClicked(event -> {
				SimulationTraceItem item = row.getItem();
				if(item == null) {
					return;
				}
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					this.currentTrace.set(item.getTrace());
				}
			});

			return row;
		});
	}

	public void setSimulatorStage(final SimulatorStage simulatorStage) {
		this.simulatorStage = simulatorStage;
	}

    public void refresh() {
        this.traceTableView.refresh();
    }
}
