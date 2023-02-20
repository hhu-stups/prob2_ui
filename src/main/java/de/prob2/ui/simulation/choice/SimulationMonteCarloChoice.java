package de.prob2.ui.simulation.choice;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.simulators.check.SimulationMonteCarlo;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@FXMLInjected
public class SimulationMonteCarloChoice extends GridPane {

	public static class SimulationStartingItem {

		private final SimulationMonteCarlo.StartingType startingType;

		public SimulationStartingItem(@NamedArg("startingType") SimulationMonteCarlo.StartingType startingType) {
			this.startingType = startingType;
		}

		@Override
		public String toString() {
			return startingType.getName();
		}

		public SimulationMonteCarlo.StartingType getStartingType() {
			return startingType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SimulationStartingItem that = (SimulationStartingItem) o;
			return startingType == that.startingType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(startingType);
		}
	}

	public static class SimulationEndingItem {

		private final SimulationMonteCarlo.EndingType endingType;

		public SimulationEndingItem(@NamedArg("endingType") SimulationMonteCarlo.EndingType endingType) {
			this.endingType = endingType;
		}

		@Override
		public String toString() {
			return endingType.getName();
		}

		public SimulationMonteCarlo.EndingType getEndingType() {
			return endingType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SimulationEndingItem that = (SimulationEndingItem) o;
			return endingType == that.endingType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(endingType);
		}
	}

	public static class SimulationPropertyItem {

		private final SimulationCheckingType checkingType;

		public SimulationPropertyItem(@NamedArg("checkingType") SimulationCheckingType checkingType) {
			this.checkingType = checkingType;
		}

		@Override
		public String toString() {
			return checkingType.getName();
		}

		public SimulationCheckingType getCheckingType() {
			return checkingType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SimulationPropertyItem that = (SimulationPropertyItem) o;
			return checkingType == that.checkingType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(checkingType);
		}
	}

	private SimulationChoosingStage choosingStage;

	@FXML
	private TextField tfSimulations;

	@FXML
	private Label lbStartAfter;

	@FXML
	private TextField tfStartAfter;

	@FXML
	private Label lbStartingPredicate;

	@FXML
	private TextField tfStartingPredicate;

	@FXML
	private Label lbStartingTime;

	@FXML
	private TextField tfStartingTime;

	@FXML
	private Label lbSteps;

	@FXML
	private TextField tfSteps;

	@FXML
	private TextField tfMaxStepsBeforeProperty;

	@FXML
	private Label lbEndingPredicate;

	@FXML
	private TextField tfEndingPredicate;

	@FXML
	private Label lbEndingTime;

	@FXML
	private TextField tfEndingTime;

	@FXML
	private ChoiceBox<SimulationStartingItem> startingChoice;

	@FXML
	private ChoiceBox<SimulationEndingItem> endingChoice;

	@FXML
	private CheckBox cbCheckProperty;


