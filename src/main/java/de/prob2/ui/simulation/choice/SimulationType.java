package de.prob2.ui.simulation.choice;

import de.prob2.ui.internal.Translatable;

public enum SimulationType implements Translatable {

	MONTE_CARLO_SIMULATION("simulation.type.monteCarloSimulation"),
	HYPOTHESIS_TEST("simulation.type.hypothesisTest"),
	ESTIMATION("simulation.type.estimation"),
	;

	private final String translationKey;

	SimulationType(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslationKey() {
		return translationKey;
	}
}
