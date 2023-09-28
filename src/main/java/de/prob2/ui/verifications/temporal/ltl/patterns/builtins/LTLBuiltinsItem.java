package de.prob2.ui.verifications.temporal.ltl.patterns.builtins;

public class LTLBuiltinsItem {

	private final String pattern;
	
	private final String description;
	
	public LTLBuiltinsItem(String pattern, String description) {
		this.pattern = pattern;
		this.description = description;
	}
	
	public String getPattern() {
		return pattern;
	}
	
	public String getDescription() {
		return description;
	}
	
}
