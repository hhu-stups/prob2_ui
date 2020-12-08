package de.prob2.ui.simulation.table;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class SimulationListViewDebugItem extends ListCell<SimulationDebugItem> {

	@FXML
	private VBox itemBox;


	private SimulationDebugItem item;

	private final CurrentTrace currentTrace;

	private final ResourceBundle bundle;

	public SimulationListViewDebugItem(final StageManager stageManager, final CurrentTrace currentTrace, final ResourceBundle bundle) {
		stageManager.loadFXML(this,"simulation_list_view_item.fxml");
		this.item = null;
		this.currentTrace = currentTrace;
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
					// Note: Rodin parser does not have IF-THEN-ELSE nor REAL
					AbstractEvalResult evalResult = currentTrace.getCurrentState().eval(new ClassicalB(item.getProbability(), FormulaExpand.EXPAND));
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

			if(!item.getChoiceID().isEmpty()) {
				Label lbChoiceID = new Label(String.format(bundle.getString("simulation.item.choiceID"), item.getChoiceID()));
				lbChoiceID.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbChoiceID);
			}

			if(!item.getValuesAsString().isEmpty()) {
				Label lbVariableValues = new Label(String.format(bundle.getString("simulation.item.values"), item.getValuesAsString()));
				lbVariableValues.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbVariableValues);

				Map<String, String> values = item.getValues();
				Map<String, String> evaluatedValues = new HashMap<>(values);

				if(currentTrace.getCurrentState() != null && currentTrace.getCurrentState().isInitialised()) {
					for (String key : evaluatedValues.keySet()) {
						evaluatedValues.computeIfPresent(key, (k, v) -> {
							// Note: Rodin parser does not have IF-THEN-ELSE nor REAL
							AbstractEvalResult evalResult = currentTrace.getCurrentState().eval(new ClassicalB(v, FormulaExpand.EXPAND));
							return evalResult.toString();
						});
					}
					Label lbEvaluatedValues = new Label(String.format(bundle.getString("simulation.item.concreteValues"), evaluatedValues.toString()));
					lbEvaluatedValues.getStyleClass().add("information");
					this.itemBox.getChildren().add(lbEvaluatedValues);
				}

			}

			if(!item.getValuesProbability().isEmpty()) {
				Label lbVariableValuesProbability = new Label(String.format(bundle.getString("simulation.item.valuesProbabilities"), item.getValuesProbability()));
				lbVariableValuesProbability.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbVariableValuesProbability);

				if(currentTrace.getCurrentState() != null && currentTrace.getCurrentState().isInitialised()) {
					// Note: Rodin parser does not have IF-THEN-ELSE nor REAL
					AbstractEvalResult evalResult = currentTrace.getCurrentState().eval(new ClassicalB(item.getValuesProbability(), FormulaExpand.EXPAND));
					Label lbValuesProbability = new Label(String.format(bundle.getString("simulation.item.concreteValuesProbabilities"), evalResult.toString()));
					lbValuesProbability.getStyleClass().add("information");
					this.itemBox.getChildren().add(lbValuesProbability);
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
