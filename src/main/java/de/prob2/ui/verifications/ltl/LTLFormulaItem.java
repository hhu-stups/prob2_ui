package de.prob2.ui.verifications.ltl;

import javafx.beans.property.SimpleStringProperty;

public class LTLFormulaItem {
	
	private SimpleStringProperty name;
	private SimpleStringProperty description;
	private String formula;
	
	private LTLFormulaStage formulaStage;
	
	public LTLFormulaItem(String name, String description, LTLFormulaStage formulaStage) {
		this.name = new SimpleStringProperty(this, "name", name);
		this.description = new SimpleStringProperty(this, "description", description);
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
		return "Name: " + name + ", Description: " + description;
	}
	
	public String getName() {
		return name.get();
	}
	
	public String getDescription() {
		return description.get();
	}

}
