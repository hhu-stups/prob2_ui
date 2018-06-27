package de.prob2.ui.verifications.ltl.formula;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Objects;

public class LTLFormulaItem extends AbstractCheckableItem {

	private transient ObjectProperty<Trace> counterExample;

	public LTLFormulaItem(String name, String description, String code) {
		super(name, description, code);
		this.counterExample = new SimpleObjectProperty<>(null);
	}
			
	public void setCounterExample(Trace counterExample) {
		if(counterExample == null) {
			this.counterExample = new SimpleObjectProperty<>(counterExample);
			return;
		}
		this.counterExample.set(counterExample);
	}

	public Trace getCounterExample() {
		return counterExample.get();
	}
	
	public ObjectProperty<Trace> counterExampleProperty() {
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
		return Objects.hash(name);
	}

}
