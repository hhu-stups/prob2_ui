package de.prob2.ui.verifications.cbc;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CBCFormulaItem extends AbstractCheckableItem {
	
	public enum CBCType {
		INVARIANT,SEQUENCE,DEADLOCK, FIND_DEADLOCK, FIND_VALID_STATE, FIND_REDUNDANT_INVARIANTS, 
		REFINEMENT, ASSERTIONS
	}
	
	protected CBCType type;
	
	private transient List<Trace> counterExamples;
	
	private transient Trace example;

	public CBCFormulaItem(String name, String code, CBCType type) {
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
		if (!(obj instanceof CBCFormulaItem)) {
			return false;
		}
		CBCFormulaItem otherItem = (CBCFormulaItem) obj;
		return otherItem.getName().equals(this.getName()) &&
				otherItem.getCode().equals(this.getCode()) &&
				otherItem.getType().equals(this.getType());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, code, type);
	}
	
	public CBCType getType() {
		return type;
	}
	

}
