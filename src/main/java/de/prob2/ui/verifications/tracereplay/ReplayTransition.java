package de.prob2.ui.verifications.tracereplay;

import java.util.List;

public class ReplayTransition {
	private final String name;
	private final List<String> parameterValues;

	public ReplayTransition(final String name, final List<String> parameters) {
		this.name = name;
		this.parameterValues = parameters;
	}
	
	public String getName() {
		return name;
	}
	
	public List<String> getParameterValues() {
		return parameterValues;
	}
}
