package de.prob2.ui.simulation.choice;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.simulators.check.SimulationCheckingSimulator;

import javafx.beans.NamedArg;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

@FXMLInjected
@Singleton
public final class SimulationConditionChoice extends GridPane {
	public static class SimulationStartingItem {

		private final SimulationCheckingSimulator.StartingType startingType;

		public SimulationStartingItem(@NamedArg("startingType") SimulationCheckingSimulator.StartingType startingType) {
			this.startingType = startingType;
		}

		@Override
		public String toString() {
			return startingType.name();
		}

		public String getName(I18n i18n) {
			return i18n.translate(startingType.getKey());
		}

		public SimulationCheckingSimulator.StartingType getStartingType() {
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

		private final SimulationCheckingSimulator.EndingType endingType;

		public SimulationEndingItem(@NamedArg("endingType") SimulationCheckingSimulator.EndingType endingType) {
			this.endingType = endingType;
		}

		@Override
		public String toString() {
			return endingType.name();
		}

		public String getName(I18n i18n) {
			return i18n.translate(endingType.getKey());
		}

		public SimulationCheckingSimulator.EndingType getEndingType() {
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

	private SimulationChoosingStage choosingStage;

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
	private CheckBox cbMaxStepsBeforeProperty;

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
	private CheckBox cbStartingChoice;

	@FXML
	private ChoiceBox<SimulationStartingItem> startingChoice;

	@FXML
	private ChoiceBox<SimulationEndingItem> endingChoice;

	@FXML
	private ChoiceBox<SimulationType> simulationChoice;

	private final I18n i18n;


	@Inject
	private SimulationConditionChoice(final StageManager stageManager, final I18n i18n) {
		super();
		this.i18n = i18n;
		stageManager.loadFXML(this, "simulation_condition_choice.fxml");
	}

	@FXML
	private void initialize() {
		tfMaxStepsBeforeProperty.visibleProperty().bind(cbMaxStepsBeforeProperty.selectedProperty());
		simulationChoice.getSelectionModel().select(0);

		startingChoice.visibleProperty().bind(cbStartingChoice.selectedProperty());
		cbStartingChoice.selectedProperty().addListener((observable, from, to) -> {
			startingChoice.getSelectionModel().clearSelection();
			startingChoice.getSelectionModel().select(startingChoice.getSelectionModel().getSelectedItem());
		});

		startingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.getChildren().removeAll(lbStartAfter, tfStartAfter, lbStartingPredicate, tfStartingPredicate, lbStartingTime, tfStartingTime);
			if(to != null) {
				switch (to.getStartingType()) {
					case START_AFTER_STEPS:
						this.add(lbStartAfter, 1, 4);
						this.add(tfStartAfter, 2, 4);
						break;
					case STARTING_PREDICATE:
					case STARTING_PREDICATE_ACTIVATED:
						this.add(lbStartingPredicate, 1, 4);
						this.add(tfStartingPredicate, 2, 4);
						break;
					case STARTING_TIME:
						this.add(lbStartingTime, 1, 4);
						this.add(tfStartingTime, 2, 4);
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
						this.add(lbSteps, 1, 6);
						this.add(tfSteps, 2, 6);
						break;
					case ENDING_PREDICATE:
						this.add(lbEndingPredicate, 1, 6);
						this.add(tfEndingPredicate, 2, 6);
						break;
					case ENDING_TIME:
						this.add(lbEndingTime, 1, 6);
						this.add(tfEndingTime, 2, 6);
						break;
					default:
						break;
				}
			}
			choosingStage.sizeToScene();
		});

		startingChoice.setConverter(new StringConverter<>() {
			@Override
			public String toString(SimulationStartingItem object) {
				if (object == null) {
					return "";
				}
				return object.getStartingType().getName(i18n);
			}

			@Override
			public SimulationStartingItem fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String to SimulationStartingItem not supported");
			}
		});


