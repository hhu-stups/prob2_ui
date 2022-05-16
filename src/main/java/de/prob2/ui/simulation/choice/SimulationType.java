package de.prob2.ui.simulation.choice;

public enum SimulationType {

	MONTE_CARLO_SIMULATION("simulation.type.monteCarloSimulation"),
	HYPOTHESIS_TEST("simulation.type.hypothesisTest"),
	ESTIMATION("simulation.type.estimation"),
	;

	private final String translationKey;

	SimulationType(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return translationKey;
	}
}
