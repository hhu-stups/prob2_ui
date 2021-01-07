package de.prob2.ui.simulation.table;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.simulators.Simulator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class SimulationListViewDebugItem extends ListCell<SimulationDebugItem> {

	@FXML
	private VBox itemBox;


	private SimulationDebugItem item;

	private final CurrentTrace currentTrace;

	private final Simulator simulator;

	private final ResourceBundle bundle;

	public SimulationListViewDebugItem(final StageManager stageManager, final CurrentTrace currentTrace, final Simulator simulator, final ResourceBundle bundle) {
		stageManager.loadFXML(this,"simulation_list_view_item.fxml");
		this.item = null;
		this.currentTrace = currentTrace;
		this.simulator = simulator;
		this.bundle = bundle;
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.itemBox);
	}

	@Override
	protected void updateItem(final SimulationDebugItem item, final boolean empty) {
		super.updateItem(item, empty);
		this.item = item;
		if(item != null) {
			this.itemBox.getChildren().clear();

			Label lbOpName = new Label(item.getOpName());
			lbOpName.getStyleClass().add("name");
			this.itemBox.getChildren().add(lbOpName);

			if(!item.getTime().isEmpty()) {
				Label lbTime = new Label(String.format(bundle.getString("simulation.item.time"), item.getTime()));
				lbTime.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbTime);
			}

			if(!item.getDelayAsString().isEmpty()) {
				Label lbDelay = new Label(String.format(bundle.getString("simulation.item.delay"), item.getDelay()));
				lbDelay.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbDelay);
			}

			if(!item.getProbability().isEmpty()) {
				Label lbProbability = new Label(String.format(bundle.getString("simulation.item.probability"), item.getProbability()));
				lbProbability.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbProbability);

				if(currentTrace.getCurrentState() != null && currentTrace.getCurrentState().isInitialised()) {
					AbstractEvalResult evalResult = SimulationHelperFunctions.evaluateForSimulation(currentTrace.getCurrentState(), item.getProbability());
					Label lbProbabilityValue = new Label(String.format(bundle.getString("simulation.item.probabilityValue"), evalResult.toString()));
					lbProbabilityValue.getStyleClass().add("information");
					this.itemBox.getChildren().add(lbProbabilityValue);
				}
			}

			if(!item.getPriority().isEmpty()) {
				Label lbPriority = new Label(String.format(bundle.getString("simulation.item.priority"), item.getPriority()));
				lbPriority.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbPriority);
			}

			if(!item.getValuesAsString().isEmpty()) {
				Label lbVariableValues = new Label(String.format(bundle.getString("simulation.item.values"), item.getValuesAsString()));
				lbVariableValues.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbVariableValues);

				Map<String, Object> values = item.getValues();
				Map<String, String> evaluatedValues = new HashMap<>();

				if(currentTrace.getCurrentState() != null && currentTrace.getCurrentState().isInitialised()) {
					for (String key : values.keySet()) {
						Object value = values.get(key);
						if(value instanceof List) {
							// TODO
						} else {
							AbstractEvalResult evalResult = SimulationHelperFunctions.evaluateForSimulation(currentTrace.getCurrentState(), (String) values.get(key));
							evaluatedValues.put(key, evalResult.toString());
						}
					}
					Label lbEvaluatedValues = new Label(String.format(bundle.getString("simulation.item.concreteValues"), evaluatedValues.toString()));
					lbEvaluatedValues.getStyleClass().add("information");
					this.itemBox.getChildren().add(lbEvaluatedValues);
				}

			}
			this.setPrefHeight(itemBox.getChildren().size() * 20.0f);
			this.setGraphic(this.itemBox);
			this.setText("");
		} else {
			clear();
		}
	}

	public void clear() {
		itemBox.getChildren().clear();
		this.setGraphic(this.itemBox);
		this.setText("");
	}
}
