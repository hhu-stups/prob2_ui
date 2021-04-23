package de.prob2.ui.verifications.ltl.formula;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ltl.ILTLItem;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class LTLFormulaItem extends AbstractCheckableItem implements ILTLItem {
	@JsonIgnore
	private final ObjectProperty<Trace> counterExample = new SimpleObjectProperty<>(this, "counterExample", null);

	public LTLFormulaItem(String code, String description) {
		super("", description, code);
	}
	
	@JsonCreator
	private LTLFormulaItem(
		@JsonProperty("name") final String name,
		@JsonProperty("description") final String description,
		@JsonProperty("code") final String code
	) {
		super(name, description, code);
	}
	
	@Override
	public void reset() {
		super.reset();
		this.setCounterExample(null);
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
	
	public boolean settingsEqual(final LTLFormulaItem other) {
		return this.getCode().equals(other.getCode())
			&& this.getDescription().equals(other.getDescription());
	}
}
