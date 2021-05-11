package de.prob2.ui.verifications.ltl.formula;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ltl.ILTLItem;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
	"name",
	"description",
	"code",
	"selected",
})
public class LTLFormulaItem extends AbstractCheckableItem implements ILTLItem {
	private final String description;
	
	@JsonIgnore
	private final ObjectProperty<Trace> counterExample = new SimpleObjectProperty<>(this, "counterExample", null);

	public LTLFormulaItem(String code, String description) {
		this("", description, code);
	}
	
	@JsonCreator
	private LTLFormulaItem(
		@JsonProperty("name") final String name,
		@JsonProperty("description") final String description,
		@JsonProperty("code") final String code
	) {
		super(name, code);
		
		this.description = description;
	}
	
	@Override
	public void reset() {
		super.reset();
		this.setCounterExample(null);
	}
	
	public String getDescription() {
		return this.description;
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
