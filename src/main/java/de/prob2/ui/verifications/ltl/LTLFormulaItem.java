package de.prob2.ui.verifications.ltl;

import java.util.Objects;
import de.prob.statespace.Trace;

public class LTLFormulaItem extends LTLCheckableItem {

	private String formula;
	private transient Trace counterExample;

	public LTLFormulaItem(String name, String description, String formula) {
		super(name, description);
		this.formula = formula;
		this.counterExample = null;
	}
	
	public void setData(String name, String description, String formula) {
		initializeStatus();
		this.name = name;
		this.description = description;
		this.formula = formula;
	}
		
	public String getFormula() {
		return formula;
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
		return this.name.equals(otherFormulaItem.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description, formula);
	}

}
