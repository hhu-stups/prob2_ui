package de.prob2.ui.simulation;

import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;

public enum EvaluationMode {

	CLASSICAL_B,
	EVENT_B;

	public static EvaluationMode extractMode(AbstractModel model) {
		// TODO: Handle mode for other formalisms
		return switch (model) {
			case ClassicalBModel ignored -> EvaluationMode.CLASSICAL_B;
			case EventBModel ignored -> EvaluationMode.EVENT_B;
			default -> null;
		};
	}
}
