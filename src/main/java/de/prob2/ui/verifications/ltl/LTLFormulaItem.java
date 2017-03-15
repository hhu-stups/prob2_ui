package de.prob2.ui.verifications.ltl;

public class LTLFormulaItem {
	
	private String name;
	private String formula;
	
	public LTLFormulaItem(String name) {
		this.name = name;
	}
	
	public void setFormula(String formula) {
		this.formula = formula;
	}
		
	public String toString() {
		return name;
	}

}
