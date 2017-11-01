package de.prob2.ui.verifications.symbolicchecking;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SymbolicCheckingFormulaItem extends AbstractCheckableItem {
		
	protected SymbolicCheckingType type;
	
	private transient List<Trace> counterExamples;
	
	private transient Trace example;

	public SymbolicCheckingFormulaItem(String name, String code, SymbolicCheckingType type) {
		super(name, type.name(), code);
		this.type = type;
		this.example = null;
		this.initializeCounterExamples();
	}
		
	public void initializeCounterExamples() {
		this.counterExamples = new ArrayList<>();
	}
	
	public List<Trace> getCounterExamples() {
		return counterExamples;
	}
	
	public void setExample(Trace example) {
		this.example = example;
	}
	
	public Trace getExample() {
		return example;
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
	

}
