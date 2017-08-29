package de.prob2.ui.verifications.cbc;

import de.prob.statespace.Trace;

public class CBCFormulaFindStateItem extends CBCFormulaItem {
	
	private Trace example;
	
	public CBCFormulaFindStateItem(String name, String code, CBCType type) {
		super(name, code, type);
		this.example = null;
	}
	
	public void setExample(Trace example) {
		this.example = example;
	}
	
	public Trace getExample() {
		return example;
	}

}
