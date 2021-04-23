package de.prob2.ui.animation.symbolic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.statespace.Trace;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicItem;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SymbolicAnimationItem extends SymbolicItem {
	@JsonIgnore
	private final ListProperty<Trace> examples = new SimpleListProperty<>(this, "examples", FXCollections.observableArrayList());

	@JsonCreator
	private SymbolicAnimationItem(
		@JsonProperty("name") final String name,
		@JsonProperty("code") final String code,
		@JsonProperty("type") final SymbolicExecutionType type
	) {
		super(name, code, type);
	}

	public SymbolicAnimationItem(String name, SymbolicExecutionType type) {
		this(name, name, type);
	}

	@Override
	public void reset() {
		super.reset();
		this.examples.clear();
	}


	public ObservableList<Trace> getExamples() {
		return examples.get();
	}

	public ListProperty<Trace> examplesProperty() {
		return examples;
	}
}
