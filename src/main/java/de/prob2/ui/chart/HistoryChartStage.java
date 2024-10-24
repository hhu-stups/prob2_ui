package de.prob2.ui.chart;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.statespace.Trace;
import de.prob.statespace.TraceElement;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.CheckingStatusCell;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class HistoryChartStage extends Stage {

	private static final Logger LOGGER = LoggerFactory.getLogger(HistoryChartStage.class);

	@FXML
	private ScrollPane chartsScrollPane;
	@FXML
	private FlowPane chartsPane;
	@FXML
	private LineChart<Number, Number> singleChart;
	@FXML
	private TableView<ChartFormulaTask> tvFormula;
	@FXML
	private TableColumn<ChartFormulaTask, CheckingStatus> statusColumn;
	@FXML
	private TableColumn<ChartFormulaTask, String> idColumn;
	@FXML
	private TableColumn<ChartFormulaTask, String> formulaColumn;
	@FXML
	private Button addButton;
	@FXML
	private Button removeButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private CheckBox separateChartsCheckBox;
	@FXML
	private CheckBox rectangularLineChartCheckBox;
	@FXML
	private Spinner<Integer> startSpinner;

	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final FileChooserManager fileChooserManager;
	private final I18n i18n;
	private final Provider<EditChartFormulaStage> editChartFormulaStageProvider;

	private final ObservableList<LineChart<Number, Number>> separateCharts;
	/**
	 * This field exists to stop the bindings on it from being garbage collected.
	 * And to have an identity reference to the observable - needed for unbinding!
	 */
	private ObservableList<ChartFormulaTask> currentFormulaTasks;

	@Inject
	private HistoryChartStage(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject,
	                          final FileChooserManager fileChooserManager, final I18n i18n,
	                          final Provider<EditChartFormulaStage> editChartFormulaStageProvider) {
		super();

		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.fileChooserManager = fileChooserManager;
		this.i18n = i18n;
		this.editChartFormulaStageProvider = editChartFormulaStageProvider;

		this.separateCharts = FXCollections.observableArrayList();

		stageManager.loadFXML(this, "history_chart_stage.fxml", this.getClass().getName());
	}

	@FXML
	private void initialize() {
		helpButton.setHelpContent("mainmenu.visualisations.historyChart", null);

		this.tvFormula.getItems().addListener(this::onFormulaListChange);
		this.statusColumn.setCellFactory(col -> new CheckingStatusCell<>());
		this.statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		this.idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		this.formulaColumn.setCellValueFactory(new PropertyValueFactory<>("formula"));
		this.tvFormula.setRowFactory(tv -> {
			TableRow<ChartFormulaTask> row = new TableRow<>();

			// == edit ==
			MenuItem editItem = new MenuItem(i18n.translate("common.editFormula"));
			editItem.setOnAction(event -> this.editFormulaWithDialog(row.getItem()));
			// ============

			// == change status ==
			MenuItem dischargeItem = new MenuItem(i18n.translate("common.formula.discharge"));
			dischargeItem.setOnAction(event -> {
				ChartFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				item.setStatus(CheckingStatus.SUCCESS);
			});
			MenuItem failItem = new MenuItem(this.i18n.translate("common.formula.fail"));
			failItem.setOnAction(event -> {
				ChartFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				item.setStatus(CheckingStatus.FAIL);
			});
			MenuItem unknownItem = new MenuItem(this.i18n.translate("common.formula.unknown"));
			unknownItem.setOnAction(event -> {
				ChartFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				item.setStatus(CheckingStatus.NOT_CHECKED);
			});
			Menu statusMenu = new Menu(this.i18n.translate("dynamic.setStatus"), null, dischargeItem, failItem, unknownItem);
			// ==============

			// == remove ==
			MenuItem removeItem = new MenuItem(i18n.translate("sharedviews.checking.contextMenu.remove"));
			removeItem.setOnAction(event -> {
				ChartFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				this.currentProject.getCurrentMachine().removeValidationTask(item);
			});
			// =====================

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(editItem, statusMenu, removeItem)));
			return row;
		});

		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> this.loadFormulas(to));
		this.loadFormulas(this.currentProject.getCurrentMachine());

		this.removeButton.disableProperty()
				.bind(Bindings.isNull(this.tvFormula.getSelectionModel().selectedItemProperty()));

		this.separateChartsCheckBox.selectedProperty().addListener((observable, from, to) -> {
			this.chartsPane.getChildren().clear();
			if (to) {
				this.chartsPane.getChildren().addAll(this.separateCharts);
			} else {
				this.chartsPane.getChildren().addAll(this.singleChart);
			}
		});
		this.separateChartsCheckBox.setSelected(true);

		this.rectangularLineChartCheckBox.selectedProperty().addListener((observable, from, to) -> updateCharts());
		this.rectangularLineChartCheckBox.setSelected(true);

		this.singleChart.prefWidthProperty().bind(this.chartsScrollPane.widthProperty().subtract(5));
		this.singleChart.prefHeightProperty().bind(this.chartsScrollPane.heightProperty().subtract(5));

		this.showingProperty().addListener((observable, from, to) -> {
			if (to) {
				this.updateCharts();
			}
		});
		this.currentTrace.addListener((observable, from, to) -> {
			if (to == null) {
				return;
			}
			SpinnerValueFactory<Integer> currentSpinnerFactory = startSpinner.getValueFactory();
			startSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, to.size(), currentSpinnerFactory.getValue() == null ? 0 : currentSpinnerFactory.getValue()));
			this.updateCharts();
		});
		startSpinner.valueProperty().addListener((observable, from, to) -> {
			// Workaround for a NPE in JavaFX
			if (to == null) {
				startSpinner.getValueFactory().setValue(0);
				return;
			}
			this.updateCharts();
		});
		this.updateCharts();
		addChartMenu(singleChart);
	}

	private void loadFormulas(Machine machine) {
		if (this.currentFormulaTasks != null) {
			Bindings.unbindContent(this.tvFormula.getItems(), this.currentFormulaTasks);
			this.currentFormulaTasks = null;
		}
		if (machine != null) {
			this.currentFormulaTasks = machine.getChartFormulaTasks();
			Bindings.bindContent(this.tvFormula.getItems(), this.currentFormulaTasks);
		}
	}

	private void onFormulaListChange(ListChangeListener.Change<? extends ChartFormulaTask> change) {
		while (change.next()) {
			if (change.wasRemoved()) {
				this.removeCharts(change.getFrom(), change.getFrom() + change.getRemovedSize());
			}
			if (change.wasAdded()) {
				this.addCharts(change.getFrom(), change.getTo(), change.getList());
			}
		}
		this.updateCharts();
	}

	private void addChartMenu(LineChart<Number, Number> chart) {
		final MenuItem saveImageItem = new MenuItem(i18n.translate("chart.historyChart.menus.item.saveAsImage"));
		saveImageItem.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(i18n.translate("chart.historyChart.fileChooser.saveAsImage"));
			fileChooser.getExtensionFilters().add(fileChooserManager.getPngFilter());
			Path path = fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.HISTORY_CHART, stageManager.getCurrent());
			if (path == null) {
				return;
			}
			WritableImage image = chart.snapshot(new SnapshotParameters(), null);
			try {
				ImageIO.write(SwingFXUtils.fromFXImage(image, null), "PNG", path.toFile());
			} catch (IOException ex) {
				LOGGER.error("Saving as PNG failed", ex);
				final Alert alert = stageManager.makeExceptionAlert(ex, "common.alerts.couldNotSaveFile.content", path);
				alert.initOwner(this);
				alert.showAndWait();
			}
		});

		final MenuItem saveCSVItem = new MenuItem(i18n.translate("chart.historyChart.menus.item.saveAsCSV"));
		saveCSVItem.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(i18n.translate("chart.historyChart.fileChooser.saveAsCSV"));
			fileChooser.getExtensionFilters().add(fileChooserManager.getCsvFilter());
			Path path = fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.HISTORY_CHART, stageManager.getCurrent());
			if (path == null) {
				return;
			}

			try (CSVPrinter csvPrinter = CSVFormat.DEFAULT.print(path, StandardCharsets.UTF_8)) {
				for (XYChart.Series<Number, Number> series : chart.getData()) {
					for (XYChart.Data<Number, Number> entry : series.getData()) {
						csvPrinter.printRecord(entry.getXValue(), entry.getYValue());
					}
				}
			} catch (IOException ex) {
				LOGGER.error("Saving as CSV failed", ex);
				final Alert alert = stageManager.makeExceptionAlert(ex, "common.alerts.couldNotSaveFile.content", path);
				alert.initOwner(this);
				alert.showAndWait();
			}
		});

		final ContextMenu menu = new ContextMenu(saveImageItem, saveCSVItem);

		chart.setOnMouseClicked(e -> {
			if (MouseButton.SECONDARY.equals(e.getButton())) {
				menu.show(chart.getScene().getWindow(), e.getScreenX(), e.getScreenY());
			}
		});
	}

	private void editFormulaWithDialog(ChartFormulaTask oldTask) {
		Machine machine = this.currentProject.getCurrentMachine();
		if (machine == null) {
			return;
		}

		EditChartFormulaStage stage = this.editChartFormulaStageProvider.get();
		stage.initOwner(this);
		if (oldTask != null) {
			stage.setInitialFormulaTask(oldTask);
		}
		stage.showAndWait();

		ChartFormulaTask newTask = stage.getResult();
		if (newTask != null) {
			Machine newMachine = this.currentProject.getCurrentMachine();
			if (newMachine == machine) {
				if (oldTask != null) {
					newMachine.replaceValidationTaskIfNotExist(oldTask, newTask);
				} else {
					newMachine.addValidationTaskIfNotExist(newTask);
				}
			} else {
				LOGGER.warn("The machine has changed, discarding task changes");
			}
		}
	}

	@FXML
	private void handleAdd() {
		this.editFormulaWithDialog(null);
	}

	@FXML
	private void handleRemove() {
		Machine machine = this.currentProject.getCurrentMachine();
		if (machine != null) {
			ChartFormulaTask item = this.tvFormula.getSelectionModel().getSelectedItem();
			if (item != null) {
				machine.removeValidationTask(item);
			}
		}
	}

	private static void updateXAxisTicks(NumberAxis axis, double upperBound) {
		// If the range of values is small enough (50 or less),
		// JavaFX often adds non-integer tick marks to the axis,
		// even when all values are actually integers.
		// We always want integer tick marks,
		// so we disable the automatic tick placing and force a tick unit of 1.
		// However, if this is done with a too large range of values (2000 or more),
		// JavaFX complains on stderr about too many tick marks,
		// so we enable auto-ranging again when there are enough values.
		if (upperBound <= 50.0) {
			axis.setAutoRanging(false);
			axis.setTickUnit(1.0);
			axis.setUpperBound(upperBound);
		} else {
			axis.setAutoRanging(true);
		}
	}

	private void removeCharts(final int start, final int end) {
		this.singleChart.getData().remove(start, end);
		this.separateCharts.remove(start, end);
		if (this.separateChartsCheckBox.isSelected()) {
			this.chartsPane.getChildren().remove(start, end);
		}
	}

	private void addCharts(final int start, final int end, final List<? extends ChartFormulaTask> charts) {
		for (int i = start; i < end; i++) {
			final XYChart.Series<Number, Number> seriesSingle = new XYChart.Series<>(charts.get(i).getFormula(),
					FXCollections.observableArrayList());
			this.singleChart.getData().add(i, seriesSingle);

			final XYChart.Series<Number, Number> seriesSeparate = new XYChart.Series<>(charts.get(i).getFormula(),
					FXCollections.observableArrayList());
			final NumberAxis separateXAxis = new NumberAxis();
			separateXAxis.getStyleClass().add("time-axis");
			final NumberAxis separateYAxis = new NumberAxis();
			final LineChart<Number, Number> separateChart = new LineChart<>(separateXAxis, separateYAxis,
					FXCollections.singletonObservableList(seriesSeparate));
			separateChart.getStyleClass().add("history-chart");
			addChartMenu(separateChart);

			seriesSingle.getData().addListener((ListChangeListener<XYChart.Data<Number, Number>>) change -> {
				// Update the separate chart series whenever the single chart
				// series is updated.
				while (change.next()) {
					if (change.wasRemoved()) {
						seriesSeparate.getData().remove(change.getFrom(), change.getFrom() + change.getRemovedSize());
					}

					if (change.wasAdded()) {
						seriesSeparate.getData().addAll(change.getFrom(), change.getAddedSubList());
					}
				}

				// Update the upper bound of the X axis of the separate chart
				updateXAxisTicks(separateXAxis, change.getList().isEmpty() ? 1.0
					: change.getList().get(change.getList().size() - 1).getXValue().doubleValue());
			});

			separateChart.setMinWidth(160);
			separateChart.setMinHeight(80);
			separateChart.setMaxWidth(Double.POSITIVE_INFINITY);
			separateChart.setMaxHeight(Double.POSITIVE_INFINITY);

			// Adjust the sizes of all separate charts so they always fill the
			// entire flow pane, and are as close as possible to 320px * 240px.
			// We subtract 1.0 from the resulting width/height, to ensure that
			// the sum is not larger than the flow pane's width/height.
			// Otherwise the charts jump around as the flow pane tries to make
			// them fit.
			separateChart.prefWidthProperty()
					.bind(Bindings.createDoubleBinding(
							() -> (chartsPane.getWidth() / Math.round(chartsPane.getWidth() / 320.0)) - 1.0,
							chartsPane.widthProperty()));
			separateChart.prefHeightProperty()
					.bind(Bindings.createDoubleBinding(
							() -> (chartsPane.getHeight() / Math.round(chartsPane.getHeight() / 240.0)) - 1.0,
							chartsPane.heightProperty()));

			this.separateCharts.add(i, separateChart);
			if (this.separateChartsCheckBox.isSelected()) {
				this.chartsPane.getChildren().add(i, separateChart);
			}
		}
	}

	private void updateCharts() {
		if (!this.isShowing()) {
			return;
		}

		final List<List<XYChart.Data<Number, Number>>> newDatas = new ArrayList<>();
		for (int i = 0; i < this.singleChart.getData().size(); i++) {
			newDatas.add(new ArrayList<>());
		}

		int elementCounter = 0;
		final Trace trace = this.currentTrace.get();
		if (trace != null) {

			TraceElement element = trace.getCurrent();
			boolean showErrors = true;
			int value = startSpinner.getValue() == null ? 0 : startSpinner.getValue();
			while (element != null && element.getIndex() >= value) {
				tryEvalFormulas(newDatas, elementCounter, element, showErrors);
				element = element.getPrevious();
				elementCounter++;
				// Only display errors to the user for the current state (otherwise errors are repeated for every state)
				showErrors = false;
			}
		}

		for (int i = 0; i < newDatas.size(); i++) {
			final List<XYChart.Data<Number, Number>> newData = newDatas.get(i);
			moveXValues(elementCounter, newData);
			this.singleChart.getData().get(i).getData().clear();
			this.singleChart.getData().get(i).getData().addAll(newData);
		}
		updateMaxXBound(newDatas);
	}

	private void tryEvalFormulas(final List<List<XYChart.Data<Number, Number>>> newDatas, final int xPos, final TraceElement element, final boolean showErrors) {
		// TODO: cache this
		var formulas = this.tvFormula.getItems().stream()
				                            .map(ChartFormulaTask::getFormula)
				                            .map(this.currentTrace.getModel()::parseFormula)
				                            .toList();
		List<AbstractEvalResult> results = element.getCurrentState().eval(formulas);
		for (int i = 0; i < results.size(); i++) {
			final AbstractEvalResult result = results.get(i);
			if (result instanceof IdentifierNotInitialised) {
				continue;
			}
			final Number value;
			try {
				value = resultToNumber(result, showErrors);
			} catch (IllegalArgumentException e) {
				LOGGER.debug("Not convertible to int, ignoring", e);
				continue;
			}
			// Add additional data point for rectangular shapes in line chart
			if (rectangularLineChartCheckBox.isSelected()) {
				newDatas.get(i).add(0, new XYChart.Data<>(xPos - 1, value));
			}
			newDatas.get(i).add(0, new XYChart.Data<>(xPos, value));
		}
	}

	private static void moveXValues(final int offset, final List<XYChart.Data<Number, Number>> newData) {
		for (final XYChart.Data<Number, Number> newDatum : newData) {
			newDatum.setXValue(offset - newDatum.getXValue().intValue() - 1);
		}
	}

	private void updateMaxXBound(final List<List<XYChart.Data<Number, Number>>> newDatas) {
		double maxXBound = 1.0;
		for (final List<XYChart.Data<Number, Number>> newData : newDatas) {
			if (!newData.isEmpty()) {
				final double lastX = newData.get(newData.size() - 1).getXValue().doubleValue();
				if (lastX > maxXBound) {
					maxXBound = lastX;
				}
			}
		}
		updateXAxisTicks((NumberAxis) this.singleChart.getXAxis(), maxXBound);
	}

	private Number resultToNumber(final AbstractEvalResult aer, final boolean showErrors) {
		if (aer instanceof EvalResult) {
			final String value = ((EvalResult) aer).getValue();
			if ("TRUE".equals(value)) {
				return 1;
			} else if ("FALSE".equals(value)) {
				return 0;
			} else {
				try {
					return Double.parseDouble(value);
				} catch (NumberFormatException e) {
					if (showErrors) {
						final Alert alert = stageManager.makeExceptionAlert(e, "chart.historyChart.alerts.formulaEvalError.header",
								"chart.historyChart.alerts.formulaEvalError.invalidInteger.content");
						alert.initOwner(this);
						alert.show();
					}
					throw new IllegalArgumentException("Could not evaluate formula for history chart: Not a valid integer", e);
				}
			}
		} else {
			if (showErrors) {
				final Alert alert = stageManager.makeAlert(Alert.AlertType.ERROR,
						"chart.historyChart.alerts.formulaEvalError.header",
						"chart.historyChart.alerts.formulaEvalError.notAnEvalResult.content", aer);
				alert.initOwner(this);
				alert.show();
			}
			throw new IllegalArgumentException(
					"Could not evaluate formula for history chart: Expected an EvalResult, not "
							+ aer.getClass().getName());
		}
	}
}
