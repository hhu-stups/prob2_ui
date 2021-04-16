package de.prob2.ui.simulation.choice;



import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SimulationChoiceBindings {

	private final SimpleStringProperty simulationsProperty;

	private final SimpleStringProperty startAfterProperty;

	private final SimpleStringProperty startingPredicateProperty;

	private final SimpleStringProperty startingTimeProperty;

	private final SimpleStringProperty stepsProperty;

	private final SimpleStringProperty endingPredicateProperty;

	private final SimpleStringProperty endingTimeProperty;

	private final SimpleObjectProperty<SimulationMonteCarloChoice.SimulationStartingItem> startingItemProperty;

	private final SimpleObjectProperty<SimulationMonteCarloChoice.SimulationEndingItem> endingItemProperty;

	private final SimpleStringProperty monteCarloTimeProperty;

	private final SimpleStringProperty predicateProperty;

	private final SimpleObjectProperty<SimulationMonteCarloChoice.SimulationPropertyItem> checkingProperty;

	@Inject
	public SimulationChoiceBindings() {
		this.simulationsProperty = new SimpleStringProperty();
		this.startAfterProperty = new SimpleStringProperty();
		this.startingPredicateProperty = new SimpleStringProperty();
		this.startingTimeProperty = new SimpleStringProperty();
		this.stepsProperty = new SimpleStringProperty();
		this.endingPredicateProperty = new SimpleStringProperty();
		this.endingTimeProperty = new SimpleStringProperty();
		this.startingItemProperty = new SimpleObjectProperty<>();
		this.endingItemProperty = new SimpleObjectProperty<>();
		this.monteCarloTimeProperty = new SimpleStringProperty();
		this.predicateProperty = new SimpleStringProperty();
		this.checkingProperty = new SimpleObjectProperty();
	}

	public SimpleStringProperty simulationsProperty() {
		return simulationsProperty;
	}

	public SimpleStringProperty startAfterProperty() {
		return startAfterProperty;
	}

	public SimpleStringProperty startingPredicateProperty() {
		return startingPredicateProperty;
	}

	public SimpleStringProperty startingTimeProperty() {
		return startingTimeProperty;
	}

	public SimpleStringProperty stepsProperty() {
		return stepsProperty;
	}

	public SimpleStringProperty endingPredicateProperty() {
		return endingPredicateProperty;
	}

	public SimpleStringProperty endingTimeProperty() {
		return endingTimeProperty;
	}

	public SimpleObjectProperty<SimulationMonteCarloChoice.SimulationStartingItem> startingItemProperty() {
		return startingItemProperty;
	}

	public SimpleObjectProperty<SimulationMonteCarloChoice.SimulationEndingItem> endingItemProperty() {
		return endingItemProperty;
	}

	public SimpleStringProperty monteCarloTimeProperty() {
		return monteCarloTimeProperty;
	}

	public SimpleStringProperty predicateProperty() {
		return predicateProperty;
	}

	public SimpleObjectProperty<SimulationMonteCarloChoice.SimulationPropertyItem> checkingProperty() {
		return checkingProperty;
	}

}
