package de.prob2.ui.chart;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.TraceElement;

import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class HistoryChartStage extends Stage {
	private static final class ClassicalBListCell extends ListCell<ClassicalB> {
		private ClassicalBListCell() {
			super();
		}
		
		@Override
		protected void updateItem(final ClassicalB item, final boolean empty) {
			super.updateItem(item, empty);
			
			this.setText(item == null || empty ? null : item.getCode());
			this.setGraphic(null);
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
					formula = new ClassicalB(textField.getText());
				} catch (EvaluationException e) {
					LOGGER.debug("Could not parse user-entered formula", e);
					textField.getStyleClass().add("text-field-error");
					return;
				}
				this.commitEdit(formula);
			});
			textField.textProperty().addListener(observable -> textField.getStyleClass().remove("text-field-error"));
			
			this.setText(null);
			this.setGraphic(textField);
		}
	}
	
	private static class TraceElementStringConverter extends StringConverter<TraceElement> {
		@Override
		public String toString(final TraceElement object) {
			return object == DUMMY_TRACE_ELEMENT ? "(no model loaded)" : HistoryView.transitionToString(object.getTransition());
		}
		
		@Override
		public TraceElement fromString(final String string) {
			throw new UnsupportedOperationException("Cannot convert from string to TraceElement");
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HistoryChartStage.class);
	private static final TraceElement DUMMY_TRACE_ELEMENT = new TraceElement(new State("Dummy state", null));
	
	@FXML private FlowPane chartsPane;
	@FXML private LineChart<Number, Number> singleChart;
	@FXML private ListView<ClassicalB> formulaList;
	@FXML private Button addButton;
	@FXML private Button removeButton;
	@FXML private CheckBox separateChartsCheckBox;
	@FXML private ChoiceBox<TraceElement> startChoiceBox;
	
	private final CurrentTrace currentTrace;
	
	private final ObservableList<LineChart<Number, Number>> separateCharts;
	
	@Inject
	private HistoryChartStage(final StageManager stageManager, final CurrentTrace currentTrace) {
		super();
		
		this.currentTrace = currentTrace;
		this.separateCharts = FXCollections.observableArrayList();
		
		stageManager.loadFXML(this, "history_chart_stage.fxml", this.getClass().getName());
	}
	
	@FXML
	private void initialize() {
		this.formulaList.setCellFactory(view -> new ClassicalBListCell());
		this.formulaList.getItems().addListener((ListChangeListener<ClassicalB>)change -> {
			while (change.next()) {
				if (change.wasRemoved()) {
					this.removeCharts(change.getFrom(), change.getFrom() + change.getRemovedSize());
				}
				
				if (change.wasAdded()) {
					this.addCharts(change.getFrom(), change.getTo(), change.getList());
				}
			}
			this.updateCharts();
		});
		
		this.removeButton.disableProperty().bind(Bindings.isEmpty(this.formulaList.getSelectionModel().getSelectedIndices()));
		
		this.separateChartsCheckBox.selectedProperty().addListener((observable, from, to) -> {
			if (to) {
				this.chartsPane.getChildren().setAll(this.separateCharts);
			} else {
				this.chartsPane.getChildren().setAll(this.singleChart);
			}
		});
		this.separateChartsCheckBox.setSelected(true);
		
		this.startChoiceBox.setConverter(new HistoryChartStage.TraceElementStringConverter());
		this.startChoiceBox.valueProperty().addListener((observable, from, to) -> this.updateCharts());
		
		this.singleChart.prefWidthProperty().bind(this.chartsPane.widthProperty());
		this.singleChart.prefHeightProperty().bind(this.chartsPane.heightProperty());
		
		this.currentTrace.addListener((observable, from, to) -> this.updateStartChoiceBox());
		this.updateStartChoiceBox();
	}
	
	@FXML
	private void handleAdd() {
		this.formulaList.getItems().add(new ClassicalB("0"));
		this.formulaList.edit(this.formulaList.getItems().size()-1);
	}
	
	@FXML
	private void handleRemove() {
		this.formulaList.getItems().remove(this.formulaList.getSelectionModel().getSelectedIndex());
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
			final XYChart.Series<Number, Number> seriesSingle = new XYChart.Series<>(charts.get(i).getCode(), FXCollections.observableArrayList());
			this.singleChart.getData().add(i, seriesSingle);
			
			final XYChart.Series<Number, Number> seriesSeparate = new XYChart.Series<>(charts.get(i).getCode(), FXCollections.observableArrayList());
			final NumberAxis separateXAxis = new NumberAxis();
			separateXAxis.getStyleClass().add("time-axis");
			separateXAxis.setAutoRanging(false);
			separateXAxis.setTickUnit(1.0);
			separateXAxis.setUpperBound(0.0);
			final NumberAxis separateYAxis = new NumberAxis();
			final LineChart<Number, Number> separateChart = new LineChart<>(separateXAxis, separateYAxis, FXCollections.singletonObservableList(seriesSeparate));
			separateChart.getStyleClass().add("history-chart");
			
			seriesSingle.getData().addListener((ListChangeListener<XYChart.Data<Number, Number>>)change -> {
				// Update the separate chart series whenever the single chart series is updated.
				while (change.next()) {
					if (change.wasRemoved()) {
						seriesSeparate.getData().remove(change.getFrom(), change.getFrom()+change.getRemovedSize());
					}
					
					if (change.wasAdded()) {
						seriesSeparate.getData().addAll(change.getFrom(), change.getAddedSubList());
					}
				}
				
				// Update the upper bound of the X axis of the separate chart
				separateXAxis.setUpperBound(change.getList().isEmpty() ? 1.0 : change.getList().get(change.getList().size()-1).getXValue().doubleValue());
			});
			
			separateChart.setMinWidth(160);
			separateChart.setMinHeight(80);
			separateChart.setMaxWidth(Double.POSITIVE_INFINITY);
			separateChart.setMaxHeight(Double.POSITIVE_INFINITY);
			
			// Adjust the sizes of all separate charts so they always fill the entire flow pane, and are as close as possible to 320px * 240px.
			// We subtract 1.0 from the resulting width/height, to ensure that the sum is not larger than the flow pane's width/height. Otherwise the charts jump around as the flow pane tries to make them fit.
			separateChart.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> (chartsPane.getWidth() / Math.round(chartsPane.getWidth() / 320.0)) - 1.0, chartsPane.widthProperty()));
			separateChart.prefHeightProperty().bind(Bindings.createDoubleBinding(() -> (chartsPane.getHeight() / Math.round(chartsPane.getHeight() / 240.0)) - 1.0, chartsPane.heightProperty()));
			
			this.separateCharts.add(i, separateChart);
			if (this.separateChartsCheckBox.isSelected()) {
				this.chartsPane.getChildren().add(i, separateChart);
			}
		}
	}
	
	private void updateStartChoiceBox() {
		if (this.currentTrace.exists()) {
			final TraceElement startElement = this.startChoiceBox.getValue();
			this.startChoiceBox.getItems().clear();
			
			TraceElement element = currentTrace.get().getCurrent();
			TraceElement prevElement = element;
			while (element != null) {
				this.startChoiceBox.getItems().add(element);
				prevElement = element;
				element = element.getPrevious();
			}
			
			this.startChoiceBox.setValue(startElement == null || startElement == DUMMY_TRACE_ELEMENT ? prevElement : startElement);
		} else {
			this.startChoiceBox.getItems().setAll(DUMMY_TRACE_ELEMENT);
			this.startChoiceBox.setValue(DUMMY_TRACE_ELEMENT);
		}
		
		this.updateCharts();
	}
	
	private void updateCharts() {
		final List<List<XYChart.Data<Number, Number>>> newDatas = new ArrayList<>();
		for (int i = 0; i < this.singleChart.getData().size(); i++) {
			newDatas.add(new ArrayList<>());
		}
		
		int elementCounter = 0;
		if (this.currentTrace.exists()) {
			final StateSpace stateSpace = this.currentTrace.getStateSpace();
			
			// Workaround for StateSpace.eval only taking exactly a List<IEvalElement>, and not a List<ClassicalB>
			final List<IEvalElement> formulas = new ArrayList<>(this.formulaList.getItems());
			final TraceElement startElement = this.startChoiceBox.getValue();
			
			TraceElement element = this.currentTrace.get().getCurrent();
			TraceElement prevElement = element;
			while (element != null && prevElement != startElement) {
				final List<AbstractEvalResult> results = stateSpace.eval(element.getCurrentState(), formulas);
				
				for (int i = 0; i < results.size(); i++) {
					final AbstractEvalResult result = results.get(i);
					if (result instanceof IdentifierNotInitialised) {
						continue;
					}
					final int value;
					try {
						value = resultToInt(result);
					} catch (IllegalArgumentException e) {
						LOGGER.debug("Not convertible to int, ignoring", e);
						continue;
					}
					newDatas.get(i).add(0, new XYChart.Data<>(elementCounter, value));
				}
				
				prevElement = element;
				element = element.getPrevious();
				elementCounter++;
			}
		}
		
		double maxXBound = 1.0;
		for (int i = 0; i < newDatas.size(); i++) {
			final List<XYChart.Data<Number, Number>> newData = newDatas.get(i);
			
			for (XYChart.Data<Number, Number> newDatum : newData) {
				newDatum.setXValue(elementCounter - newDatum.getXValue().intValue() - 1);
			}
			
			if (!newData.isEmpty()) {
				final double lastX = newData.get(newData.size()-1).getXValue().doubleValue();
				if (lastX > maxXBound) {
					maxXBound = lastX;
				}
			}
			
			this.singleChart.getData().get(i).getData().setAll(newData);
		}
		((NumberAxis)this.singleChart.getXAxis()).setUpperBound(maxXBound);
	}
	
	private static int resultToInt(final AbstractEvalResult aer) {
		if (aer instanceof EvalResult) {
			final String value = ((EvalResult)aer).getValue();
			if ("TRUE".equals(value)) {
				return 1;
			} else if ("FALSE".equals(value)) {
				return 0;
			} else {
				try {
					return Integer.parseInt(value);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Not a valid integer", e);
				}
			}
		} else {
			throw new IllegalArgumentException("Expected an EvalResult, not " + aer.getClass().getName());
		}
	}
}
