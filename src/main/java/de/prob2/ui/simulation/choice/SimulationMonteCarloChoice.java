package de.prob2.ui.simulation.choice;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.simulators.check.SimulationMonteCarlo;
import javafx.beans.NamedArg;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
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

	public static class SimulationInitialItem {

		private final SimulationMonteCarlo.InitialType initialType;

		public SimulationInitialItem(@NamedArg("initialType") SimulationMonteCarlo.InitialType initialType) {
			this.initialType = initialType;
		}

		@Override
		public String toString() {
			return initialType.getName();
		}

		public SimulationMonteCarlo.InitialType getInitialType() {
			return initialType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SimulationInitialItem that = (SimulationInitialItem) o;
			return initialType == that.initialType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(initialType);
		}
	}

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

	protected SimulationChoosingStage choosingStage;

	@FXML
	protected Label lbInitialSteps;

	@FXML
	protected TextField tfInitialSteps;

	@FXML
	protected Label lbInitialPredicate;

	@FXML
	protected TextField tfInitialPredicate;

	@FXML
	protected Label lbInitialTime;

	@FXML
	protected TextField tfInitialTime;

	@FXML
	protected TextField tfSimulations;

	@FXML
	protected Label lbStartAfter;

	@FXML
	protected TextField tfStartAfter;

	@FXML
	protected Label lbStartingPredicate;

	@FXML
	protected TextField tfStartingPredicate;

	@FXML
	protected Label lbStartingTime;

	@FXML
	protected TextField tfStartingTime;

	@FXML
	protected Label lbSteps;

	@FXML
	protected TextField tfSteps;

	@FXML
	protected TextField tfMaxStepsBeforeProperty;

	@FXML
	protected Label lbEndingPredicate;

	@FXML
	protected TextField tfEndingPredicate;

	@FXML
	protected Label lbEndingTime;

	@FXML
	protected TextField tfEndingTime;

	@FXML
	protected ChoiceBox<SimulationInitialItem> initialChoice;

	@FXML
	protected ChoiceBox<SimulationStartingItem> startingChoice;

	@FXML
	protected ChoiceBox<SimulationEndingItem> endingChoice;


	@Inject
	protected SimulationMonteCarloChoice(final StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "simulation_monte_carlo_choice.fxml");
	}

	protected SimulationMonteCarloChoice() {
		super();
		//Default constructor for super classes using other FXML file
	}

	@FXML
	protected void initialize() {
		initialChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.getChildren().removeAll(lbInitialSteps, tfInitialSteps, lbInitialTime, tfInitialTime, lbInitialPredicate, tfInitialPredicate);
			if(to != null) {
				switch (to.getInitialType()) {
					case NO_CONDITION:
						break;
					case INITIAL_STEPS:
						this.add(lbInitialSteps, 1, 4);
						this.add(tfInitialSteps, 2, 4);
						break;
					case INITIAL_PREDICATE:
						this.add(lbInitialPredicate, 1, 4);
						this.add(tfInitialPredicate, 2, 4);
						break;
					case INITIAL_TIME:
						this.add(lbInitialTime, 1, 4);
						this.add(tfInitialTime, 2, 4);
						break;
					default:
						break;
				}
			}
			choosingStage.sizeToScene();
		});

		startingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.getChildren().removeAll(lbStartAfter, tfStartAfter, lbStartingPredicate, tfStartingPredicate, lbStartingTime, tfStartingTime);
			if(to != null) {
				switch (to.getStartingType()) {
					case NO_CONDITION:
						break;
					case START_AFTER_STEPS:
						this.add(lbStartAfter, 1, 6);
						this.add(tfStartAfter, 2, 6);
						break;
					case STARTING_PREDICATE:
						this.add(lbStartingPredicate, 1, 6);
						this.add(tfStartingPredicate, 2, 6);
						break;
					case STARTING_TIME:
						this.add(lbStartingTime, 1, 6);
						this.add(tfStartingTime, 2, 6);
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
						this.add(lbSteps, 1, 8);
						this.add(tfSteps, 2, 8);
						break;
					case ENDING_PREDICATE:
						this.add(lbEndingPredicate, 1, 8);
						this.add(tfEndingPredicate, 2, 8);
						break;
					case ENDING_TIME:
						this.add(lbEndingTime, 1, 8);
						this.add(tfEndingTime, 2, 8);
						break;
					default:
						break;
				}
			}
			choosingStage.sizeToScene();
		});
	}

	public boolean checkSelection() {
		SimulationInitialItem initialItem = initialChoice.getSelectionModel().getSelectedItem();
		SimulationStartingItem startingItem = startingChoice.getSelectionModel().getSelectedItem();
		SimulationEndingItem endingItem = endingChoice.getSelectionModel().getSelectedItem();

		if(initialItem == null || startingItem == null || endingItem == null) {
			return false;
		}
		try {
			int numberSimulations = Integer.parseInt(tfSimulations.getText());
			int stepsBeforeProperty = Integer.parseInt(tfMaxStepsBeforeProperty.getText());
			if(numberSimulations < 0 || stepsBeforeProperty < 0) {
				return false;
			}
			switch (initialItem.getInitialType()) {
				case NO_CONDITION:
					break;
				case INITIAL_STEPS:
					int initialSteps = Integer.parseInt(tfInitialSteps.getText());
					if(initialSteps < 0) {
						return false;
					}
					break;
				case INITIAL_PREDICATE:
					if(tfInitialPredicate.getText().isEmpty()) {
						return false;
					}
					break;
				case INITIAL_TIME:
					int initialTime = Integer.parseInt(tfInitialTime.getText());
					if(initialTime < 0) {
						return false;
					}
					break;
				default:
					break;
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

		SimulationInitialItem initialItem = initialChoice.getSelectionModel().getSelectedItem();
		if(initialItem != null) {
			switch (initialItem.getInitialType()) {
				case NO_CONDITION:
					break;
				case INITIAL_STEPS:
					information.put("INITIAL_STEPS", Integer.parseInt(tfInitialSteps.getText()));
					break;
				case INITIAL_PREDICATE:
					information.put("INITIAL_PREDICATE", tfInitialPredicate.getText());
					break;
				case INITIAL_TIME:
					information.put("INITIAL_TIME", Integer.parseInt(tfInitialTime.getText()));
					break;
				default:
					break;
			}
		}

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

	public void setSimulationChoosingStage(SimulationChoosingStage choosingStage) {
		this.choosingStage = choosingStage;
	}

	public void bindSimulationsProperty(SimpleStringProperty property) {
		tfSimulations.textProperty().bindBidirectional(property);
	}
	
	public void bindInitialProperty(SimpleStringProperty initialStepsProperty, SimpleStringProperty initialPredicateProperty, SimpleStringProperty initialTimeProperty) {
		tfInitialSteps.textProperty().bindBidirectional(initialStepsProperty);
		tfInitialPredicate.textProperty().bindBidirectional(initialPredicateProperty);
		tfInitialTime.textProperty().bindBidirectional(initialTimeProperty);
	}

	public void bindStartingProperty(SimpleStringProperty startAfterProperty, SimpleStringProperty startingPredicateProperty, SimpleStringProperty startingTimeProperty) {
		tfStartAfter.textProperty().bindBidirectional(startAfterProperty);
		tfStartingPredicate.textProperty().bindBidirectional(startingPredicateProperty);
		tfStartingTime.textProperty().bindBidirectional(startingTimeProperty);
	}

	public void bindEndingProperty(SimpleStringProperty stepsProperty, SimpleStringProperty endingPredicateProperty, SimpleStringProperty endingTimeProperty) {
		tfSteps.textProperty().bindBidirectional(stepsProperty);
		tfEndingPredicate.textProperty().bindBidirectional(endingPredicateProperty);
		tfEndingTime.textProperty().bindBidirectional(endingTimeProperty);
	}

	public void bindInitialItemProperty(SimpleObjectProperty<SimulationInitialItem> property) {
		// Bind bidirectional does not work on ReadOnlyObjectProperty
		initialChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(!Objects.equals(from, to)) {
				property.set(to);
			}
		});
		property.addListener((observable, from, to) -> {
			if(!Objects.equals(from, to)) {
				initialChoice.getSelectionModel().select(to);
			}
		});
	}

	public void bindStartingItemProperty(SimpleObjectProperty<SimulationStartingItem> property) {
		// Bind bidirectional does not work on ReadOnlyObjectProperty
		startingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(!Objects.equals(from, to)) {
				property.set(to);
			}
		});
		property.addListener((observable, from, to) -> {
			if(!Objects.equals(from, to)) {
				startingChoice.getSelectionModel().select(to);
			}
		});
	}

	public void bindEndingItemProperty(SimpleObjectProperty<SimulationEndingItem> property) {
		// Bind bidirectional does not work on ReadOnlyObjectProperty
		endingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(!Objects.equals(from, to)) {
				property.set(to);
			}
		});
		property.addListener((observable, from, to) -> {
			if(!Objects.equals(from, to)) {
				endingChoice.getSelectionModel().select(to);
			}
		});
	}


}
