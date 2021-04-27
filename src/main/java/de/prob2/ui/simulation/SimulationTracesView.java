package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.FormalismType;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import de.prob2.ui.simulation.simulators.Scheduler;
import de.prob2.ui.simulation.simulators.SimulationCreator;
import de.prob2.ui.simulation.simulators.SimulationSaver;
import de.prob2.ui.simulation.table.SimulationItem;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

@FXMLInjected
@Singleton
public class SimulationTracesView extends Stage {

	private static class SimulationTraceItem {

		private final SimulationItem parent;

		private final Trace trace;

		private final List<Integer> timestamps;

		private final int index;

		public SimulationTraceItem(SimulationItem parent, Trace trace, List<Integer> timestamps, int index) {
			this.parent = parent;
			this.trace = trace;
			this.timestamps = timestamps;
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
	private final CurrentProject currentProject;
	private final ResourceBundle bundle;
	private final Injector injector;

	private SimulatorStage simulatorStage;

	@Inject
	public SimulationTracesView(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject,
								final ResourceBundle bundle, final Injector injector) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
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

	public void setItems(SimulationItem item, List<Trace> traces, List<List<Integer>> timestamps) {
		ObservableList<SimulationTraceItem> items = FXCollections.observableArrayList();
		for(int i = 1; i <= traces.size(); i++) {
			items.add(new SimulationTraceItem(item, traces.get(i - 1), timestamps.get(i-1), i));
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
				Trace trace = new Trace(currentTrace.getStateSpace());
				currentTrace.set(trace);
				SimulationTraceItem traceItem = row.getItem();
				SimulationConfiguration config = injector.getInstance(SimulationCreator.class).createConfiguration(traceItem.getTrace(), traceItem.getTimestamps(), false, SimulationConfiguration.metadataBuilder().build());
				RealTimeSimulator realTimeSimulator = injector.getInstance(RealTimeSimulator.class);

				try {
					realTimeSimulator.initSimulator(config);
				} catch (IOException exception) {
					exception.printStackTrace();
					// TODO: Handle error
				}
				realTimeSimulator.setupBeforeSimulation(trace);
				trace.setExploreStateByDefault(false);
				simulatorStage.simulate();
				trace.setExploreStateByDefault(true);
			});

			final MenuItem saveTraceItem = new MenuItem(bundle.getString("simulation.contextMenu.saveTrace"));

			saveTraceItem.setOnAction(e -> {
				SimulationTraceItem item = row.getItem();
				if(item == null) {
					return;
				}
				TraceFileHandler traceSaver = injector.getInstance(TraceFileHandler.class);
				if (currentTrace.get() != null) {
					Trace trace = item.getTrace();
					try {
						traceSaver.save(trace, currentProject.getCurrentMachine());
					} catch (IOException exception) {
						exception.printStackTrace();
						// TODO: Handle error
					}
				}
			});

			final MenuItem saveTimedTraceItem = new MenuItem(bundle.getString("simulation.contextMenu.saveTimedTrace"));

			saveTimedTraceItem.setOnAction(e -> {
				SimulationTraceItem item = row.getItem();
				if(item == null) {
					return;
				}
				SimulationSaver simulationSaver = injector.getInstance(SimulationSaver.class);
				try {
					String createdBy = "Simulation: " + item.getParent().getTypeAsName() + "; " + item.getParent().getConfiguration();
					simulationSaver.saveConfiguration(item.getTrace(), item.getTimestamps(), createdBy);
				} catch (IOException exception) {
					exception.printStackTrace();
					// TODO: Handle error
				}
			});

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu) null)
					.otherwise(new ContextMenu(loadTraceItem, playTraceItem, saveTraceItem, saveTimedTraceItem)));

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
