package de.prob2.ui.verifications.ltl.formula;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ltl.ILTLItem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Objects;

public class LTLFormulaItem extends AbstractCheckableItem implements ILTLItem {

	private transient ObjectProperty<Trace> counterExample;

	public LTLFormulaItem(String code, String description) {
		super("", description, code);
		this.counterExample = new SimpleObjectProperty<>(null);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		this.counterExample = new SimpleObjectProperty<>(null);
	}
			
	public void setCounterExample(Trace counterExample) {
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
		return this.code.equals(otherFormulaItem.getCode()) && this.description.equals(otherFormulaItem.getDescription());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(code, description);
	}

}
