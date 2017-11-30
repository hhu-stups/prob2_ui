package de.prob2.ui.verifications.tracereplay;

import java.util.List;

import de.prob.statespace.Transition;

public class ReplayTraceTransition {
	private final String operationName;
	private final List<String> parameterPredicates;

	public ReplayTraceTransition(Transition transition) {
		this.operationName = transition.getName();
		this.parameterPredicates = transition.getParameterPredicates();
	}

	public String getOperationName() {
		return operationName;
	}

	public List<String> getParameterPredicates() {
		return parameterPredicates;
	}
}