	@Inject
	private SimulationMonteCarloChoice(final StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "simulation_monte_carlo_choice.fxml");
	}

	private SimulationMonteCarloChoice() {
		super();
		//Default constructor for super classes using other FXML file
	}

	@FXML
	private void initialize() {
		startingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.getChildren().removeAll(lbStartAfter, tfStartAfter, lbStartingPredicate, tfStartingPredicate, lbStartingTime, tfStartingTime);
			if(to != null) {
				switch (to.getStartingType()) {
					case NO_CONDITION:
						break;
					case START_AFTER_STEPS:
						this.add(lbStartAfter, 1, 5);
						this.add(tfStartAfter, 2, 5);
						break;
					case STARTING_PREDICATE:
					case STARTING_PREDICATE_ACTIVATED:
						this.add(lbStartingPredicate, 1, 5);
						this.add(tfStartingPredicate, 2, 5);
						break;
					case STARTING_TIME:
						this.add(lbStartingTime, 1, 5);
						this.add(tfStartingTime, 2, 5);
						break;
					default:
						break;
				}
			}
			choosingStage.sizeToScene();
		});

		endingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.getChildren().removeAll(lbSteps, tfSteps, lbEndingPredicate, tfEndingPredicate, lbEndingTime, tfEndingTime);
			if(to != null) {
				switch (to.getEndingType()) {
					case NUMBER_STEPS:
						this.add(lbSteps, 1, 7);
						this.add(tfSteps, 2, 7);
						break;
					case ENDING_PREDICATE:
						this.add(lbEndingPredicate, 1, 7);
						this.add(tfEndingPredicate, 2, 7);
						break;
					case ENDING_TIME:
						this.add(lbEndingTime, 1, 7);
						this.add(tfEndingTime, 2, 7);
						break;
					default:
						break;
				}
			}
			choosingStage.sizeToScene();
		});
	}

	public boolean checkSelection() {
		SimulationStartingItem startingItem = startingChoice.getSelectionModel().getSelectedItem();
		SimulationEndingItem endingItem = endingChoice.getSelectionModel().getSelectedItem();

		if(startingItem == null || endingItem == null) {
			return false;
		}
		try {
			int numberSimulations = Integer.parseInt(tfSimulations.getText());
			int stepsBeforeProperty = Integer.parseInt(tfMaxStepsBeforeProperty.getText());
			if(numberSimulations < 0 || stepsBeforeProperty < 0) {
				return false;
			}
			switch (startingItem.getStartingType()) {
				case NO_CONDITION:
					break;
				case START_AFTER_STEPS:
					int startAfterSteps = Integer.parseInt(tfStartAfter.getText());
					if(startAfterSteps < 0) {
						return false;
					}
					break;
				case STARTING_PREDICATE:
				case STARTING_PREDICATE_ACTIVATED:
					if(tfStartingPredicate.getText().isEmpty()) {
						return false;
					}
					break;
				case STARTING_TIME:
					int startingTime = Integer.parseInt(tfStartingTime.getText());
					if(startingTime < 0) {
						return false;
					}
					break;
				default:
					break;
			}

			switch(endingItem.getEndingType()) {
				case NUMBER_STEPS:
					int numberSteps = Integer.parseInt(tfSteps.getText());
					if(numberSteps < 0) {
						return false;
					}
					break;
				case ENDING_PREDICATE:
					if(tfEndingPredicate.getText().isEmpty()) {
						return false;
					}
					break;
				case ENDING_TIME:
					int endingTime = Integer.parseInt(tfEndingTime.getText());
					if(endingTime < 0) {
						return false;
					}
					break;
				default:
					break;
			}

		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public Map<String, Object> extractInformation() {
		Map<String, Object> information = new HashMap<>();
		information.put("EXECUTIONS", Integer.parseInt(tfSimulations.getText()));
		information.put("MAX_STEPS_BEFORE_PROPERTY", Integer.parseInt(tfMaxStepsBeforeProperty.getText()));

		SimulationStartingItem startingItem = startingChoice.getSelectionModel().getSelectedItem();
		if(startingItem != null) {
			switch (startingItem.getStartingType()) {
				case NO_CONDITION:
					break;
				case START_AFTER_STEPS:
					information.put("START_AFTER_STEPS", Integer.parseInt(tfStartAfter.getText()));
					break;
				case STARTING_PREDICATE:
					information.put("STARTING_PREDICATE", tfStartingPredicate.getText());
					break;
				case STARTING_PREDICATE_ACTIVATED:
					information.put("STARTING_PREDICATE_ACTIVATED", tfStartingPredicate.getText());
					break;
				case STARTING_TIME:
					information.put("STARTING_TIME", Integer.parseInt(tfStartingTime.getText()));
					break;
				default:
					break;
			}
		}

		SimulationEndingItem endingItem = endingChoice.getSelectionModel().getSelectedItem();
		if(endingItem != null) {
			switch(endingItem.getEndingType()) {
				case NUMBER_STEPS:
					information.put("STEPS_PER_EXECUTION", Integer.parseInt(tfSteps.getText()));
					break;
				case ENDING_PREDICATE:
					information.put("ENDING_PREDICATE", tfEndingPredicate.getText());
					break;
				case ENDING_TIME:
					information.put("ENDING_TIME", Integer.parseInt(tfEndingTime.getText()));
					break;
				default:
					break;
			}
		}
		return information;
	}

	public BooleanProperty checkPropertyProperty() {
		return cbCheckProperty.selectedProperty();
	}

	public boolean checkProperty() {
		return cbCheckProperty.isSelected();
	}

	public void setChoosingStage(SimulationChoosingStage choosingStage) {
		this.choosingStage = choosingStage;
	}
}
