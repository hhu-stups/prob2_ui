package de.prob2.ui.verifications.ltl.formula;

import java.util.Objects;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

public class LTLFormulaItem extends LTLPatternItem {

	private transient Trace counterExample;

	public LTLFormulaItem(String name, String description, String code) {
		super(name, description, code);
		this.counterExample = null;
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
		return Objects.hash(name, description, code);
	}

}
