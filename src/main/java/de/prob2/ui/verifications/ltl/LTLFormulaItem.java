package de.prob2.ui.verifications.ltl;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class LTLFormulaItem {
	
	private SimpleObjectProperty<FontAwesomeIconView> status;
	private SimpleStringProperty name;
	private SimpleStringProperty description;
	
	private LTLFormulaStage formulaStage;
	
	public LTLFormulaItem(FontAwesomeIconView status, String name, String description, LTLFormulaStage formulaStage) {
		this.status = new SimpleObjectProperty<>(this, "status", status);
		this.name = new SimpleStringProperty(this, "name", name);
		this.description = new SimpleStringProperty(this, "description", description);
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
		return name.get();
	}
	
	public void setName(String name) {
		this.name.set(name);
	}
	
	public String getDescription() {
		return description.get();
	}
	
	public void setDescription(String description) {
		this.description.set(description);
	}
	
	public FontAwesomeIconView getStatus() {
		return status.get();
	}
	
	public void setStatus(FontAwesomeIconView status) {
		this.status.set(status);
	}

}
