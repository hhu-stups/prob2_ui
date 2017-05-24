package de.prob2.ui.verifications.ltl.patterns;

import de.prob2.ui.verifications.ltl.LTLCheckableItem;

public class LTLPatternItem extends LTLCheckableItem {
	
	private String pattern;
	
	public LTLPatternItem(String name, String description, String pattern) {
		super(name, description);
		this.pattern = pattern;
	}
	
	public String getPattern() {
		return pattern;
	}
	
	public void setData(String name, String description, String pattern) {
		initializeStatus();
		this.name = name;
		this.description = description;
		this.pattern = pattern;
	}

}
