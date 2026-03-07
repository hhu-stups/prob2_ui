package de.prob2.ui.animation.tracereplay.statistics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.SimulationItem;
import de.prob2.ui.simulation.SimulationScenarioHandler;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.CheckingStatusCell;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.prob.statespace.LoadedMachine;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.prob.statespace.Transition.INITIALISE_MACHINE_NAME;
import static de.prob.statespace.Transition.SETUP_CONSTANTS_NAME;

@FXMLInjected
@Singleton
public final class TraceStatisticsView extends Stage {
	public static final class TraceStatisticsItem {

		private final String name;
		private final int totalComputations;
		private final int effectObserved;
		private final float percentage;

		public TraceStatisticsItem(String name, int totalComputations, int effectObserved, float percentage) {
			this.name = name;
			this.totalComputations = totalComputations;
			this.effectObserved = effectObserved;
			this.percentage = percentage;
		}

		public String getName() {
			return name;
		}

		public int getTotalComputations() {
			return totalComputations;
		}

		public int getEffectObserved() {
			return effectObserved;
		}

		public float getPercentage() {
			return percentage;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceStatisticsView.class);

	@FXML
	private ChoiceBox<String> cbOperation;

	@FXML
	private Button btEvaluate;

	@FXML
	private TextField tfDesiredEffects;

	@FXML
	private TextField tfComputations;

	@FXML
	private TableView<TraceStatisticsItem> statisticsTableView;
	@FXML
	private TableColumn<TraceStatisticsItem, String> traceColumn;
	@FXML
	private TableColumn<TraceStatisticsItem, Integer> totalComputationsColumn;
	@FXML
	private TableColumn<TraceStatisticsItem, Integer> effectColumn;
	@FXML
	private TableColumn<TraceStatisticsItem, Float> percentageColumn;

	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final I18n i18n;


	@Inject
	public TraceStatisticsView(final StageManager stageManager, final FileChooserManager fileChooserManager, final CurrentTrace currentTrace, final CurrentProject currentProject, final I18n i18n) {
		super();
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.i18n = i18n;
		stageManager.loadFXML(this, "trace_statistics_view.fxml");
	}

	@FXML
	private void initialize() {
		initTableColumns();
		statisticsTableView.disableProperty().bind(currentTrace.stateSpaceProperty().isNull());
		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			clear();
			setup();
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());
		btEvaluate.disableProperty().bind(cbOperation.getSelectionModel().selectedItemProperty().isNull());
	}

	private void setup() {
		if(currentTrace.getStateSpace() == null) {
			return;
		}
		LoadedMachine loadedMachine = currentTrace.getStateSpace().getLoadedMachine();
		List<String> operations = new ArrayList<>();
		operations.add(SETUP_CONSTANTS_NAME);
		operations.add(INITIALISE_MACHINE_NAME);
		operations.addAll(loadedMachine.getOperationNames().stream()
				.map(loadedMachine::getMachineOperationInfo)
				.filter(OperationInfo::isTopLevel)
				.map(OperationInfo::getOperationName).toList());
		cbOperation.getItems().addAll(operations);
	}

	private void initTableColumns() {
		traceColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		totalComputationsColumn.setCellValueFactory(new PropertyValueFactory<>("totalComputations"));
		effectColumn.setCellValueFactory(new PropertyValueFactory<>("effectObserved"));
		percentageColumn.setCellValueFactory(new PropertyValueFactory<>("percentage"));
	}

	public void refresh() {
		this.statisticsTableView.refresh();
	}

	private void clear() {
		this.statisticsTableView.getItems().clear();
		cbOperation.getItems().clear();
	}

	@FXML
	private void exportCSV() {

	}

	@FXML
	private void evaluate() {
		statisticsTableView.getItems().clear();
		String selectedOperation = cbOperation.getSelectionModel().getSelectedItem();
		String computations = tfComputations.getText();
		String desiredEffects = tfDesiredEffects.getText();
		Machine machine = currentProject.getCurrentMachine();
		ObservableList<ReplayTrace> traces = machine.getTraces();
		for(ReplayTrace replayTrace : traces) {
			if(replayTrace.selected()) {
				Trace trace = replayTrace.getTrace();
				if(trace == null) {
					continue;
				}
				int numberComputations = 0;
				int numberDesiredEffects = 0;
				List<Transition> transitions = trace.getTransitionList();
				for(Transition transition : transitions) {
					if(transition.getName().equals(selectedOperation)) {
						State destination = transition.getDestination();
						String computationsResult = destination.eval(computations).toString();
						String desiredEffectsResult = destination.eval(desiredEffects).toString();
						if("TRUE".equals(desiredEffectsResult)) {
							try {
								numberComputations += Integer.parseInt(computationsResult);
							} catch (NumberFormatException e) {
								final Alert alert = stageManager.makeExceptionAlert(e, "animation.tracereplay.statistics.alert.header",
										"animation.tracereplay.statistics.alert.content");
								alert.initOwner(this);
								alert.show();
								throw new IllegalArgumentException("Could not evaluate formula for trace statistics: Not a valid integer", e);
							}
							numberDesiredEffects++;
						} else {
							try {
								numberComputations += Integer.parseInt(computationsResult);
								numberDesiredEffects += Integer.parseInt(desiredEffectsResult);
							} catch (NumberFormatException e) {
								final Alert alert = stageManager.makeExceptionAlert(e, "animation.tracereplay.statistics.alert.header",
										"animation.tracereplay.statistics.alert.content");
								alert.initOwner(this);
								alert.show();
								throw new IllegalArgumentException("Could not evaluate formula for trace statistics: Not a valid integer", e);
							}
						}
					}
				}
				statisticsTableView.getItems().add(new TraceStatisticsItem(replayTrace.getName(), numberComputations, numberDesiredEffects, numberComputations == 0 ? 0.0f : 100.0f * numberDesiredEffects / numberComputations));
			}
		}
	}
}
