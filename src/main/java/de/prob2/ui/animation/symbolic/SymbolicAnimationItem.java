package de.prob2.ui.animation.symbolic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.statespace.Trace;
import de.prob2.ui.symbolic.SymbolicItem;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class SymbolicAnimationItem extends SymbolicItem<SymbolicAnimationType> {
	private final SymbolicAnimationType type;
	
	@JsonIgnore
	private final ObjectProperty<Trace> example = new SimpleObjectProperty<>(this, "example", null);

	@JsonCreator
	public SymbolicAnimationItem(
		@JsonProperty("code") final String code,
		@JsonProperty("type") final SymbolicAnimationType type
	) {
		super(code);
		this.type = type;
	}

	@Override
	public SymbolicAnimationType getType() {
		return this.type;
	}

	@Override
	public void reset() {
		super.reset();
		this.setExample(null);
	}

	public ObjectProperty<Trace> exampleProperty() {
		return this.example;
	}

	public Trace getExample() {
		return this.exampleProperty().get();
	}

	public void setExample(final Trace example) {
		this.exampleProperty().set(example);
	}
}
