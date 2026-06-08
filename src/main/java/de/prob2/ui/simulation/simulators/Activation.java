package de.prob2.ui.simulation.simulators;

import java.util.List;
import java.util.Map;

import de.prob2.ui.simulation.configuration.ActivationKind;
import de.prob2.ui.simulation.configuration.TransitionSelection;

public record Activation(
		String id,
		List<String> operation,
		Map<String, String> paramValues,
		int time,
		String additionalGuards,
		ActivationKind activationKind,
		Map<String, String> fixedVariables,
		Map<String, Map<String, String>> probabilisticVariables,
		TransitionSelection transitionSelection,
		List<String> firingTransitionParameters,
		String firingTransitionParametersPredicate,
		String withPredicate
) {

	public Activation decreaseTime(int delta) {
		return new Activation(
				this.id(),
				this.operation(),
				this.paramValues(),
				this.time() - delta,
				this.additionalGuards(),
				this.activationKind(),
				this.fixedVariables(),
				this.probabilisticVariables(),
				this.transitionSelection(),
				this.firingTransitionParameters(),
				this.firingTransitionParametersPredicate(),
				this.withPredicate()
		);
	}
}
