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
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.TraceElement;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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
				textField.getStyleClass().remove("badsearch");
				final ClassicalB formula;
				try {
					formula = new ClassicalB(textField.getText());
				} catch (EvaluationException e) {
					LOGGER.debug("Could not parse user-entered formula", e);
					textField.getStyleClass().add("badsearch");
					return;
				}
				this.commitEdit(formula);
			});
			textField.textProperty().addListener(observable -> textField.getStyleClass().remove("badsearch"));
			
			this.setText(null);
			this.setGraphic(textField);
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HistoryChartStage.class);
	
	@FXML private LineChart<Number, Number> chart;
	@FXML private ListView<ClassicalB> formulaList;
	@FXML private Button addButton;
	@FXML private Button removeButton;
	
	private final CurrentTrace currentTrace;
	
	@Inject
	private HistoryChartStage(final StageManager stageManager, final CurrentTrace currentTrace) {
		super();
		
		this.currentTrace = currentTrace;
		
		stageManager.loadFXML(this, "history_chart_stage.fxml", this.getClass().getName());
	}
	
	@FXML
	private void initialize() {
		this.formulaList.setCellFactory(view -> new ClassicalBListCell());
		this.formulaList.getItems().addListener((ListChangeListener<ClassicalB>)change -> {
			while (change.next()) {
				if (change.wasRemoved()) {
					LOGGER.debug("Removed {} ({} to {})", change.getRemoved(), change.getFrom(), change.getFrom()+change.getRemovedSize());
					this.chart.getData().remove(change.getFrom(), change.getFrom()+change.getRemovedSize());
				}
				if (change.wasAdded()) {
					for (int i = change.getFrom(); i < change.getTo(); i++) {
						LOGGER.debug("Added {} at {}", change.getList().get(i), i);
						this.chart.getData().add(i, new XYChart.Series<>(change.getList().get(i).getCode(), FXCollections.observableArrayList()));
					}
				}
			}
			this.update(this.currentTrace.get());
		});
		
		this.removeButton.disableProperty().bind(Bindings.isEmpty(this.formulaList.getSelectionModel().getSelectedIndices()));
		
		this.currentTrace.addListener((observable, from, to) -> this.update(to));
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
	
	private void update(final Trace trace) {
		final List<List<XYChart.Data<Number, Number>>> newDatas = new ArrayList<>();
		for (int i = 0; i < this.chart.getData().size(); i++) {
			newDatas.add(new ArrayList<>());
		}
		
		if (trace != null) {
			final StateSpace stateSpace = trace.getStateSpace();
			
			TraceElement element = trace.getCurrent();
			// Workaround for StateSpace.eval only taking exactly a List<IEvalElement>, and not a List<ClassicalB>
			final List<IEvalElement> formulas = new ArrayList<>(this.formulaList.getItems());
			while (element != null) {
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
					newDatas.get(i).add(0, new XYChart.Data<>(-1, value));
				}
				
				element = element.getPrevious();
			}
		}
		
		for (int i = 0; i < newDatas.size(); i++) {
			final List<XYChart.Data<Number, Number>> newData = newDatas.get(i);
			
			for (int j = 0; j < newData.size(); j++) {
				newData.get(j).setXValue(j);
			}
			
			this.chart.getData().get(i).getData().setAll(newData);
		}
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
