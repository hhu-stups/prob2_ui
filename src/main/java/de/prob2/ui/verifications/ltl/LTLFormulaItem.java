package de.prob2.ui.verifications.ltl;

import java.util.Objects;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.statespace.Trace;
import javafx.scene.paint.Color;

//TODO: Refactor this and Machine
public class LTLFormulaItem {

	private transient FontAwesomeIconView status;
	private String name;
	private String description;
	private String formula;

	private transient Trace counterExample;

	public LTLFormulaItem(String name, String description, String formula) {
		initializeStatus();
		this.name = name;
		this.description = description;
		this.formula = formula;
		this.counterExample = null;
	}
	
	public void setData(String name, String description, String formula) {
		initializeStatus();
		this.name = name;
		this.description = description;
		this.formula = formula;
	}
	
	public void initializeStatus() {
		FontAwesomeIconView newStatus = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		newStatus.setFill(Color.BLUE);
		this.status = newStatus;
	}
	
	@Override
	public String toString() {
		return "Name: " + name + ", Description: " + description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getFormula() {
		return formula;
	}

	public FontAwesomeIconView getStatus() {
		return status;
	}

	public void setCheckedSuccessful() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
		icon.setFill(Color.GREEN);
		this.status = icon;
	}

	public void setCheckedFailed() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.REMOVE);
		icon.setFill(Color.RED);
		this.status = icon;
	}

	public void setCounterExample(Trace counterExample) {
		this.counterExample = counterExample;
	}

	public Trace getCounterExample() {
		return counterExample;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof LTLFormulaItem)) {
			return false;
		}
		LTLFormulaItem otherFormulaItem = (LTLFormulaItem) other;
		return this.name.equals(otherFormulaItem.getName())
				&& this.description.equals(otherFormulaItem.getDescription())
				&& this.formula.equals(otherFormulaItem.getFormula());
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description, formula);
	}

}
