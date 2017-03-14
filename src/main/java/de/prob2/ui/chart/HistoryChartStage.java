package de.prob2.ui.chart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.StateSpace;
import de.prob.statespace.TraceElement;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class HistoryChartStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(HistoryChartStage.class);
	
	@FXML private LineChart<Number, Number> chart;
	
	private final CurrentTrace currentTrace;
	private final List<IEvalElement> formulas;
	
	@Inject
	private HistoryChartStage(final StageManager stageManager, final CurrentTrace currentTrace) {
		super();
		
		this.currentTrace = currentTrace;
		this.formulas = Arrays.asList(
			new ClassicalB("card(active)"),
			new ClassicalB("card(ready)"),
			new ClassicalB("card(waiting)"),
			new ClassicalB("PID1 : waiting"),
			new ClassicalB("PID2 : waiting"),
			new ClassicalB("PID3 : waiting")
		);
		
		stageManager.loadFXML(this, "history_chart_stage.fxml", this.getClass().getName());
	}
	
	@FXML
	private void initialize() {
		for (final IEvalElement ee : this.formulas) {
			this.chart.getData().add(new XYChart.Series<>(ee.getCode(), FXCollections.observableArrayList()));
		}
		
		this.currentTrace.addListener((observable, from, to) -> {
			final List<List<XYChart.Data<Number, Number>>> newDatas = new ArrayList<>();
			for (int i = 0; i < this.chart.getData().size(); i++) {
				newDatas.add(new ArrayList<>());
			}
			
			if (to != null) {
				final StateSpace stateSpace = to.getStateSpace();
				
				TraceElement element = to.getCurrent();
				while (element != null) {
					final List<AbstractEvalResult> results = stateSpace.eval(element.getCurrentState(), this.formulas);
					
					for (int i = 0; i < results.size(); i++) {
						final AbstractEvalResult result = results.get(i);
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
		});
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
