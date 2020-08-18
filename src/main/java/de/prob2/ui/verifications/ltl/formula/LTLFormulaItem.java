package de.prob2.ui.verifications.ltl.formula;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ltl.ILTLItem;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class LTLFormulaItem extends AbstractCheckableItem implements ILTLItem {
	public static final JsonDeserializer<LTLFormulaItem> JSON_DESERIALIZER = LTLFormulaItem::new;

	private final transient ObjectProperty<Trace> counterExample = new SimpleObjectProperty<>(this, "counterExample", null);

	public LTLFormulaItem(String code, String description) {
		super("", description, code);
	}
	
	private LTLFormulaItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		super(json, typeOfT, context);
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
