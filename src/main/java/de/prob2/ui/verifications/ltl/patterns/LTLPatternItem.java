package de.prob2.ui.verifications.ltl.patterns;

import de.prob2.ui.verifications.ltl.LTLCheckableItem;

public class LTLPatternItem extends LTLCheckableItem {
		
	private String code;
	
	public LTLPatternItem(String name, String description, String code) {
		super(name, description);
		initializeStatus();
		setData(name, description, code);
		
	}	
	
	public void setData(String name, String description, String code) {
		setName(name);
		setDescription(description);
		setCode(code);
	}
	
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getCode() {
		return code;
	}
	
}
