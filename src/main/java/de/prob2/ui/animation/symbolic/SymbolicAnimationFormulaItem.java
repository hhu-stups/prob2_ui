package de.prob2.ui.animation.symbolic;

import java.util.Objects;

import de.prob.statespace.Trace;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaItem;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SymbolicAnimationFormulaItem extends SymbolicFormulaItem {

	private transient ListProperty<Trace> examples;

	public SymbolicAnimationFormulaItem(String name, SymbolicExecutionType type) {
		super(name, type);
		this.examples = new SimpleListProperty<>(FXCollections.observableArrayList());
	}

	@Override
	public void initialize() {
		super.initialize();
		if(this.examples == null) {
			this.examples = new SimpleListProperty<>(FXCollections.observableArrayList());
		} else {
			this.examples.setValue(FXCollections.observableArrayList());
		}
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
		if (!(obj instanceof SymbolicAnimationFormulaItem)) {
			return false;
		}
		SymbolicAnimationFormulaItem otherItem = (SymbolicAnimationFormulaItem) obj;
		return otherItem.getName().equals(this.getName()) &&
				otherItem.getCode().equals(this.getCode()) &&
				otherItem.getType().equals(this.getType());
	}
	
	@Override
	public int hashCode() {
		 return Objects.hash(name, code, type);
	}
	
	

}
