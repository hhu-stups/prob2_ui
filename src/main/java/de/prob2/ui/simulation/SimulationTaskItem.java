package de.prob2.ui.simulation;

import de.prob.check.tracereplay.PersistentTransition;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.table.SimulationChoiceDebugItem;
import de.prob2.ui.simulation.table.SimulationDebugItem;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.simulation.table.SimulationOperationDebugItem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.layout.VBox;

import java.util.Map;

public class SimulationTaskItem extends TableCell<SimulationItem, String> {

	@FXML
	private VBox itemBox;

	private SimulationItem item;

	private final I18n i18n;

	public SimulationTaskItem(final StageManager stageManager, final I18n i18n) {
		super();
		stageManager.loadFXML(this,"simulation_task_item.fxml");
		this.item = null;
		this.i18n = i18n;
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.itemBox);
	}

	@Override
	public void updateItem(final String item, final boolean empty) {
		super.updateItem(item, empty);
		final TableRow<SimulationItem> tableRow = this.getTableRow();
		this.item = tableRow.getItem();
		if(this.item != null) {
			this.itemBox.getChildren().clear();
			updateItem();
			this.setPrefHeight(itemBox.getChildren().size() * 20.0f);
			this.setGraphic(this.itemBox);
			this.setText("");
		} else {
			clear();
		}
	}

	public void updateItem() {
		if(item.getId() != null && !item.getId().isEmpty()) {
			Label lbID = new Label(item.getId());
			lbID.getStyleClass().add("id");
			this.itemBox.getChildren().add(lbID);
		}

		Map<String, Object> information = item.getInformation();

		if(information.containsKey("EXECUTIONS")) {

			Label lbMonteCarloSimulation = new Label("Monte Carlo Simulation");
			lbMonteCarloSimulation.getStyleClass().add("id");
			this.itemBox.getChildren().add(lbMonteCarloSimulation);

			Label lbExecutions = new Label(String.format("Executions: %s", information.get("EXECUTIONS")));
			lbExecutions.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbExecutions);
		}

		if(information.containsKey("MAX_STEPS_BEFORE_PROPERTY") && (int) information.get("MAX_STEPS_BEFORE_PROPERTY") > 0) {
			Label lbMaxStepsBeforeProperty = new Label(String.format("Max Steps before Start: %s", information.get("MAX_STEPS_BEFORE_PROPERTY")));
			lbMaxStepsBeforeProperty.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbMaxStepsBeforeProperty);
		}

		if(information.containsKey("START_AFTER_STEPS")) {
			Label lbStartAfterSteps = new Label(String.format("Start after %s steps", information.get("START_AFTER_STEPS")));
			lbStartAfterSteps.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbStartAfterSteps);
		}

		if(information.containsKey("STARTING_PREDICATE")) {
			Label lbStartingPredicate = new Label(String.format("Starting predicate: %s", information.get("STARTING_PREDICATE")));
			lbStartingPredicate.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbStartingPredicate);
		}

		if(information.containsKey("STARTING_PREDICATE_ACTIVATED")) {
			Label lbStartingPredicate = new Label(String.format("Starting predicate (activated): %s", information.get("STARTING_PREDICATE_ACTIVATED")));
			lbStartingPredicate.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbStartingPredicate);
		}

		if(information.containsKey("STARTING_TIME")) {
			Label lbStartingTime = new Label(String.format("Starting time: %s", information.get("STARTING_TIME")));
			lbStartingTime.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbStartingTime);
		}


		if(information.containsKey("STEPS_PER_EXECUTION")) {
			Label lbStepsPerExecution = new Label(String.format("Steps per execution: %s", information.get("STEPS_PER_EXECUTION")));
			lbStepsPerExecution.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbStepsPerExecution);
		}

		if(information.containsKey("ENDING_PREDICATE")) {
			Label lbEndingPredicate = new Label(String.format("Ending predicate: %s", information.get("ENDING_PREDICATE")));
			lbEndingPredicate.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbEndingPredicate);
		}

		if(information.containsKey("ENDING_TIME")) {
			Label lbEndingTime = new Label(String.format("Ending time: %s", information.get("ENDING_TIME")));
			lbEndingTime.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbEndingTime);
		}

		if(item.getType() == SimulationType.ESTIMATION || item.getType() == SimulationType.HYPOTHESIS_TEST) {
			Label lbProperty = new Label("Property");
			lbProperty.getStyleClass().add("id");
			this.itemBox.getChildren().add(lbProperty);
		}


		if(information.containsKey("CHECKING_TYPE")) {
			Label lbCheckingType = new Label(String.format("Checking type: %s", information.get("CHECKING_TYPE")));
			lbCheckingType.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbCheckingType);
		}

		if(information.containsKey("PREDICATE")) {
			Label lbPredicate = new Label(String.format("Checking predicate: %s", information.get("PREDICATE")));
			lbPredicate.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbPredicate);
		}

		if(information.containsKey("TIME")) {
			Label lbTime = new Label(String.format("Checking time: %s", information.get("TIME")));
			lbTime.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbTime);
		}

		if(item.getType() == SimulationType.HYPOTHESIS_TEST) {
			if(information.containsKey("HYPOTHESIS_CHECKING_TYPE")) {
				Label lbHypothesisTest = new Label(String.format("Hypothesis Test (%s)", information.get("HYPOTHESIS_CHECKING_TYPE")));
				lbHypothesisTest.getStyleClass().add("id");
				this.itemBox.getChildren().add(lbHypothesisTest);
			}

			if(information.containsKey("PROBABILITY")) {
				Label lbProbability = new Label(String.format("Probability: %s", information.get("PROBABILITY")));
				lbProbability.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbProbability);
			}

			if(information.containsKey("SIGNIFICANCE")) {
				Label lbSignificance = new Label(String.format("Significance level: %s", information.get("SIGNIFICANCE")));
				lbSignificance.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbSignificance);
			}
		}


		if(item.getType() == SimulationType.ESTIMATION) {
			if (information.containsKey("ESTIMATION_TYPE")) {
				Label lbEstimationType = new Label(String.format("Estimation type (%s)", information.get("ESTIMATION_TYPE")));
				lbEstimationType.getStyleClass().add("id");
				this.itemBox.getChildren().add(lbEstimationType);
			}

			if (information.containsKey("DESIRED_VALUE")) {
				Label lbDesiredValue = new Label(String.format("Desired value: %s", information.get("DESIRED_VALUE")));
				lbDesiredValue.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbDesiredValue);
			}

			if (information.containsKey("EPSILON")) {
				Label lbEpsilon = new Label(String.format("Epsilon: %s", information.get("EPSILON")));
				lbEpsilon.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbEpsilon);
			}
		}

	}

	public void clear() {
		itemBox.getChildren().clear();
		this.setGraphic(this.itemBox);
		this.setText("");
	}
}
