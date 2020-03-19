package de.prob2.ui.animation.symbolic;

import java.lang.reflect.Type;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import de.prob.statespace.Trace;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicItem;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SymbolicAnimationItem extends SymbolicItem {
	public static final JsonDeserializer<SymbolicAnimationItem> JSON_DESERIALIZER = SymbolicAnimationItem::new;

	private final transient ListProperty<Trace> examples = new SimpleListProperty<>(this, "examples", FXCollections.observableArrayList());

	public SymbolicAnimationItem(String name, SymbolicExecutionType type) {
		super(name, type);
	}

	private SymbolicAnimationItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		super(json, typeOfT, context);
	}

	@Override
	public void initialize() {
		super.initialize();
		this.examples.clear();
	}


	public ObservableList<Trace> getExamples() {
		return examples.get();
	}

	public ListProperty<Trace> examplesProperty() {
		return examples;
	}

	@Override
	public void reset() {
		this.initialize();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof SymbolicAnimationItem)) {
			return false;
		}
		SymbolicAnimationItem otherItem = (SymbolicAnimationItem) obj;
		return otherItem.getName().equals(this.getName()) &&
				otherItem.getCode().equals(this.getCode()) &&
				otherItem.getType().equals(this.getType());
	}
	
	@Override
	public int hashCode() {
		 return Objects.hash(name, code, type);
	}

}
