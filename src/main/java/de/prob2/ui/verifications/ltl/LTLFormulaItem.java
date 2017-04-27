package de.prob2.ui.verifications.ltl;

import java.util.ArrayList;
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
	private transient LTLFormulaDialog formulaDialog;

	public LTLFormulaItem(LTLFormulaDialog formulaDialog, String name, String description, String formula) {
		initializeStatus();
		this.name = name;
		this.description = description;
		this.formula = formula;
		this.counterExample = null;
		this.formulaDialog = formulaDialog;
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
		initializeStatus();
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
	
	public void setFormulaDialog(LTLFormulaDialog formulaDialog) {
		this.formulaDialog = formulaDialog;
	}
	
	public boolean showAndRegisterChange() {
		formulaDialog.setName(getName());
		formulaDialog.setDescription(getDescription());
		formulaDialog.setFormula(getFormula());
		ArrayList<Boolean> changed = new ArrayList<>();
		changed.add(new Boolean(false));
		formulaDialog.showAndWait().ifPresent(result-> {
			if(!getName().equals(result.getName()) || !getDescription().equals(result.getDescription()) || 
					!getFormula().equals(result.getFormula())) {
				setName(result.getName());
				setDescription(result.getDescription());
				setFormula(result.getFormula());
				changed.set(0, new Boolean(true));
			}
		});
		return changed.get(0).booleanValue();
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
