package de.prob2.ui.simulation.simulators;

import java.util.List;
import java.util.Map;

import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.ProbabilisticVariables;

public record Activation(
		String operation,
		int time,
		String additionalGuards,
		ActivationOperationConfiguration.ActivationKind activationKind,
		Map<String, String> fixedVariables,
		ProbabilisticVariables probabilisticVariables,
		List<String> firingTransitionParameters,
		String firingTransitionParametersPredicate,
		String withPredicate
) {

	public Activation decreaseTime(int delta) {
		return new Activation(
				this.operation(),
				this.time() - delta,
				this.additionalGuards(),
				this.activationKind(),
				this.fixedVariables(),
				this.probabilisticVariables(),
				this.firingTransitionParameters(),
				this.firingTransitionParametersPredicate(),
				this.withPredicate()
		);
	}
}
