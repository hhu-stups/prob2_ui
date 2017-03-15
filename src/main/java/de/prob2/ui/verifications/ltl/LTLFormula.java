package de.prob2.ui.verifications.ltl;

public class LTLFormula {
	
	private String name;
	private String formula;
	
	public LTLFormula(String name) {
		this.name = name;
	}
	
	public void setFormula(String formula) {
		this.formula = formula;
	}
		
	public String toString() {
		return name;
	}

}
