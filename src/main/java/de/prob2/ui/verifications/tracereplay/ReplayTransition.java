package de.prob2.ui.verifications.tracereplay;

import java.util.List;

public class ReplayTransition {
	private final String name;
	private final List<String> parameters;

	public ReplayTransition(final String name, final List<String> parameters) {
		this.name = name;
		this.parameters = parameters;
	}
	
	public String getName() {
		return name;
	}
	
	public List<String> getParameterValues() {
		return parameters;
	}
}
