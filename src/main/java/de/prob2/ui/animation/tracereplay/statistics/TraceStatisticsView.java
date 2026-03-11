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
import java.util.Comparator;
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
		private final String totalComputations;
		private final String effectObserved;
		private final String percentage;

		public TraceStatisticsItem(String name, String totalComputations, String effectObserved, String percentage) {
			this.name = name;
			this.totalComputations = totalComputations;
			this.effectObserved = effectObserved;
			this.percentage = percentage;
		}

		public String getName() {
			return name;
		}

		public String getTotalComputations() {
			return totalComputations;
		}

		public String getEffectObserved() {
			return effectObserved;
		}

		public String getPercentage() {
			return percentage;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceStatisticsView.class);

	@FXML
	private ChoiceBox<String> cbOperation;

	@FXML
	private Button btEvaluate;

	@FXML
	private Button btAdd;

	@FXML
	private Button btEdit;

	@FXML
	private TextField tfDesiredEffects;

	@FXML
	private TextField tfComputations;

	@FXML
	private TableView<TraceStatisticsFormulasItem> formulaTableView;

	@FXML
	private TableColumn<TraceStatisticsFormulasItem, CheckingStatus> statusColumn;

	@FXML
	private TableColumn<TraceStatisticsFormulasItem, String> formulaOperationColumn;

	@FXML
	private TableColumn<TraceStatisticsFormulasItem, String> formulaComputationsColumn;

	@FXML
	private TableColumn<TraceStatisticsFormulasItem, String> formulaEffectColumn;

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
		initTable();
		statisticsTableView.disableProperty().bind(currentTrace.stateSpaceProperty().isNull());
		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			clear();
			setup();
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());

		currentTrace.addListener((observable, from, to) -> {
			clear();
			setup();
		});

		btEvaluate.disableProperty().bind(cbOperation.getSelectionModel().selectedItemProperty().isNull());
		btAdd.disableProperty().bind(cbOperation.getSelectionModel().selectedItemProperty().isNull());
		btEdit.disableProperty().bind(formulaTableView.getSelectionModel().selectedItemProperty().isNull());
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
		Machine machine = currentProject.getCurrentMachine();
		if (machine != null) {
			formulaTableView.setItems(machine.getTraceStatisticsFormulas());
		} else {
			formulaTableView.setItems(FXCollections.emptyObservableList());
		}

		formulaTableView.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to == null) {
				return;
			}
			cbOperation.getSelectionModel().select(to.getOperation());
			tfComputations.setText(to.getComputations());
			tfDesiredEffects.setText(to.getDesiredEffects());
		});
	}

	private void initTable() {
		statusColumn.setCellFactory(col -> new CheckingStatusCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		formulaOperationColumn.setCellValueFactory(new PropertyValueFactory<>("operationWithId"));
		formulaComputationsColumn.setCellValueFactory(new PropertyValueFactory<>("computations"));
		formulaEffectColumn.setCellValueFactory(new PropertyValueFactory<>("desiredEffects"));

		traceColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		totalComputationsColumn.setCellValueFactory(new PropertyValueFactory<>("totalComputations"));
		effectColumn.setCellValueFactory(new PropertyValueFactory<>("effectObserved"));
		percentageColumn.setCellValueFactory(new PropertyValueFactory<>("percentage"));

		traceColumn.setCellFactory(col -> new TableCell<TraceStatisticsItem, String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if(empty || item == null) {
					setText(null);
					setStyle("");
				} else {
					setText(item);
					if(item.equals(i18n.translate("animation.tracereplay.statistics.table.special.mean")) || item.equals(i18n.translate("animation.tracereplay.statistics.table.special.sum"))) {
						setStyle("-fx-font-weight: bold;");
					} else {
						setStyle("");
					}
				}
			}
		});

		statisticsTableView.setSortPolicy(tv -> {
			Comparator<TraceStatisticsItem> comparator = (a, b) -> {
				boolean aSpecial = a.equals(i18n.translate("animation.tracereplay.statistics.table.special.mean")) || a.equals(i18n.translate("animation.tracereplay.statistics.table.special.sum"));
				boolean bSpecial = b.equals(i18n.translate("animation.tracereplay.statistics.table.special.mean")) || b.equals(i18n.translate("animation.tracereplay.statistics.table.special.sum"));

				if(aSpecial && !bSpecial) {
					return -1;
				}
				if(!aSpecial && bSpecial) {
					return 1;
				}
				return 0;
			};
			FXCollections.sort(tv.getItems(), tv.getComparator() == null ? comparator.reversed() : comparator.reversed().thenComparing(tv.getComparator()));
			return true;
		});

		this.formulaTableView.setRowFactory(param -> {
			TableRow<TraceStatisticsFormulasItem> row = new TableRow<>();

			row.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2) {
					this.evaluate();
				}
			});

			MenuItem editFormula = new MenuItem(i18n.translate("animation.tracereplay.statistics.editID"));
			editFormula.setOnAction(event -> {
				TraceStatisticsFormulasItem item = row.getItem();
				if (item == null) {
					return;
				}
				editID(item);
			});

			MenuItem evaluateItem = new MenuItem(i18n.translate("common.evaluateFormula"));
			evaluateItem.setOnAction(event -> this.evaluate());

			MenuItem dischargeItem = new MenuItem(i18n.translate("common.formula.discharge"));
			dischargeItem.setOnAction(event -> {
				TraceStatisticsFormulasItem item = row.getItem();
				if (item == null) {
					return;
				}
				item.setStatus(CheckingStatus.SUCCESS);
			});

			MenuItem failItem = new MenuItem(this.i18n.translate("common.formula.fail"));
			failItem.setOnAction(event -> {
				TraceStatisticsFormulasItem item = row.getItem();
				if (item == null) {
					return;
				}
				item.setStatus(CheckingStatus.FAIL);
			});

			MenuItem unknownItem = new MenuItem(this.i18n.translate("common.formula.unknown"));
			unknownItem.setOnAction(event -> {
				TraceStatisticsFormulasItem item = row.getItem();
				if (item == null) {
					return;
				}
				item.setStatus(CheckingStatus.NOT_CHECKED);
			});

			Menu statusMenu = new Menu(this.i18n.translate("common.formula.setStatus"), null, dischargeItem, failItem, unknownItem);

			MenuItem removeItem = new MenuItem(i18n.translate("sharedviews.checking.contextMenu.remove"));
			removeItem.setOnAction(event -> {
				TraceStatisticsFormulasItem item = row.getItem();
				if (item == null) {
					return;
				}
				this.currentProject.getCurrentMachine().removeValidationTask(item);
			});

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(evaluateItem, editFormula, statusMenu, removeItem)));
			return row;
		});
	}

	public void refresh() {
		this.statisticsTableView.refresh();
	}

	private void clear() {
		this.statisticsTableView.getItems().clear();
		cbOperation.getItems().clear();
	}

	@FXML
	private void exportCSV() throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("animation.tracereplay.statistics.save.title"));
		fileChooser.setInitialFileName("TraceStatistics.csv");
		fileChooser.getExtensionFilters().add(fileChooserManager.getCsvFilter());
		Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.TRACE_STATISTICS, stageManager.getCurrent());
		if (path != null) {
			CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
					.setHeader(i18n.translate("animation.tracereplay.statistics.table.columns.trace"), i18n.translate("animation.tracereplay.statistics.table.columns.totalComputation"), i18n.translate("animation.tracereplay.statistics.table.columns.desiredEffect"), i18n.translate("animation.tracereplay.statistics.table.columns.percentage"))
					.build();
			try (CSVPrinter csvPrinter = csvFormat.print(path, StandardCharsets.UTF_8)) {
				for (TraceStatisticsItem item : statisticsTableView.getItems()) {
					csvPrinter.printRecord(item.getName(), item.getTotalComputations(), item.getEffectObserved(), item.getPercentage());
				}
			}
		}
	}

	@FXML
	private void evaluate() {
		statisticsTableView.getItems().clear();
		String selectedOperation = cbOperation.getSelectionModel().getSelectedItem();
		String computations = tfComputations.getText();
		String desiredEffects = tfDesiredEffects.getText();
		Machine machine = currentProject.getCurrentMachine();
		ObservableList<ReplayTrace> traces = machine.getTraces();

		List<Number> listNumberComputations = new ArrayList();
		List<Number> listNumberDesiredEffects = new ArrayList();
		List<Number> listPercentages = new ArrayList();

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
								showErrorAlertInStatistics(e);
								throw new IllegalArgumentException("Could not evaluate formula for trace statistics: Not a valid integer", e);
							}
							numberDesiredEffects++;
						} else {
							try {
								numberComputations += Integer.parseInt(computationsResult);
								numberDesiredEffects += Integer.parseInt(desiredEffectsResult);
							} catch (NumberFormatException e) {
								showErrorAlertInStatistics(e);
								throw new IllegalArgumentException("Could not evaluate formula for trace statistics: Not a valid integer", e);
							}
						}
					}
				}
				double percentage = computePercentage(numberDesiredEffects, numberComputations);
				listNumberComputations.add(numberComputations);
				listNumberDesiredEffects.add(numberDesiredEffects);
				listPercentages.add(percentage);
				statisticsTableView.getItems().add(new TraceStatisticsItem(replayTrace.getName(), String.valueOf(numberComputations), String.valueOf(numberDesiredEffects), String.valueOf(percentage)));
			}
		}

		if(!statisticsTableView.getItems().isEmpty()) {
			double totalComputationsMean = computeMeanFromList(listNumberComputations);
			double effectsObservedMean = computeMeanFromList(listNumberDesiredEffects);
			double percentagesMean = computeMeanFromList(listPercentages);

			double totalComputationsStd = computeStdFromList(listNumberComputations, totalComputationsMean);
			double effectsObservedStd = computeStdFromList(listNumberDesiredEffects, effectsObservedMean);
			double percentagesStd = computeStdFromList(listPercentages, percentagesMean);

			statisticsTableView.getItems().add(new TraceStatisticsItem(i18n.translate("animation.tracereplay.statistics.table.special.mean"), convertMeanWithStd(totalComputationsMean, totalComputationsStd), convertMeanWithStd(effectsObservedMean, effectsObservedStd), convertMeanWithStd(percentagesMean, percentagesStd)));

			double totalComputationsSum = computeSumFromList(listNumberComputations);
			double effectsObservedSum = computeSumFromList(listNumberDesiredEffects);
			statisticsTableView.getItems().add(new TraceStatisticsItem(i18n.translate("animation.tracereplay.statistics.table.special.sum"), convertIntToStringForTable(totalComputationsSum), convertIntToStringForTable(effectsObservedSum), convertIntToStringForTable(computePercentage(effectsObservedSum, totalComputationsSum))));
		}
	}

	private double computeMeanFromList(List<Number> list) {
		return list.stream().mapToDouble(val -> val.doubleValue()).average().orElse(0.0f);
	}

	private double computeStdFromList(List<Number> list, double mean) {
		return Math.sqrt(list.stream().mapToDouble(val -> Math.pow(val.doubleValue() - mean, 2)).average().orElse(0.0f));
	}

	private double computeSumFromList(List<Number> list) {
		return list.stream().mapToDouble(val -> val.doubleValue()).sum();
	}

	private double computePercentage(double d1, double d2) {
		double result = 100.0f * d1 / d2;
		if(!Double.isFinite(result)) {
			return 0.0;
		}
		return result;
	}

	private String convertIntToStringForTable(int number) {
		return String.format("%.2f", number);
	}

	private String convertIntToStringForTable(double number) {
		return String.format("%.2f", number);
	}

	private String convertMeanWithStd(double mean, double std) {
		return String.format("%.2f +- %.2f", mean, std);
	}

	private void showErrorAlertInStatistics(NumberFormatException e) {
		final Alert alert = stageManager.makeExceptionAlert(e, "animation.tracereplay.statistics.alert.header",
				"animation.tracereplay.statistics.alert.content");
		alert.initOwner(this);
		alert.show();
	}

	@FXML
	private void add() {
		Machine machine = currentProject.getCurrentMachine();
		String operation = cbOperation.getSelectionModel().getSelectedItem();
		if(operation == null) {
			return;
		}
		String computations = tfComputations.getText();
		String desiredEffects = tfDesiredEffects.getText();
		TraceStatisticsFormulasItem item = new TraceStatisticsFormulasItem(null, operation, computations, desiredEffects);
		machine.addValidationTaskIfNotExist(item);
	}

	@FXML
	private void edit() {
		TraceStatisticsFormulasItem oldItem = formulaTableView.getSelectionModel().getSelectedItem();
		if(oldItem == null) {
			return;
		}
		String operation = cbOperation.getSelectionModel().getSelectedItem();
		String computations = tfComputations.getText();
		String desiredEffects = tfDesiredEffects.getText();
		TraceStatisticsFormulasItem newItem = new TraceStatisticsFormulasItem(oldItem.getId(), operation, computations, desiredEffects);
		this.currentProject.getCurrentMachine().replaceValidationTaskIfNotExist(oldItem, newItem);
	}

	private void editID(TraceStatisticsFormulasItem item) {
		final TextInputDialog dialog = new TextInputDialog(item.getId() == null ? "" : item.getId());
		stageManager.register(dialog);
		dialog.setTitle(i18n.translate("animation.tracereplay.view.contextMenu.editId"));
		dialog.setHeaderText(i18n.translate("vomanager.validationTaskId"));
		dialog.getEditor().setPromptText(i18n.translate("common.optionalPlaceholder"));
		dialog.showAndWait().map(idText -> {
			final String id = idText.trim().isEmpty() ? null : idText;
			TraceStatisticsFormulasItem newItem = new TraceStatisticsFormulasItem(id, item.getOperation(), item.getComputations(), item.getDesiredEffects());
			this.currentProject.getCurrentMachine().replaceValidationTaskIfNotExist(item, newItem);
			return null;
		});
	}


}
