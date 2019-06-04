package de.prob2.ui.animation.symbolic;

import java.util.Objects;

import de.prob.statespace.Trace;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaItem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class SymbolicAnimationFormulaItem extends SymbolicFormulaItem {
	
	private transient ObjectProperty<Trace> example;

	public SymbolicAnimationFormulaItem(String name, SymbolicExecutionType type) {
		super(name, type);
		this.example = new SimpleObjectProperty<>(null);
	}

	@Override
	public void initialize() {
		super.initialize();
		if(this.example == null) {
			this.example = new SimpleObjectProperty<>(null);
		} else {
			this.example.set(null);
		}
	}
	
	public void setExample(Trace example) {
		this.example.set(example);
	}
	
	public Trace getExample() {
		return example.get();
	}
	
	public ObjectProperty<Trace> exampleProperty() {
		return example;
	}
	
	@Override
	public void reset() {
		this.initialize();
		if(this.example == null) {
			this.example = new SimpleObjectProperty<>(null);
		} else {
			this.example.set(null);
		}
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
