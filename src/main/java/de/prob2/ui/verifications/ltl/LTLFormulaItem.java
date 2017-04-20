package de.prob2.ui.verifications.ltl;

import java.util.Objects;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.statespace.Trace;
import javafx.scene.paint.Color;

public class LTLFormulaItem {
	
	private transient FontAwesomeIconView status;
	private String name;
	private String description;
	private String formula;
	
	private transient Trace counterExample;
	private transient LTLFormulaStage formulaStage;
	
	public LTLFormulaItem(String name, String description) {
		initializeStatus();
		this.name = name;
		this.description = description;
		this.formula = "";
		this.counterExample = null;
	}
	
	public LTLFormulaItem(LTLFormulaItem item) {
		this.status = item.status;
		this.name = item.name;
		this.description = item.description;
		this.formula = item.formula;
		this.counterExample = item.counterExample;
		this.formulaStage = item.formulaStage;
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
	
	public void setCheckedSuccessful() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
		icon.setFill(Color.GREEN);
		this.setStatus(icon);
	}
	
	public void setCheckedFailed() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.REMOVE);
		icon.setFill(Color.RED);
		this.setStatus(icon);
	}
	
	public void setCounterExample(Trace counterExample) {
		this.counterExample = counterExample;
	}
	
	public Trace getCounterExample() {
		return counterExample;
	}
		
	@Override
	public boolean equals(Object other) {
		LTLFormulaItem otherFormulaItem = (LTLFormulaItem) other;
		if(this.name.equals(otherFormulaItem.getName()) && 
				this.description.equals(otherFormulaItem.getDescription()) &&
				this.formula.equals(otherFormulaItem.getFormula())) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, description, formula);
	}
		
}
