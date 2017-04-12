package de.prob2.ui.verifications.ltl;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.paint.Color;

public class LTLFormulaItem {
	
	private transient FontAwesomeIconView status;
	private String name;
	private String description;
	private String formula;
	
	private transient LTLFormulaStage formulaStage;
	
	public LTLFormulaItem(String name, String description) {
		initializeStatus();
		this.name = name;
		this.description = description;
	}
	
	public void initializeStatus() {
		FontAwesomeIconView newStatus = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		newStatus.setFill(Color.BLUE);
		this.status = newStatus;
	}
	
	public void setFormulaStage(LTLFormulaStage formulaStage) {
		this.formulaStage = formulaStage;
		formulaStage.setItem(this);
	}
	
	public void checkFormula() {
		formulaStage.checkFormula();
	}
	
	public void show() {
		formulaStage.show();
	}
	
	@Override
	public String toString() {
		return "Name: " + name + ", Description: " + description;
	}
		
	public String getName() {
		return name;
	}
		
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
		
	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setFormula(String formula) {
		this.formula = formula;
	}
	
	public String getFormula() {
		return formula;
	}
	
	public FontAwesomeIconView getStatus() {
		return status;
	}
		
	public void setStatus(FontAwesomeIconView status) {
		this.status = status;
	}
	
}
