package de.prob2.ui.verifications.symbolicchecking;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.statespace.Trace;
import de.prob2.ui.symbolic.SymbolicItem;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@JsonPropertyOrder({
	"id",
	"type",
	"code",
	"selected",
})
public class SymbolicCheckingFormulaItem extends SymbolicItem<SymbolicCheckingType> implements IValidationTask {
	private final String id;
	private final SymbolicCheckingType type;
	
	@JsonIgnore
	private final ListProperty<Trace> counterExamples = new SimpleListProperty<>(this, "counterExamples", FXCollections.observableArrayList());
	
	@JsonCreator
	public SymbolicCheckingFormulaItem(
		@JsonProperty("id") final String id,
		@JsonProperty("code") final String code,
		@JsonProperty("type") final SymbolicCheckingType type
	) {
		super(code);
		this.id = id;
		this.type = type;
	}
	
	@Override
	public String getId() {
		return this.id;
	}
	
	@Override
	public SymbolicCheckingType getType() {
		return this.type;
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
