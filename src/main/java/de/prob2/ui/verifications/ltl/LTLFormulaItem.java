package de.prob2.ui.verifications.ltl;

public class LTLFormulaItem {
	
	private String name;
	private String formula;
	
	private LTLFormulaStage formulaStage;
	
	public LTLFormulaItem(String name, LTLFormulaStage formulaStage) {
		this.name = name;
		this.formulaStage = formulaStage;
	}
	
	public void setFormula(String formula) {
		this.formula = formula;
	}
	
	public void show() {
		formulaStage.show();
	}
	
	@Override
	public String toString() {
		return name;
	}

}
