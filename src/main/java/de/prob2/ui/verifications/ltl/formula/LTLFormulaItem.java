package de.prob2.ui.verifications.ltl.formula;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ltl.ILTLItem;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
	"id",
	"description",
	"code",
	"selected",
})
public class LTLFormulaItem extends AbstractCheckableItem implements ILTLItem, IValidationTask {
	private final String id;
	private final String code;
	private final String description;
	
	@JsonIgnore
	private final ObjectProperty<Trace> counterExample = new SimpleObjectProperty<>(this, "counterExample", null);
	
	@JsonCreator
	public LTLFormulaItem(
		@JsonProperty("id") final String id,
		@JsonProperty("code") final String code,
		@JsonProperty("description") final String description
	) {
		super();
		
		this.id = id;
		this.code = code;
		this.description = description;
	}
	
	@Override
	public void reset() {
		super.reset();
		this.setCounterExample(null);
	}
	
	@Override
	public String getId() {
		return this.id;
	}
	
	public String getCode() {
		return this.code;
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
