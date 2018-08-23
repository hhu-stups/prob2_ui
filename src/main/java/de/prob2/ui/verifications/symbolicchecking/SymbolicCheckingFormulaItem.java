package de.prob2.ui.verifications.symbolicchecking;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Objects;

public class SymbolicCheckingFormulaItem extends AbstractCheckableItem {
		
	private SymbolicCheckingType type;
	
	private transient ListProperty<Trace> counterExamples;

	public SymbolicCheckingFormulaItem(String name, String code, SymbolicCheckingType type) {
		super(name, type.getName(), code);
		this.type = type;
		this.initializeCounterExamples();
	}
		
	public void initializeCounterExamples() {
		this.counterExamples = new SimpleListProperty<>(FXCollections.observableArrayList());
	}
	
	public ObservableList<Trace> getCounterExamples() {
		return counterExamples.get();
	}
	
	public ListProperty<Trace> counterExamplesProperty() {
		return counterExamples;
	}
	
	public void reset() {
		this.initializeStatus();
		this.counterExamples.clear();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof SymbolicCheckingFormulaItem)) {
			return false;
		}
		SymbolicCheckingFormulaItem otherItem = (SymbolicCheckingFormulaItem) obj;
		return otherItem.getName().equals(this.getName()) &&
				otherItem.getCode().equals(this.getCode()) &&
				otherItem.getType().equals(this.getType());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, code, type);
	}
	
	public void setType(SymbolicCheckingType type) {
		this.type = type;
	}
	
	public SymbolicCheckingType getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return String.join(" ", name, code, type.name());
	}
	
	public void setData(String name, String description, String code, SymbolicCheckingType type) {
		super.setData(name, description, code);
		this.type = type;
	}
	

}