		endingChoice.setConverter(new StringConverter<>() {
			@Override
			public String toString(SimulationEndingItem object) {
				if (object == null) {
					return "";
				}
				return object.getEndingType().getName(i18n);
			}

			@Override
			public SimulationEndingItem fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String to SimulationEndingItem not supported");
			}
		});
	}

	public boolean checkSelection() {
		SimulationStartingItem startingItem = startingChoice.getSelectionModel().getSelectedItem();
		SimulationEndingItem endingItem = endingChoice.getSelectionModel().getSelectedItem();

		if(endingItem == null) {
			return false;
		}
		try {
			int stepsBeforeProperty = cbMaxStepsBeforeProperty.isSelected() ? Integer.parseInt(tfMaxStepsBeforeProperty.getText()) : 0;
			if(stepsBeforeProperty < 0) {
				return false;
			}
			if(startingItem != null) {
				switch (startingItem.getStartingType()) {
					case START_AFTER_STEPS:
						int startAfterSteps = Integer.parseInt(tfStartAfter.getText());
						if (startAfterSteps < 0) {
							return false;
						}
						break;
					case STARTING_PREDICATE:
					case STARTING_PREDICATE_ACTIVATED:
						if (tfStartingPredicate.getText().isEmpty()) {
							return false;
						}
						break;
					case STARTING_TIME:
						int startingTime = Integer.parseInt(tfStartingTime.getText());
						if (startingTime < 0) {
							return false;
						}
						break;
					default:
						break;
				}
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
		information.put("MAX_STEPS_BEFORE_PROPERTY", cbMaxStepsBeforeProperty.isSelected() ? Integer.parseInt(tfMaxStepsBeforeProperty.getText()): 0);

		SimulationStartingItem startingItem = startingChoice.getSelectionModel().getSelectedItem();
		if(startingItem != null) {
			switch (startingItem.getStartingType()) {
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

	public void setInformation(SimulationType type, Map<String, Object> object) {
		cbMaxStepsBeforeProperty.setSelected(false);
		cbStartingChoice.setSelected(false);

		if(object.containsKey("MAX_STEPS_BEFORE_PROPERTY") && !"0".equals(object.get("MAX_STEPS_BEFORE_PROPERTY").toString())) {
			cbMaxStepsBeforeProperty.setSelected(true);
			tfMaxStepsBeforeProperty.setText(object.get("MAX_STEPS_BEFORE_PROPERTY").toString());
		}

		if(object.containsKey("START_AFTER_STEPS")) {
			cbStartingChoice.setSelected(true);
			startingChoice.getSelectionModel().select(new SimulationStartingItem(SimulationCheckingSimulator.StartingType.START_AFTER_STEPS));
			tfStartAfter.setText(object.get("START_AFTER_STEPS").toString());
		}

		if(object.containsKey("STARTING_PREDICATE")) {
			cbStartingChoice.setSelected(true);
			startingChoice.getSelectionModel().select(new SimulationStartingItem(SimulationCheckingSimulator.StartingType.STARTING_PREDICATE));
			tfStartingPredicate.setText(object.get("STARTING_PREDICATE").toString());
		}

		if(object.containsKey("STARTING_PREDICATE_ACTIVATED")) {
			cbStartingChoice.setSelected(true);
			startingChoice.getSelectionModel().select(new SimulationStartingItem(SimulationCheckingSimulator.StartingType.STARTING_PREDICATE_ACTIVATED));
			tfStartingPredicate.setText(object.get("STARTING_PREDICATE_ACTIVATED").toString());
		}

		if(object.containsKey("STARTING_TIME")) {
			cbStartingChoice.setSelected(true);
			startingChoice.getSelectionModel().select(new SimulationStartingItem(SimulationCheckingSimulator.StartingType.STARTING_TIME));
			tfStartingTime.setText(object.get("STARTING_TIME").toString());
		}


		if(object.containsKey("STEPS_PER_EXECUTION")) {
			endingChoice.getSelectionModel().select(new SimulationEndingItem(SimulationCheckingSimulator.EndingType.NUMBER_STEPS));
			tfSteps.setText(object.get("STEPS_PER_EXECUTION").toString());
		}

		if(object.containsKey("ENDING_PREDICATE")) {
			endingChoice.getSelectionModel().select(new SimulationEndingItem(SimulationCheckingSimulator.EndingType.ENDING_PREDICATE));
			tfEndingPredicate.setText(object.get("ENDING_PREDICATE").toString());
		}

		if(object.containsKey("ENDING_TIME")) {
			endingChoice.getSelectionModel().select(new SimulationEndingItem(SimulationCheckingSimulator.EndingType.ENDING_TIME));
			tfEndingTime.setText(object.get("ENDING_TIME").toString());
		}

		simulationChoice.getSelectionModel().select(type);
	}

	public void reset() {
		cbMaxStepsBeforeProperty.setSelected(false);
		cbStartingChoice.setSelected(false);
		tfMaxStepsBeforeProperty.clear();
		tfStartingTime.clear();
		tfStartingPredicate.clear();
		tfStartAfter.clear();
		tfSteps.clear();
		tfEndingTime.clear();
		tfEndingPredicate.clear();
		endingChoice.getSelectionModel().select(new SimulationEndingItem(SimulationCheckingSimulator.EndingType.NUMBER_STEPS));
		simulationChoice.getSelectionModel().select(SimulationType.MONTE_CARLO_SIMULATION);
	}


	public ChoiceBox<SimulationType> simulationChoice() {
		return simulationChoice;
	}

	public boolean checkProperty() {
		SimulationType type = simulationChoice.getSelectionModel().getSelectedItem();
		return type != null && type != SimulationType.MONTE_CARLO_SIMULATION;
	}

	public void setChoosingStage(SimulationChoosingStage choosingStage) {
		this.choosingStage = choosingStage;
	}
}
