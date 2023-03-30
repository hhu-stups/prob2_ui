package de.prob2.ui.simulation.choice;

import com.google.inject.Singleton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.SimulationMode;
import de.prob2.ui.simulation.simulators.check.SimulationCheckingSimulator;
import javafx.beans.NamedArg;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@FXMLInjected
@Singleton
public class SimulationMonteCarloChoice extends GridPane {

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
	private Label lbSimulations;

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
	private Label lbCheckProperty;

	@FXML
	private CheckBox cbCheckProperty;

	private final I18n i18n;

	private final SimulationMode simulationMode;


	@Inject
	private SimulationMonteCarloChoice(final StageManager stageManager, final I18n i18n, final SimulationMode simulationMode) {
		super();
		this.i18n = i18n;
		this.simulationMode = simulationMode;
		stageManager.loadFXML(this, "simulation_monte_carlo_choice.fxml");
	}

	@FXML
	private void initialize() {
		tfMaxStepsBeforeProperty.visibleProperty().bind(cbMaxStepsBeforeProperty.selectedProperty());
		lbCheckProperty.visibleProperty().bind(Bindings.createBooleanBinding(() -> simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO, simulationMode.modeProperty()));
		cbCheckProperty.visibleProperty().bind(Bindings.createBooleanBinding(() -> simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO, simulationMode.modeProperty()));
		lbSimulations.visibleProperty().bind(Bindings.createBooleanBinding(() -> simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO, simulationMode.modeProperty()));
		tfSimulations.visibleProperty().bind(Bindings.createBooleanBinding(() -> simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO, simulationMode.modeProperty()));

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

		startingChoice.setConverter(new StringConverter<SimulationStartingItem>() {
			@Override
			public String toString(SimulationStartingItem object) {
				if(object == null) {
					return "";
				}
				return object.getStartingType().getName(i18n);
			}

			@Override
			public SimulationStartingItem fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String to SimulationStartingItem not supported");
			}
		});


		endingChoice.setConverter(new StringConverter<SimulationEndingItem>() {
			@Override
			public String toString(SimulationEndingItem object) {
				if(object == null) {
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
			int numberSimulations = Integer.parseInt(tfSimulations.getText());
			int stepsBeforeProperty = cbMaxStepsBeforeProperty.isSelected() ? Integer.parseInt(tfMaxStepsBeforeProperty.getText()) : 0;
			if(numberSimulations < 0 || stepsBeforeProperty < 0) {
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
		if(simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO) {
			information.put("EXECUTIONS", Integer.parseInt(tfSimulations.getText()));
		}
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
