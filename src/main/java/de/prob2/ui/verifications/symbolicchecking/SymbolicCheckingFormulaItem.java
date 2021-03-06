package de.prob2.ui.verifications.symbolicchecking;

import java.lang.reflect.Type;

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

public class SymbolicCheckingFormulaItem extends SymbolicItem {
	public static final JsonDeserializer<SymbolicCheckingFormulaItem> JSON_DESERIALIZER = SymbolicCheckingFormulaItem::new;
	
	private final transient ListProperty<Trace> counterExamples = new SimpleListProperty<>(this, "counterExamples", FXCollections.observableArrayList());

	public SymbolicCheckingFormulaItem(String name, String code, SymbolicExecutionType type) {
		super(name, code, type);
	}
	
	private SymbolicCheckingFormulaItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		super(json, typeOfT, context);
	}
	
	public ObservableList<Trace> getCounterExamples() {
		return counterExamples.get();
	}
	
	public ListProperty<Trace> counterExamplesProperty() {
		return counterExamples;
	}
	
	@Override
	public void reset() {
		super.reset();
		this.counterExamples.clear();
	}
}
