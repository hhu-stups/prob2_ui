package de.prob2.ui.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.statespace.Trace;
import de.prob.statespace.TraceElement;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.history.HistoryItem;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class HistoryChartStage extends Stage {
	private static final class ClassicalBListCell extends ListCell<ClassicalB> {
		private final StringProperty code;

		private ClassicalBListCell() {
			super();

			this.code = new SimpleStringProperty(this, "code", null);
			this.textProperty().bind(this.code);
		}

		@Override
		protected void updateItem(final ClassicalB item, final boolean empty) {
			super.updateItem(item, empty);

			this.code.set(item == null || empty ? null : item.getCode());
		}

		@Override
		public void startEdit() {
			super.startEdit();

			if (!this.isEditing()) {
				return;
			}

			final TextField textField = new TextField(this.getText());
			textField.setOnAction(event -> {
				textField.getStyleClass().remove("text-field-error");
				final ClassicalB formula;
				try {
					formula = new ClassicalB(textField.getText(), FormulaExpand.EXPAND);
				} catch (EvaluationException e) {
					LOGGER.debug("Could not parse user-entered formula", e);
					textField.getStyleClass().add("text-field-error");
					return;
				}
				this.commitEdit(formula);
			});
			textField.setOnKeyPressed(event -> {
				if (KeyCode.ESCAPE.equals(event.getCode())) {
					this.cancelEdit();
				}
			});
			textField.textProperty().addListener(observable -> textField.getStyleClass().remove("text-field-error"));

			this.textProperty().unbind();
			this.setText(null);
			this.setGraphic(textField);
			textField.requestFocus();
		}

		@Override
		public void commitEdit(final ClassicalB newValue) {
			super.commitEdit(newValue);
			this.textProperty().bind(this.code);
			this.setGraphic(null);
		}

		@Override
		public void cancelEdit() {
			super.cancelEdit();
			this.textProperty().bind(this.code);
			this.setGraphic(null);
		}
	}

	private class HistoryItemStringConverter extends StringConverter<HistoryItem> {
		@Override
		public String toString(final HistoryItem object) {
			return object == null ? bundle.getString("common.noModelLoaded") : object.toPrettyString();
		}

		@Override
		public HistoryItem fromString(final String string) {
			throw new UnsupportedOperationException("Cannot convert from string to HistoryItem");
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(HistoryChartStage.class);

	@FXML
	private ScrollPane chartsScrollPane;
	@FXML
	private FlowPane chartsPane;
	@FXML
	private LineChart<Number, Number> singleChart;
	@FXML
	private ListView<ClassicalB> formulaList;
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
	private ChoiceBox<HistoryItem> startChoiceBox;

	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final ResourceBundle bundle;

	private final ObservableList<LineChart<Number, Number>> separateCharts;

	@Inject
	private HistoryChartStage(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject, final ResourceBundle bundle) {
		super();

		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.bundle = bundle;

		this.separateCharts = FXCollections.observableArrayList();

		stageManager.loadFXML(this, "history_chart_stage.fxml", this.getClass().getName());
	}

	@FXML
	private void initialize() {
		helpButton.setHelpContent("historyChart", null);
		this.formulaList.setCellFactory(view -> new ClassicalBListCell());
		this.formulaList.getItems().addListener((ListChangeListener<ClassicalB>) change -> {
			while (change.next()) {
				if (change.wasRemoved()) {
					this.removeCharts(change.getFrom(), change.getFrom() + change.getRemovedSize());
				}

				if (change.wasAdded()) {
					this.addCharts(change.getFrom(), change.getTo(), change.getList());
				}
			}
			this.updateCharts();
			this.updateFormulaCodeList();
		});
		
		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if(from != to) {
				if (!to.getHistoryChartItems().isEmpty()) {
					to.getHistoryChartItems().forEach(s -> this.formulaList.getItems().add(new ClassicalB(s, FormulaExpand.EXPAND)));
				} else {
					this.formulaList.getItems().clear();
				}
			}
		});
		
		

		this.removeButton.disableProperty()
				.bind(Bindings.isEmpty(this.formulaList.getSelectionModel().getSelectedIndices()));

		this.separateChartsCheckBox.selectedProperty().addListener((observable, from, to) -> {
			if (to) {
				this.chartsPane.getChildren().setAll(this.separateCharts);
			} else {
				this.chartsPane.getChildren().setAll(this.singleChart);
			}
		});
		this.separateChartsCheckBox.setSelected(true);

		this.rectangularLineChartCheckBox.selectedProperty().addListener((observable, from, to) -> {
			updateCharts();
		});
		this.rectangularLineChartCheckBox.setSelected(true);

		this.startChoiceBox.setConverter(new HistoryItemStringConverter());
		this.startChoiceBox.valueProperty().addListener((observable, from, to) -> this.updateCharts());

		this.singleChart.prefWidthProperty().bind(this.chartsScrollPane.widthProperty().subtract(5));
		this.singleChart.prefHeightProperty().bind(this.chartsScrollPane.heightProperty().subtract(5));

		this.showingProperty().addListener((observable, from, to) -> {
			if (to) {
				this.updateStartChoiceBox();
			}
		});
		this.currentTrace.addListener((observable, from, to) -> this.updateStartChoiceBox());
		this.updateStartChoiceBox();
	}

	@FXML
	private void handleAdd() {
		this.formulaList.getItems().add(new ClassicalB("0", FormulaExpand.EXPAND));
		this.formulaList.edit(this.formulaList.getItems().size() - 1);
		updateFormulaCodeList();
	}

	@FXML
	private void handleRemove() {
		this.formulaList.getItems().remove(this.formulaList.getSelectionModel().getSelectedIndex());
		updateFormulaCodeList();
	}

	private void updateFormulaCodeList() {
		ArrayList<String> formulaCodeList = new ArrayList<>();
		this.formulaList.getItems().forEach(b -> formulaCodeList.add(b.getCode()));
		this.currentProject.currentMachineProperty().get().setHistoryChartItems(formulaCodeList);
	}

	private void removeCharts(final int start, final int end) {
		this.singleChart.getData().remove(start, end);
		this.separateCharts.remove(start, end);
		if (this.separateChartsCheckBox.isSelected()) {
			this.chartsPane.getChildren().remove(start, end);
		}
	}

	private void addCharts(final int start, final int end, final List<? extends ClassicalB> charts) {
		for (int i = start; i < end; i++) {
			final XYChart.Series<Number, Number> seriesSingle = new XYChart.Series<>(charts.get(i).getCode(),
					FXCollections.observableArrayList());
			this.singleChart.getData().add(i, seriesSingle);

			final XYChart.Series<Number, Number> seriesSeparate = new XYChart.Series<>(charts.get(i).getCode(),
					FXCollections.observableArrayList());
			final NumberAxis separateXAxis = new NumberAxis();
			separateXAxis.getStyleClass().add("time-axis");
			separateXAxis.setAutoRanging(false);
			separateXAxis.setTickUnit(1.0);
			separateXAxis.setUpperBound(0.0);
			final NumberAxis separateYAxis = new NumberAxis();
			final LineChart<Number, Number> separateChart = new LineChart<>(separateXAxis, separateYAxis,
					FXCollections.singletonObservableList(seriesSeparate));
			separateChart.getStyleClass().add("history-chart");

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
				separateXAxis.setUpperBound(change.getList().isEmpty() ? 1.0
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

	private void updateStartChoiceBox() {
		if (!this.isShowing()) {
			return;
		}
		
		final Trace trace = currentTrace.get();
		if (trace != null) {
			final HistoryItem startItem = this.startChoiceBox.getValue();
			final List<HistoryItem> items = HistoryItem.itemsForTrace(trace);
			this.startChoiceBox.getItems().setAll(items);
			this.startChoiceBox.setValue(startItem == null ? items.get(items.size()-1) : startItem);
		} else {
			this.startChoiceBox.getItems().setAll((HistoryItem)null);
			this.startChoiceBox.setValue(null);
		}

		this.updateCharts();
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
			final int startIndex = this.startChoiceBox.getValue().getIndex();

			TraceElement element = trace.getCurrent();
			boolean showErrors = true;
			while (element != null && element.getIndex() >= startIndex) {
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
			this.singleChart.getData().get(i).getData().setAll(newData);
		}
		updateMaxXBound(newDatas);
	}

	private void tryEvalFormulas(final List<List<XYChart.Data<Number, Number>>> newDatas, final int xPos, final TraceElement element, final boolean showErrors) {
		final List<AbstractEvalResult> results = this.currentTrace.getStateSpace().eval(element.getCurrentState(), this.formulaList.getItems());
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
				newDatas.get(i).add(0, new XYChart.Data<>(xPos + 1, value));
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
		((NumberAxis) this.singleChart.getXAxis()).setUpperBound(maxXBound);
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
